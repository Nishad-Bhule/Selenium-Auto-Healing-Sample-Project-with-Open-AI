 package utils;
 import java.util.List;

import org.openqa.selenium.By;
 import org.openqa.selenium.WebDriver;
 import org.openqa.selenium.WebElement;

public class XPathUtils {
	private XPathUtils() {}

    public static boolean isValidXPath(String xpath) {
        if (xpath == null || xpath.isBlank()
                || !xpath.startsWith("/") || xpath.length() > 1000) {
            return false;
        }
        // Reject obvious multi-statement or script-like input.
        if (xpath.contains(";") || xpath.contains("\n") || xpath.contains("\r")) {
            return false;
        }
        return true;
    }

    public static boolean matchesExactlyOne(WebDriver driver, String xpath) {
        if (!isValidXPath(xpath)) return false;
        try {
            List<WebElement> elements = driver.findElements(By.xpath(xpath));
            return elements.size() == 1;
        } catch (Exception e) {
            return false;
        }
    }


}
