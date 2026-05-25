package base;

import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import java.time.Duration;

public class BaseTest {

    // ThreadLocal container preserves context isolation for concurrent TestNG thread executions
    private static final ThreadLocal<WebDriver> driver = new ThreadLocal<>();

    public WebDriver getDriver() {
        return driver.get();
    }

    @BeforeMethod
    public void setUp() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--start-maximized");
        options.addArguments("--disable-notifications");

        // Prevents renderer from being killed on slow networks or high CPU parallel loads
        options.addArguments("--disable-renderer-backgrounding");
        options.addArguments("--disable-backgrounding-occluded-windows");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");

        // 🌟 THE CRITICAL STABILITY FIX: Switch page load strategy to EAGER
        // Releases the blocking thread the millisecond the core HTML DOM interactive layer mounts,
        // stopping Chrome from hanging during heavy concurrent navigate().refresh() cycles.
        options.setPageLoadStrategy(PageLoadStrategy.EAGER);

        // 🌟 CI/CD AGENT CONTROLLER: Automatic Headless fallback shield
        // Runs headlessly on your Jenkins server agent while keeping browsers visible during local IntelliJ debugging.
        if (System.getenv("JENKINS_HOME") != null) {
            options.addArguments("--headless=new");
        }

        WebDriver rawDriver = new ChromeDriver(options);

        // No implicitWait — strictly keeps architecture separate from your FluentWait structures inside WaitUtils
        // Managed fluid timeouts adapted to slow remote server load spikes
        rawDriver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(45));
        rawDriver.manage().timeouts().scriptTimeout(Duration.ofSeconds(30));

        driver.set(rawDriver);
        getDriver().get("https://practice.expandtesting.com/notes/app/login");
    }

    @AfterMethod
    public void tearDown() {
        if (getDriver() != null) {
            try {
                getDriver().quit();
            } finally {
                // Critical clean-up loop: Frees memory and drops thread slots to avoid cross-contamination
                driver.remove();
            }
        }
    }
}