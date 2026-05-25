package base;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import java.time.Duration;

public class BaseTest {

    private static final ThreadLocal<WebDriver> driver = new ThreadLocal<>();

    public WebDriver getDriver() {
        return driver.get();
    }

    @BeforeMethod
    public void setUp() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--start-maximized");
        options.addArguments("--disable-notifications");
        // Prevents renderer from being killed on slow networks
        options.addArguments("--disable-renderer-backgrounding");
        options.addArguments("--disable-backgrounding-occluded-windows");

        WebDriver rawDriver = new ChromeDriver(options);

        // No implicitWait — conflicts with FluentWait in WaitUtils
        // pageLoad raised to 60s — practice.expandtesting.com can be slow
        rawDriver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(60));
        rawDriver.manage().timeouts().scriptTimeout(Duration.ofSeconds(30));

        driver.set(rawDriver);
        getDriver().get("https://practice.expandtesting.com/notes/app/login");
    }

    @AfterMethod
    public void tearDown() {
        if (getDriver() != null) {
            getDriver().quit();
            driver.remove();
        }
    }
}
