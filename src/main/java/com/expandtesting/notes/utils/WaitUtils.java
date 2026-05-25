package com.expandtesting.notes.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * WaitUtils — Centralized, FluentWait-backed waiting utilities.
 *
 * Strategy:
 *  - FluentWait polls every 300 ms, ignoring transient StaleElementReferenceException
 *    and NoSuchElementException so callers never need try/catch for those.
 *  - All public helpers accept an explicit timeout (seconds) so individual call-sites
 *    can tune urgency without sprinkling Thread.sleep() across the codebase.
 *  - waitForModalToDisappear() is the single replacement for every post-action sleep
 *    that was previously guarding against lingering Bootstrap/React modal backdrops.
 */
public class WaitUtils {

    private static final Logger log = LogManager.getLogger(WaitUtils.class);

    // Polling cadence — fast enough to catch quick transitions, light on CPU
    private static final Duration POLL_INTERVAL = Duration.ofMillis(300);

    // -----------------------------------------------------------------------
    // Core factory
    // -----------------------------------------------------------------------

    /**
     * Build a FluentWait for the given driver with the supplied timeout.
     * Ignores the two most common transient DOM exceptions automatically.
     */
    public static FluentWait<WebDriver> buildWait(WebDriver driver, int timeoutSeconds) {
        return new FluentWait<>(driver)
                .withTimeout(Duration.ofSeconds(timeoutSeconds))
                .pollingEvery(POLL_INTERVAL)
                .ignoring(NoSuchElementException.class)
                .ignoring(StaleElementReferenceException.class);
    }

    // -----------------------------------------------------------------------
    // Element-level waits
    // -----------------------------------------------------------------------

    /**
     * Wait until the element located by {@code locator} is present in the DOM
     * AND visible on screen, then return it.
     */
    public static WebElement waitForElementToBeVisible(WebDriver driver, By locator, int timeoutSeconds) {
        log.debug("Waiting up to {}s for element to be visible: {}", timeoutSeconds, locator);
        return buildWait(driver, timeoutSeconds)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    /**
     * Wait until the element is visible AND enabled (not disabled/greyed-out),
     * then return it — safe to call before clicking buttons or select elements.
     */
    public static WebElement waitForElementToBeClickable(WebDriver driver, By locator, int timeoutSeconds) {
        log.debug("Waiting up to {}s for element to be clickable: {}", timeoutSeconds, locator);
        return buildWait(driver, timeoutSeconds)
                .until(ExpectedConditions.elementToBeClickable(locator));
    }

    /**
     * Wait until the element is either absent from the DOM or invisible.
     * Use this after dismissing modals, delete confirmations, or spinners.
     */
    public static void waitForElementToDisappear(WebDriver driver, By locator, int timeoutSeconds) {
        log.debug("Waiting up to {}s for element to disappear: {}", timeoutSeconds, locator);
        buildWait(driver, timeoutSeconds)
                .until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    /**
     * Wait until at least {@code minCount} elements matching {@code locator}
     * are present in the DOM. Returns as soon as the count is reached.
     */
    public static void waitForMinimumElementCount(WebDriver driver, By locator, int minCount, int timeoutSeconds) {
        log.debug("Waiting up to {}s for at least {} elements: {}", timeoutSeconds, minCount, locator);
        buildWait(driver, timeoutSeconds)
                .until(ExpectedConditions.numberOfElementsToBeMoreThan(locator, minCount - 1));
    }

    // -----------------------------------------------------------------------
    // Page / URL waits
    // -----------------------------------------------------------------------

    /**
     * Block until the current URL contains {@code fragment}.
     * Replaces post-login/post-navigation Thread.sleep() calls.
     */
    public static void waitForUrlToContain(WebDriver driver, String fragment, int timeoutSeconds) {
        log.debug("Waiting up to {}s for URL to contain: {}", timeoutSeconds, fragment);
        buildWait(driver, timeoutSeconds)
                .until(ExpectedConditions.urlContains(fragment));
    }

    /**
     * Wait until the browser reports document.readyState == "complete".
     * Call this after navigate().refresh() instead of sleeping.
     */
    public static void waitForPageToLoad(WebDriver driver, int timeoutSeconds) {
        log.debug("Waiting up to {}s for page to reach readyState=complete", timeoutSeconds);
        buildWait(driver, timeoutSeconds).until((ExpectedCondition<Boolean>) d -> {
            String state = ((JavascriptExecutor) d).executeScript("return document.readyState").toString();
            return "complete".equals(state);
        });
    }

    // -----------------------------------------------------------------------
    // Modal / overlay waits  ← key replacement for Thread.sleep() guards
    // -----------------------------------------------------------------------

    /**
     * Wait for ALL modal backdrops and animated overlays to disappear from the DOM.
     *
     * Covers:
     *  - Bootstrap 4/5  .modal-backdrop
     *  - React-Bootstrap / MUI  .MuiBackdrop-root
     *  - Generic  [role="dialog"]  containers
     *
     * Call this immediately after any action that opens or closes a modal so the
     * next interaction is never blocked by a lingering invisible overlay.
     */
    public static void waitForModalToDisappear(WebDriver driver, int timeoutSeconds) {
        log.debug("Waiting up to {}s for modal overlays to clear", timeoutSeconds);

        // Each backdrop selector is tried; we consider the modal gone when ALL are invisible.
        By[] backdropLocators = {
                By.cssSelector(".modal-backdrop"),
                By.cssSelector(".MuiBackdrop-root"),
                By.cssSelector("[role='dialog']")
        };

        for (By locator : backdropLocators) {
            try {
                buildWait(driver, timeoutSeconds)
                        .until(ExpectedConditions.invisibilityOfElementLocated(locator));
            } catch (TimeoutException te) {
                // Not every modal type is present on every page — log and continue.
                log.warn("Backdrop locator {} did not disappear within {}s (may not exist on this page).",
                        locator, timeoutSeconds);
            }
        }
    }

    /**
     * Wait for a specific modal to become fully visible before interacting with it.
     * Useful before filling in form fields inside a newly-opened modal dialog.
     */
    public static void waitForModalToAppear(WebDriver driver, int timeoutSeconds) {
        log.debug("Waiting up to {}s for modal to appear", timeoutSeconds);
        By modalBody = By.cssSelector(".modal.show, [role='dialog'][aria-modal='true']");
        buildWait(driver, timeoutSeconds)
                .until(ExpectedConditions.visibilityOfElementLocated(modalBody));
    }

    // -----------------------------------------------------------------------
    // Text / staleness helpers
    // -----------------------------------------------------------------------

    /**
     * Wait until the element's visible text contains {@code expectedText}.
     * Handles the case where a card title is painted asynchronously after creation.
     */
    public static void waitForTextToBePresentInElement(WebDriver driver, By locator,
                                                       String expectedText, int timeoutSeconds) {
        log.debug("Waiting up to {}s for text '{}' in element: {}", timeoutSeconds, expectedText, locator);
        buildWait(driver, timeoutSeconds)
                .until(ExpectedConditions.textToBePresentInElementLocated(locator, expectedText));
    }

    /**
     * Re-fetch the element from the DOM after a page mutation and return the fresh reference.
     * Use when an element is likely to go stale after a create/update/delete operation.
     */
    public static WebElement refreshElement(WebDriver driver, By locator, int timeoutSeconds) {
        log.debug("Re-fetching fresh element reference for: {}", locator);
        return waitForElementToBeVisible(driver, locator, timeoutSeconds);
    }
}
