package healer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class OpenAIHealer {
	   private static final String API_URL = "https://api.openai.com/v1/chat/completions";
	    private static final ObjectMapper MAPPER = new ObjectMapper();
	    private final HttpClient client = HttpClient.newBuilder()
	            .connectTimeout(Duration.ofSeconds(15))
	            .build();

	    public String suggestXPath(String failedLocator, String pageSource) {
	        String apiKey = System.getenv("OPENAI_API_KEY");
	        if (apiKey == null || apiKey.isBlank()) {
	            System.out.println("[AI] OPENAI_API_KEY is not set. AI healing skipped.");
	            return null;
	        }
	        if (pageSource == null || pageSource.isBlank()) {
	            return null;
	        }

	        try {
	            String prompt = """
	                    You help repair Selenium locators.
	                    The failed Selenium locator is: %s
	                    Inspect the HTML snippet below and return exactly ONE XPath expression
	                    for the same intended element. Prefer stable attributes such as name,
	                    type, aria-label, placeholder, and visible text. Do not return CSS,
	                    Markdown, explanations, or code fences. If no reliable match exists,
	                    return NO_MATCH.
	                    HTML:
	                    %s
	                    """.formatted(failedLocator, pageSource);

	            var payload = MAPPER.createObjectNode();
	            payload.put("model", System.getenv().getOrDefault("OPENAI_MODEL", "gpt-4o-mini"));
	            payload.put("temperature", 0);
	            var messages = payload.putArray("messages");
	            messages.addObject()
	                    .put("role", "system")
	                    .put("content", "Return only a single XPath expression or NO_MATCH.");
	            messages.addObject()
	                    .put("role", "user")
	                    .put("content", prompt);

	            HttpRequest request = HttpRequest.newBuilder()
	                    .uri(URI.create(API_URL))
	                    .timeout(Duration.ofSeconds(35))
	                    .header("Authorization", "Bearer " + apiKey)
	                    .header("Content-Type", "application/json")
	                    .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(payload)))
	                    .build();

	            HttpResponse<String> response = client.send(
	                    request, HttpResponse.BodyHandlers.ofString());

	            if (response.statusCode() < 200 || response.statusCode() >= 300) {
	                System.out.println("[AI] API returned HTTP " + response.statusCode()
	                        + ". Response: " + abbreviate(response.body(), 500));
	                return null;
	            }

	            JsonNode root = MAPPER.readTree(response.body());
	            String content = root.path("choices").path(0)
	                    .path("message").path("content").asText("").trim();

	            // Be defensive if the model returns a code fence despite the instruction.
	            content = content.replaceAll("^```(?:xpath)?\\\\s*", "")
	                    .replaceAll("\\\\s*```$", "").trim();

	            if (content.equalsIgnoreCase("NO_MATCH")
	                    || !content.startsWith("/")) {
	                System.out.println("[AI] Model did not return an acceptable XPath.");
	                return null;
	            }
	            return content;
	        } catch (Exception e) {
	            System.out.println("[AI] Locator suggestion failed: " + e.getMessage());
	            return null;
	        }
	    }

	    private String abbreviate(String value, int limit) {
	        if (value == null) return "";
	        return value.length() <= limit ? value : value.substring(0, limit);
	    }

}
