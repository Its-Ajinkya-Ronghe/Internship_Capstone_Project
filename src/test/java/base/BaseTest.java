package base;

import com.expandtesting.notes.drivers.DriverFactory;
import com.expandtesting.notes.utils.ConfigReader;
import org.openqa.selenium.WebDriver;
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
        boolean isHeadless  = System.getenv("JENKINS_HOME") != null;

        // If -DuseGrid=true is passed → connect to Selenium Grid
        // Otherwise → run locally as before
        boolean useGrid = Boolean.parseBoolean(
                System.getProperty("useGrid", "false")
        );

        WebDriver rawDriver;

        if (useGrid) {
            String gridUrl = System.getProperty(
                    "gridUrl",
                    ConfigReader.getProperty("selenium.grid.url")
            );
            rawDriver = DriverFactory.createRemoteDriver(gridUrl);
        } else {
            rawDriver = DriverFactory.createChromeDriver(isHeadless);
        }

        rawDriver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(45));
        rawDriver.manage().timeouts().scriptTimeout(Duration.ofSeconds(30));

        driver.set(rawDriver);
        getDriver().get(ConfigReader.getProperty("ui.base.url"));
    }

    @AfterMethod
    public void tearDown() {
        if (getDriver() != null) {
            try {
                getDriver().quit();
            } finally {
                driver.remove();
            }
        }
    }
}