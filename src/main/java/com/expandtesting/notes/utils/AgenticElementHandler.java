package com.expandtesting.notes.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.FluentWait;
import java.time.Duration;
import java.util.List;

public class AgenticElementHandler {
    private static final Logger log = LogManager.getLogger(AgenticElementHandler.class);

    /**
     * Agentic Self-Healing interaction utility.
     * Tries a primary locator with intelligent waiting. If it fails, it executes
     * a decision-based healing scan using backup locator properties.
     */
    public static WebElement findAndHealElement(WebDriver driver, By primaryLocator, String backupTestId, String backupTag) {
        FluentWait<WebDriver> wait = new FluentWait<>(driver)
                .withTimeout(Duration.ofSeconds(6))
                .pollingEvery(Duration.ofMillis(300))
                .ignoring(NoSuchElementException.class)
                .ignoring(StaleElementReferenceException.class);

        try {
            // 1. Intelligent Waiting System
            log.info("Agentic scan searching for primary element locator: " + primaryLocator.toString());
            return wait.until(d -> d.findElement(primaryLocator));
        } catch (TimeoutException e) {
            log.warn("⚠️ Primary locator failed. Initializing Self-Healing agent layer...");

            // 2. Decision-Based Self-Healing Fallback Execution
            try {
                String healingXpath = String.format("//%s[@data-testid='%s']", backupTag, backupTestId);
                log.info("Self-healing system analyzing backup DOM mapping: " + healingXpath);

                WebElement healedElement = driver.findElement(By.xpath(healingXpath));

                log.warn("✅ Self-Healing Success! Restored broken locator target via data-testid fallback matrix.");
                return healedElement;
            } catch (Exception healingError) {
                log.error("🛑 Self-healing agent was unable to recover element structure.");
                throw new NoSuchElementException("Agentic Element Handler failed across primary and secondary attributes.", e);
            }
        }
    }
}