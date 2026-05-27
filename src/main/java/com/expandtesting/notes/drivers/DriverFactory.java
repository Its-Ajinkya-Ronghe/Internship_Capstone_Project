package com.expandtesting.notes.drivers;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

/**
 * DriverFactory — Centralized WebDriver creation with WebDriverManager auto-setup.
 * Satisfies the /drivers module requirement from Section 2.1.
 */
public class DriverFactory {

    private static final Logger log = LogManager.getLogger(DriverFactory.class);

    private DriverFactory() {
        // Utility class — no instantiation
    }

    /**
     * Creates and returns a fully configured ChromeDriver instance.
     * WebDriverManager automatically downloads the correct chromedriver binary.
     */
    public static WebDriver createChromeDriver(boolean headless) {
        log.info("DriverFactory: Setting up ChromeDriver via WebDriverManager.");
        WebDriverManager.chromedriver().setup();

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
            log.info("DriverFactory: Headless mode enabled (CI/CD environment detected).");
            options.addArguments("--headless=new");
        }

        log.info("DriverFactory: ChromeDriver instance created successfully.");
        return new ChromeDriver(options);
    }
}