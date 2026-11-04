package tests;
import java.time.Duration;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import driver.SelfHealingDriver;
public class OrangeHRMTest {
	private WebDriver driver;
    private SelfHealingDriver healing;

    @BeforeMethod
    public void setUp() {
        driver = new ChromeDriver(); // Selenium Manager resolves the driver.
        driver.manage().window().maximize();
        driver.get("https://opensource-demo.orangehrmlive.com/");
        healing = new SelfHealingDriver(driver);
    }

    @Test
    public void loginAndVerifyDashboard() {
        // Public demo credentials commonly used by OrangeHRM.
        // If the demo site changes credentials, use its current published credentials.
      /*  healing.type(
                By.name("username"),
                "Admin",
                By.cssSelector("input[placeholder='Username']"),
                By.xpath("//input[@name='username']")
        );

        healing.type(
                By.name("password"),
                "admin123",
                By.cssSelector("input[placeholder='Passwords']"),
                 By.xpath("//input[@name='password']")
        );

        healing.click(
                By.cssSelector("button[type='submit']"),
                By.xpath("//button[@type='submit']"),
                By.xpath("//button[normalize-space()='Login']")
        ); */
        healing.type(
        	    By.id("wrongUsername"),
        	    "Admin",
        	    By.name("username"),
        	    By.cssSelector("input[placeholder='Username']")
        	);

        	healing.type(
        	    By.name("password"),
        	    "admin123",
        	    By.cssSelector("input[placeholder='Password']")
        	);

        	healing.click(
        	    By.id("wrongLoginButton"),
        	    By.cssSelector("button[type='submit']"),
        	    By.xpath("//button[normalize-space()='Login']")
        	);

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        wait.until(ExpectedConditions.urlContains("/dashboard"));

        Assert.assertTrue(driver.getCurrentUrl().contains("/dashboard"),
                "Expected to reach the OrangeHRM dashboard.");

        Assert.assertTrue(
                wait.until(ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//h6[normalize-space()='Dashboard']"))).isDisplayed(),
                "Dashboard heading should be visible.");

        System.out.println("PASS: OrangeHRM login and dashboard verification.");
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }


}
