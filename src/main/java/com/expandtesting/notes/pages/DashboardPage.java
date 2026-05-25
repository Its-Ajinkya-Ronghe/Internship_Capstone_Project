package com.expandtesting.notes.pages;

import com.expandtesting.notes.utils.WaitUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.*;

public class DashboardPage {

    private final WebDriver driver;
    private static final Logger log = LogManager.getLogger(DashboardPage.class);

    // Locators
    private final By welcomeMessageHeader  = By.xpath("//h1[@data-testid='user-profile-title']");
    private final By logOutButton          = By.xpath("//button[@data-testid='logout-button']");
    private final By deleteAccountButton   = By.xpath("//button[@data-testid='delete-account-button']");
    private final By editTitleInput        = By.id("title");
    private final By editDescriptionInput  = By.id("description");
    private final By saveChangesButton     = By.xpath("//button[@data-testid='note-submit']");
    private final By modalConfirmDeleteBtn = By.xpath("//button[@data-testid='note-delete-confirm']");

    public DashboardPage(WebDriver driver) {
        this.driver = driver;
    }

    public boolean isWelcomeHeaderDisplayed() {
        return WaitUtils.waitForElementToBeVisible(driver, welcomeMessageHeader, 10).isDisplayed();
    }

    // ------------------------------------------------------------------
    // Note interactions
    // ------------------------------------------------------------------

    /**
     * Click the edit icon and wait for the edit modal's title field to be
     * visible and clearable before returning.
     *
     * Fix for TC-UI-06 AssertionError:
     *   waitForModalToAppear() was using a CSS selector that doesn't match
     *   this app's modal structure (.modal.show / [role=dialog][aria-modal=true]).
     *   The modal opened but the wait timed out or resolved too early, so
     *   updateNoteDetails() started typing into fields that weren't ready yet —
     *   the title field still held the old value and the save registered it
     *   as-is, so the updated title never appeared on the dashboard.
     *
     *   Fix: wait directly for the title INPUT inside the edit modal to be
     *   visible — that is the exact field updateNoteDetails() writes to first,
     *   and its visibility proves the modal is fully open and interactive.
     */
    public void clickEditNoteIcon(String noteTitle) {
        log.info("Clicking edit icon for note: '{}'", noteTitle);
        By editIcon = noteCardActionLocator(noteTitle, "note-title");
        executeSafeAction(() -> clickElementRobustly(editIcon), "Edit Icon: " + noteTitle);

        // Wait directly for the title input inside the edit modal —
        // replaces waitForModalToAppear() which was failing on this app's modal CSS
        WaitUtils.waitForElementToBeVisible(driver, editTitleInput, 10);
        log.info("Edit modal open — title field visible and ready.");
    }

    /**
     * Fill the edit modal fields and save.
     * Clears each field explicitly and verifies the title was accepted
     * before clicking save — guards against React controlled inputs that
     * silently reject keystrokes when the component isn't fully mounted.
     */
    public void updateNoteDetails(String newTitle, String newDescription) {
        log.info("Updating note — newTitle='{}'", newTitle);

        // Title field: clear + type + verify value was accepted
        WebElement titleField = WaitUtils.waitForElementToBeVisible(driver, editTitleInput, 5);
        titleField.clear();
        titleField.sendKeys(newTitle);

        // Verify the field accepted the input — retry via JS if it didn't
        String actualTitle = titleField.getAttribute("value");
        if (actualTitle == null || !actualTitle.equals(newTitle)) {
            log.warn("Title field value mismatch after typing. Expected='{}', actual='{}'. Retrying via JS.", newTitle, actualTitle);
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].value='';" +
                            "arguments[0].value='" + escapeJs(newTitle) + "';" +
                            "arguments[0].dispatchEvent(new Event('input'));" +
                            "arguments[0].dispatchEvent(new Event('change'));",
                    titleField
            );
        }

        // Description field
        WebElement descField = WaitUtils.waitForElementToBeVisible(driver, editDescriptionInput, 5);
        descField.clear();
        descField.sendKeys(newDescription);

        // Save and wait for modal to fully close
        clickElementRobustly(saveChangesButton);
        WaitUtils.waitForModalToDisappear(driver, 10);
        log.info("Note update saved, modal closed.");
    }

    /**
     * Click delete icon → wait for confirm button → JS click confirm →
     * wait for modal to fully close.
     *
     * JS click used on confirm button because Bootstrap's modal fade animation
     * can still be running when the button becomes "clickable" in Selenium's
     * eyes — standard element.click() hits the animating backdrop instead.
     */
    public void clickDeleteNoteIcon(String noteTitle) {
        log.info("Clicking delete icon for note: '{}'", noteTitle);
        By deleteIcon = noteCardActionLocator(noteTitle, "note-delete");
        executeSafeAction(() -> clickElementRobustly(deleteIcon), "Delete Icon: " + noteTitle);

        // Wait for confirm button to be visible in DOM
        WebElement confirmBtn = WaitUtils.waitForElementToBeVisible(driver, modalConfirmDeleteBtn, 10);
        log.info("Delete confirmation dialog appeared.");

        // Wait for modal animation to finish (.modal.show = Bootstrap fade complete)
        By modalFullyOpen = By.cssSelector(".modal.show, .modal.fade.show");
        try {
            WaitUtils.waitForElementToBeVisible(driver, modalFullyOpen, 5);
            log.info("Modal animation complete.");
        } catch (TimeoutException te) {
            log.warn("Modal .show class not detected — proceeding with JS click anyway.");
        }

        // JS click bypasses any remaining animation overlay
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", confirmBtn);

        WaitUtils.waitForModalToDisappear(driver, 10);
        log.info("Delete confirmed and modal closed.");
    }

    /**
     * Click a category filter tab and wait for the active-filter DOM marker
     * before returning so the caller's assertion sees the filtered state.
     */
    public void filterNotesByCategory(String categoryName) {
        String normalised = categoryName.substring(0, 1).toUpperCase()
                + categoryName.substring(1).toLowerCase();
        By filterLocator = By.xpath("//*[@data-testid='category-" + normalised.toLowerCase() + "']");

        log.info("Applying category filter: '{}'", normalised);
        executeSafeAction(() -> clickElementRobustly(filterLocator), "Filter: " + normalised);

        By activeFilterLocator = By.xpath(
                "//*[@data-testid='category-" + normalised.toLowerCase() + "' and " +
                        "(contains(@class,'active') or @aria-selected='true' or @aria-current='true')]"
        );
        try {
            WaitUtils.waitForElementToBeVisible(driver, activeFilterLocator, 5);
            log.info("Category filter '{}' confirmed active.", normalised);
        } catch (TimeoutException te) {
            log.warn("Active filter marker not detected for '{}'. Continuing.", normalised);
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

    private By noteCardActionLocator(String noteTitle, String buttonTestId) {
        return By.xpath(String.format(
                "//*[@data-testid='note-card-title' and normalize-space(text())='%s']" +
                        "/ancestor::div[@data-testid='note-card']" +
                        "//button[@data-testid='%s']",
                noteTitle, buttonTestId
        ));
    }

    private void clickElementRobustly(By locator) {
        WebElement element = WaitUtils.waitForElementToBeClickable(driver, locator, 10);
        try {
            element.click();
        } catch (ElementClickInterceptedException e) {
            log.warn("Click intercepted for [{}]. Using JS click fallback.", locator);
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
        }
    }

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
                try { Thread.sleep(300); } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private String escapeJs(String text) {
        return text == null ? "" : text.replace("'", "\\'");
    }
}
