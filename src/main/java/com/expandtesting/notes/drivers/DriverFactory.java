package com.expandtesting.notes.drivers;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import java.net.MalformedURLException;
import java.net.URL;

public class DriverFactory {

    private static final Logger log = LogManager.getLogger(DriverFactory.class);

    private DriverFactory() {}

    /**
     * Local Chrome — used during normal local/CI runs.
     */
    public static WebDriver createChromeDriver(boolean headless) {
        log.info("DriverFactory: Creating local ChromeDriver.");
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = buildChromeOptions(headless);
        return new ChromeDriver(options);
    }

    /**
     * Remote Chrome via Selenium Grid — used when -DuseGrid=true is passed.
     */
    public static WebDriver createRemoteDriver(String gridUrl) {
        log.info("DriverFactory: Connecting to Selenium Grid at: {}", gridUrl);

        ChromeOptions options = buildChromeOptions(true); // always headless on Grid

        try {
            return new RemoteWebDriver(new URL(gridUrl), options);
        } catch (MalformedURLException e) {
            log.error("Invalid Selenium Grid URL: {}", gridUrl);
            throw new RuntimeException("Selenium Grid URL is malformed: " + gridUrl, e);
        }
    }

    /**
     * Shared Chrome options used by both local and remote drivers.
     */
    private static ChromeOptions buildChromeOptions(boolean headless) {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--start-maximized");
        options.addArguments("--disable-notifications");
        options.addArguments("--disable-renderer-backgrounding");
        options.addArguments("--disable-backgrounding-occluded-windows");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.setPageLoadStrategy(PageLoadStrategy.EAGER);

        if (headless) {
            log.info("DriverFactory: Headless mode ON.");
            options.addArguments("--headless=new");
        }

        return options;
    }
}