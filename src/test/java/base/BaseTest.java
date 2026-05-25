package base;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import java.time.Duration;

public class BaseTest {

    // ThreadLocal manages isolated driver instances for safe parallel execution
    private static final ThreadLocal<WebDriver> driver = new ThreadLocal<>();

    public WebDriver getDriver() {
        return driver.get();
    }

    @BeforeMethod
    public void setUp() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--start-maximized");
        options.addArguments("--disable-notifications");

        WebDriver rawDriver = new ChromeDriver(options);

        // ✅ CRITICAL FIX: implicitWait REMOVED.
        //
        // implicitWait and FluentWait/WebDriverWait must NEVER coexist.
        // When both are active, Selenium merges their timeouts in undefined ways:
        //   - FluentWait polling gets disrupted by the implicit poll underneath
        //   - A "not found" result from FluentWait can get delayed by implicitWait
        //     before the exception is thrown, causing the 5s FluentWait to actually
        //     block for 5s (implicit) + 5s (fluent) = 10s before failing
        //   - Worse: element-not-found conditions inside until() lambdas get swallowed
        //     by implicitWait retries, breaking negative checks entirely
        //
        // Our WaitUtils uses FluentWait with explicit per-call timeouts.
        // That is the ONLY wait strategy in use — no implicit wait needed.
        rawDriver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));  // ✔ CORRECT
        rawDriver.manage().timeouts().scriptTimeout(Duration.ofSeconds(30));

        driver.set(rawDriver);

        // Navigate to login page before every test row
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
