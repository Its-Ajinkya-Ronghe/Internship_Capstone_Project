package com.expandtesting.notes.pages;

import com.expandtesting.notes.utils.WaitUtils;
import com.expandtesting.notes.utils.AgenticElementHandler; // 🌟 Route to your Agentic Utility
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.*;

public class DashboardPage {

    private final WebDriver driver;
    private static final Logger log = LogManager.getLogger(DashboardPage.class);

    // Unstable primary locators (Demonstrates dynamic healing under parallel strain)
    private final By welcomeMessageHeader  = By.xpath("//h1[@id='welcome-header-unstable']");
    private final By logOutButton          = By.xpath("//button[@id='logout-btn-unstable']");
    private final By deleteAccountButton   = By.xpath("//button[@id='delete-acc-btn-unstable']");
    private final By editTitleInput        = By.id("title");
    private final By editDescriptionInput  = By.id("description");
    private final By saveChangesButton     = By.xpath("//button[@id='save-changes-unstable']");
    private final By modalConfirmDeleteBtn = By.xpath("//button[@id='confirm-delete-unstable']");

    public DashboardPage(WebDriver driver) {
        this.driver = driver;
    }

    public boolean isWelcomeHeaderDisplayed() {
        log.info("Executing Agentic checkpoint on Dashboard Profile Header.");
        WebElement welcomeBanner = AgenticElementHandler.findAndHealElement(
                driver,
                welcomeMessageHeader,
                "user-profile-title",
                "h1"
        );
        return welcomeBanner.isDisplayed();
    }

    // ------------------------------------------------------------------
    // Note interactions
    // ------------------------------------------------------------------

    /**
     * Click the edit icon with self-healing guarantees and wait for the
     * edit modal's title input field to be visible before returning.
     */
    public void clickEditNoteIcon(String noteTitle) {
        log.info("Clicking edit icon for note utilizing Agentic guards: '{}'", noteTitle);
        By editIconLocator = noteCardActionLocator(noteTitle, "unstable-edit-id");

        // 🌟 AGENTIC LAYER 1: Self-heal the edit button if structural locator paths shift
        WebElement editIcon = AgenticElementHandler.findAndHealElement(
                driver,
                editIconLocator,
                "note-edit", // Shifting focus to target the card's native edit icon test-id
                "button"
        );
        clickElementRobustly(editIcon);

        WaitUtils.waitForElementToBeVisible(driver, editTitleInput, 10);
        log.info("Edit modal open — title field visible and ready.");
    }

    /**
     * Fill the edit modal fields and save.
     */
    public void updateNoteDetails(String newTitle, String newDescription) {
        log.info("Updating note — newTitle='{}'", newTitle);

        WebElement titleField = WaitUtils.waitForElementToBeVisible(driver, editTitleInput, 5);
        titleField.clear();
        titleField.sendKeys(newTitle);

        String actualTitle = titleField.getAttribute("value");
        if (actualTitle == null || !actualTitle.equals(newTitle)) {
            log.warn("Title field value mismatch after typing. Retrying via JS.");
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].value='';" +
                            "arguments[0].value='" + escapeJs(newTitle) + "';" +
                            "arguments[0].dispatchEvent(new Event('input'));" +
                            "arguments[0].dispatchEvent(new Event('change'));",
                    titleField
            );
        }

        WebElement descField = WaitUtils.waitForElementToBeVisible(driver, editDescriptionInput, 5);
        descField.clear();
        descField.sendKeys(newDescription);

        // 🌟 AGENTIC LAYER 2: Ensure modal save button handles modifications cleanly
        WebElement saveBtn = AgenticElementHandler.findAndHealElement(
                driver,
                saveChangesButton,
                "note-submit",
                "button"
        );
        clickElementRobustly(saveBtn);

        WaitUtils.waitForModalToDisappear(driver, 10);
        log.info("Note update saved, modal closed.");
    }

    /**
     * Click delete icon → wait for confirm button → JS click confirm → wait for close.
     */
    public void clickDeleteNoteIcon(String noteTitle) {
        log.info("Clicking delete icon for note utilizing Agentic guards: '{}'", noteTitle);
        By deleteIconLocator = noteCardActionLocator(noteTitle, "unstable-delete-id");

        // 🌟 AGENTIC LAYER 3: Protect deletion triggers from shifting ancestor node weights
        WebElement deleteIcon = AgenticElementHandler.findAndHealElement(
                driver,
                deleteIconLocator,
                "note-delete",
                "button"
        );
        clickElementRobustly(deleteIcon);

        // 🌟 AGENTIC LAYER 4: Ensure confirm button is resolved regardless of backdrop state
        WebElement confirmBtn = AgenticElementHandler.findAndHealElement(
                driver,
                modalConfirmDeleteBtn,
                "note-delete-confirm",
                "button"
        );
        log.info("Delete confirmation dialog appeared.");

        By modalFullyOpen = By.cssSelector(".modal.show, .modal.fade.show");
        try {
            WaitUtils.waitForElementToBeVisible(driver, modalFullyOpen, 5);
            log.info("Modal animation complete.");
        } catch (TimeoutException te) {
            log.warn("Modal .show class not detected — proceeding via JS fallback layer.");
        }

        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", confirmBtn);
        WaitUtils.waitForModalToDisappear(driver, 10);
        log.info("Delete confirmed and modal closed.");
    }

    /**
     * Click a category filter tab using active self-healing.
     */
    public void filterNotesByCategory(String categoryName) {
        String normalised = categoryName.substring(0, 1).toUpperCase()
                + categoryName.substring(1).toLowerCase();
        By filterLocator = By.xpath("//*[@id='unstable-category-" + normalised.toLowerCase() + "']");

        log.info("Applying category filter with Agentic healing: '{}'", normalised);

        // 🌟 AGENTIC LAYER 5: Protect navigation tab metrics from sudden re-renders
        WebElement tabFilter = AgenticElementHandler.findAndHealElement(
                driver,
                filterLocator,
                "category-" + normalised.toLowerCase(),
                "*"
        );
        clickElementRobustly(tabFilter);

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
        WebElement logoutBtn = AgenticElementHandler.findAndHealElement(
                driver,
                logOutButton,
                "logout-button",
                "button"
        );
        clickElementRobustly(logoutBtn);
        WaitUtils.waitForUrlToContain(driver, "login", 10);
    }

    public void clickDeleteAccountLink() {
        WebElement delBtn = AgenticElementHandler.findAndHealElement(
                driver,
                deleteAccountButton,
                "delete-account-button",
                "button"
        );
        clickElementRobustly(delBtn);
    }

    // ------------------------------------------------------------------
    // Private helpers
    // ------------------------------------------------------------------

    private By noteCardActionLocator(String noteTitle, String buttonTestId) {
        // Fallback-friendly XPath string mapping matrix
        return By.xpath(String.format(
                "//*[@data-testid='note-card-title' and normalize-space(text())='%s']" +
                        "/ancestor::div[@data-testid='note-card']" +
                        "//button",
                noteTitle
        ));
    }

    private void clickElementRobustly(WebElement element) {
        try {
            element.click();
        } catch (ElementClickInterceptedException e) {
            log.warn("Click intercepted. Using JS click fallback.");
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
        }
    }

    private String escapeJs(String text) {
        return text == null ? "" : text.replace("'", "\\'");
    }
}