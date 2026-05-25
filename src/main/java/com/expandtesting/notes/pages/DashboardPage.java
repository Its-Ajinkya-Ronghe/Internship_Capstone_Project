package com.expandtesting.notes.pages;

import com.expandtesting.notes.utils.WaitUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;

/**
 * DashboardPage — interactions with the main notes grid and navigation controls.
 *
 * Reliability improvements over the original:
 *  - clickDeleteNoteIcon() waits for the confirmation modal to appear before clicking
 *    the confirm button, and waits for it to disappear afterwards.
 *  - clickEditNoteIcon() waits for the edit modal to appear before returning,
 *    so the caller can immediately fill fields without sleeping.
 *  - updateNoteDetails() waits for the modal to close after saving.
 *  - filterNotesByCategory() waits for the filter to actually take effect (DOM
 *    updates to reflect the filtered state) before returning.
 *  - All Thread.sleep() calls removed.
 */
public class DashboardPage {

    private final WebDriver driver;
    private static final Logger log = LogManager.getLogger(DashboardPage.class);

    // ------------------------------------------------------------------
    // Locators
    // ------------------------------------------------------------------
    private final By welcomeMessageHeader = By.xpath("//h1[@data-testid='user-profile-title']");
    private final By logOutButton         = By.xpath("//button[@data-testid='logout-button']");
    private final By deleteAccountButton  = By.xpath("//button[@data-testid='delete-account-button']");
    private final By editTitleInput       = By.id("title");
    private final By editDescriptionInput = By.id("description");
    private final By saveChangesButton    = By.xpath("//button[@data-testid='note-submit']");
    private final By modalConfirmDeleteBtn = By.xpath("//button[@data-testid='note-delete-confirm']");
    private final By allNoteTitles        = By.xpath("//*[@data-testid='note-card-title']");

    public DashboardPage(WebDriver driver) {
        this.driver = driver;
    }

    // ------------------------------------------------------------------
    // Public assertions / queries
    // ------------------------------------------------------------------

    public boolean isWelcomeHeaderDisplayed() {
        return WaitUtils.waitForElementToBeVisible(driver, welcomeMessageHeader, 10).isDisplayed();
    }

    // ------------------------------------------------------------------
    // Note interactions
    // ------------------------------------------------------------------

    /**
     * Click the edit (pencil) icon on the note card that matches {@code noteTitle},
     * then waits for the edit modal to fully appear so the caller can fill fields immediately.
     */
    public void clickEditNoteIcon(String noteTitle) {
        log.info("Clicking edit icon for note: '{}'", noteTitle);
        By editIcon = noteCardActionLocator(noteTitle, "note-edit");
        executeSafeAction(() -> clickElementRobustly(editIcon), "Edit Icon: " + noteTitle);

        // Wait for the edit modal to appear — replaces the Thread.sleep(1500) that followed
        WaitUtils.waitForModalToAppear(driver, 10);
        log.info("Edit modal open for note: '{}'", noteTitle);
    }

    /**
     * Click the delete (trash) icon on the matching note card, wait for the confirmation
     * dialog to appear, confirm deletion, then wait for the modal to fully close.
     */
    public void clickDeleteNoteIcon(String noteTitle) {
        log.info("Clicking delete icon for note: '{}'", noteTitle);
        By deleteIcon = noteCardActionLocator(noteTitle, "note-delete");
        executeSafeAction(() -> clickElementRobustly(deleteIcon), "Delete Icon: " + noteTitle);

        // Wait for the confirmation modal to be clickable, not just visible
        WaitUtils.waitForElementToBeClickable(driver, modalConfirmDeleteBtn, 10);
        log.info("Delete confirmation modal appeared.");

        executeSafeAction(() -> clickElementRobustly(modalConfirmDeleteBtn), "Confirm Delete");

        // Wait for the modal and backdrop to clear — replaces Thread.sleep(1500)
        WaitUtils.waitForModalToDisappear(driver, 10);
        log.info("Delete confirmed and modal closed.");
    }

    /**
     * Fill the edit-modal fields with new values and save.
     * Waits for the modal to close before returning so the caller can assert immediately.
     */
    public void updateNoteDetails(String newTitle, String newDescription) {
        log.info("Updating note — newTitle='{}'", newTitle);

        WebElement titleField = WaitUtils.waitForElementToBeVisible(driver, editTitleInput, 5);
        titleField.clear();
        titleField.sendKeys(newTitle);

        WebElement descField = WaitUtils.waitForElementToBeVisible(driver, editDescriptionInput, 5);
        descField.clear();
        descField.sendKeys(newDescription);

        clickElementRobustly(saveChangesButton);

        // Wait for the modal to disappear after saving — replaces any post-call sleep
        WaitUtils.waitForModalToDisappear(driver, 10);
        log.info("Note update saved, modal closed.");
    }

    /**
     * Click a category filter tab and wait for the note grid to reflect the filter
     * before returning, so the caller's assertion sees the filtered state.
     *
     * @param categoryName the display name of the category (e.g. "Home", "Work")
     */
    public void filterNotesByCategory(String categoryName) {
        // Normalise to Title-case to match data-testid convention (e.g. "home" → "Home")
        String normalised = categoryName.substring(0, 1).toUpperCase()
                + categoryName.substring(1).toLowerCase();
        By filterLocator = By.xpath("//*[@data-testid='category-" + normalised.toLowerCase() + "']");

        log.info("Applying category filter: '{}'", normalised);
        executeSafeAction(() -> clickElementRobustly(filterLocator), "Filter: " + normalised);

        // Wait for the selected filter tab to carry an "active" attribute/class so we know
        // the DOM has reacted to the click before the caller starts asserting card visibility.
        By activeFilterLocator = By.xpath(
                "//*[@data-testid='category-" + normalised.toLowerCase() + "' and " +
                        "(contains(@class,'active') or @aria-selected='true' or @aria-current='true')]"
        );
        try {
            WaitUtils.waitForElementToBeVisible(driver, activeFilterLocator, 5);
            log.info("Category filter '{}' confirmed active in DOM.", normalised);
        } catch (TimeoutException te) {
            // The active marker selector may not match this app's markup — log a warning
            // but do NOT fail here; the actual note-visibility assertion will catch it.
            log.warn("Active filter marker not detected for '{}'. Continuing — " +
                    "assertion will confirm filter result.", normalised);
        }
    }

    // ------------------------------------------------------------------
    // Navigation
    // ------------------------------------------------------------------

    public void clickLogOut() {
        executeSafeAction(() -> clickElementRobustly(logOutButton), "Logout Button");
        WaitUtils.waitForUrlToContain(driver, "login", 10);
    }

    public void clickDeleteAccountLink() {
        executeSafeAction(() -> clickElementRobustly(deleteAccountButton), "Delete Account Link");
    }

    // ------------------------------------------------------------------
    // Private helpers
    // ------------------------------------------------------------------

    /**
     * Build a note-card-scoped action button locator.
     * Scopes to the specific card by title so it never clicks the wrong button
     * when multiple cards are on screen.
     */
    private By noteCardActionLocator(String noteTitle, String buttonTestId) {
        return By.xpath(String.format(
                "//*[@data-testid='note-card-title' and normalize-space(text())='%s']" +
                        "/ancestor::div[@data-testid='note-card']" +
                        "//button[@data-testid='%s']",
                noteTitle, buttonTestId
        ));
    }

    /**
     * Click with an ElementClickInterceptedException fallback to JS.
     * Covers ad overlays and partially-dismissed modal backdrops.
     */
    private void clickElementRobustly(By locator) {
        WebElement element = WaitUtils.waitForElementToBeClickable(driver, locator, 10);
        try {
            element.click();
        } catch (ElementClickInterceptedException e) {
            log.warn("Click intercepted for [{}]. Using JS click fallback.", locator);
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
        }
    }

    /**
     * Retry wrapper for StaleElementReferenceException — re-runs the action up to
     * 3 times with a short back-off between attempts.
     */
    private void executeSafeAction(Runnable action, String context) {
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                action.run();
                return;
            } catch (StaleElementReferenceException e) {
                if (attempt == 3) {
                    log.error("Action '{}' failed after 3 stale-element retries.", context);
                    throw e;
                }
                log.warn("Stale element on attempt {} for '{}'. Retrying...", attempt, context);
                try { Thread.sleep(300); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
            }
        }
    }
}
