
package driver;

import healer.OpenAIHealer;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import utils.XPathUtils;

import java.time.Duration;
import java.util.List;

public class SelfHealingDriver {

    private final WebDriver driver;
    private final WebDriverWait wait;
    private final OpenAIHealer healer;

    public SelfHealingDriver(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(8));
        this.healer = new OpenAIHealer();
    }

    public WebElement findElement(By primary, By... fallbacks) {

        // STEP 1: Try the original locator
        System.out.println("\n====================================");
        System.out.println("[SELF-HEALING] Finding element");
        System.out.println("[PRIMARY] Trying: " + primary);

        try {
            WebElement element = wait.until(d -> {
                List<WebElement> found = d.findElements(primary);

                for (WebElement el : found) {
                    try {
                        if (el.isDisplayed() && el.isEnabled()) {
                            return el;
                        }
                    } catch (StaleElementReferenceException ignored) {
                        // Retry on the next polling attempt.
                    }
                }
                return null;
            });

            System.out.println("[SUCCESS] Primary locator worked.");
            System.out.println("[RESULT] No healing required.");
            System.out.println("====================================");

            return element;

        } catch (Exception e) {
            System.out.println("[FAILED] Primary locator: " + primary);
            System.out.println("[HEALING] Trying fallback locators...");
        }

        // STEP 2: Try fallback locators
        for (By fallback : fallbacks) {

            System.out.println("[FALLBACK] Trying: " + fallback);

            try {
                List<WebElement> found = driver.findElements(fallback);

                List<WebElement> usable = found.stream()
                        .filter(el -> {
                            try {
                                return el.isDisplayed() && el.isEnabled();
                            } catch (Exception ignored) {
                                return false;
                            }
                        })
                        .toList();

                if (usable.size() == 1) {
                    System.out.println("[HEALED] Fallback locator worked!");
                    System.out.println("[USED LOCATOR] " + fallback);
                    System.out.println("====================================");

                    return usable.get(0);
                }

                if (usable.size() > 1) {
                    System.out.println(
                            "[SKIPPED] Ambiguous fallback matched "
                                    + usable.size() + " elements: " + fallback);
                } else {
                    System.out.println("[FAILED] No visible element: " + fallback);
                }

            } catch (Exception e) {
                System.out.println("[FAILED] Fallback locator: " + fallback);
            }
        }

        // STEP 3: Ask AI for an XPath
        System.out.println("[AI HEALING] All fallback locators failed.");
        System.out.println("[AI HEALING] Requesting an XPath suggestion...");

        String dom = safePageSource();
        String suggestedXPath = healer.suggestXPath(primary.toString(), dom);

        if (suggestedXPath != null
                && XPathUtils.isValidXPath(suggestedXPath)) {

            System.out.println("[AI SUGGESTION] " + suggestedXPath);

            try {
                List<WebElement> found =
                        driver.findElements(By.xpath(suggestedXPath));

                List<WebElement> usable = found.stream()
                        .filter(el -> {
                            try {
                                return el.isDisplayed() && el.isEnabled();
                            } catch (Exception ignored) {
                                return false;
                            }
                        })
                        .toList();

                if (usable.size() == 1) {
                    System.out.println("[AI HEALED] XPath validated successfully!");
                    System.out.println("[USED AI XPATH] " + suggestedXPath);
                    System.out.println("====================================");

                    return usable.get(0);
                }

                System.out.println(
                        "[AI REJECTED] XPath matched "
                                + usable.size()
                                + " visible/enabled elements.");

            } catch (Exception e) {
                System.out.println("[AI ERROR] Could not evaluate AI XPath.");
            }

        } else {
            System.out.println(
                    "[AI FAILED] No valid XPath returned. Check API key, "
                            + "network, and XPath validation.");
        }

        System.out.println("[ERROR] All locator strategies failed.");
        System.out.println("====================================");

        throw new NoSuchElementException(
                "Unable to locate element. Primary locator: " + primary);
    }

    public void click(By primary, By... fallbacks) {
        System.out.println("[ACTION] Clicking element...");
        findElement(primary, fallbacks).click();
        System.out.println("[ACTION SUCCESS] Click completed.");
    }

    public void type(By primary, String value, By... fallbacks) {
        System.out.println("[ACTION] Typing text...");
        WebElement element = findElement(primary, fallbacks);
        element.clear();
        element.sendKeys(value);
        System.out.println("[ACTION SUCCESS] Text entered.");
    }

    private String safePageSource() {
        try {
            String source = driver.getPageSource();
            int maxChars = Integer.parseInt(
                    System.getProperty("healer.maxDomChars", "12000"));

            return source.length() > maxChars
                    ? source.substring(0, maxChars)
                    : source;

        } catch (Exception e) {
            System.out.println("[WARNING] Could not read page source.");
            return "";
        }
    }
}
