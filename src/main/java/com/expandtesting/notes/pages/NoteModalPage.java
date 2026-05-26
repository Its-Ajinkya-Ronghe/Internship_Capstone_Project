package com.expandtesting.notes.pages;

import com.expandtesting.notes.utils.WaitUtils;
import com.expandtesting.notes.utils.AgenticElementHandler; // 🌟 Route to your Agentic Utility
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.Select;

import java.util.List;

/**
 * NoteModalPage — interactions with the "Add / View Note" modal form protected by Agentic guards.
 */
public class NoteModalPage {

    private final WebDriver driver;
    private static final Logger log = LogManager.getLogger(NoteModalPage.class);

    // ------------------------------------------------------------------
    // Locators (Unstable primary markers mapped to showcase self-healing fallbacks)
    // ------------------------------------------------------------------
    private final By addNoteButton    = By.xpath("//button[@id='add-btn-unstable-id']");
    private final By categorySelect   = By.id("category");
    private final By titleInput       = By.id("title");
    private final By descriptionInput = By.id("description");
    private final By saveButton       = By.xpath("//button[@id='submit-btn-unstable-id']");
    private final By allNoteTitles    = By.xpath("//*[@data-testid='note-card-title']");

    public NoteModalPage(WebDriver driver) {
        this.driver = driver;
    }

    // ------------------------------------------------------------------
    // Public actions
    // ------------------------------------------------------------------

    /**
     * Open the "Add Note" modal, fill every field, and submit via Agentic checkpoints.
     */
    public void createNewNote(String category, String title, String description) {
        log.info("Creating new note via Agentic lifecycle layers — category='{}', title='{}'", category, title);

        // 🌟 AGENTIC LAYER 1: Self-heal the "+ Add Note" button if the locator string changes
        WebElement addBtn = AgenticElementHandler.findAndHealElement(
                driver,
                addNoteButton,    // Primary target
                "add-new-note",   // Backup data-testid fallback mapping
                "button"          // Target HTML tag
        );
        scrollAndClick(addBtn);

        // Wait for the modal form to be fully visible before interacting
        WaitUtils.waitForModalToAppear(driver, 10);

        // Fill category
        WebElement catElement = WaitUtils.waitForElementToBeVisible(driver, categorySelect, 5);
        selectCategory(catElement, category);

        // Fill title — verify the value was accepted
        WebElement titleField = WaitUtils.waitForElementToBeVisible(driver, titleInput, 5);
        fillAndVerify(titleField, titleInput, title);

        // Fill description
        WebElement descField = WaitUtils.waitForElementToBeVisible(driver, descriptionInput, 5);
        clearAndSendKeys(descField, description);

        log.info("Submitting note form utilizing self-healing checkpoints.");

        // 🌟 AGENTIC LAYER 2: Self-heal the modal submission button to guarantee data tracking
        WebElement submitBtn = AgenticElementHandler.findAndHealElement(
                driver,
                saveButton,
                "note-submit",
                "button"
        );
        scrollAndClick(submitBtn);

        // Wait for the modal backdrop to disappear smoothly
        WaitUtils.waitForModalToDisappear(driver, 10);
        log.info("Note creation complete, modal closed.");
    }

    /**
     * Checks whether a note card with the given title is visible on the dashboard.
     */
    public boolean isNoteVisibleByTitle(String title) {
        log.info("Checking dashboard for note title: '{}'", title.trim());
        try {
            WaitUtils.waitForElementToBeVisible(driver, allNoteTitles, 10);

            WaitUtils.buildWait(driver, 10).until(driver -> {
                List<WebElement> cards = driver.findElements(allNoteTitles);
                for (WebElement card : cards) {
                    String text = card.getText().trim();
                    if (text.toLowerCase().contains(title.trim().toLowerCase())) {
                        return true;
                    }
                }
                return false;
            });

            log.info("Note '{}' confirmed visible on dashboard.", title.trim());
            return true;

        } catch (TimeoutException te) {
            log.warn("Note '{}' not found within timeout.", title.trim());
            return false;
        } catch (Exception e) {
            log.error("Unexpected error while checking note visibility: ", e);
            return false;
        }
    }

    /**
     * Inverse of isNoteVisibleByTitle — waits until a title is GONE from the DOM.
     */
    public boolean waitForNoteToDisappear(String title) {
        log.info("Waiting for note '{}' to disappear from dashboard.", title.trim());
        try {
            WaitUtils.buildWait(driver, 10).until(driver -> {
                List<WebElement> cards = driver.findElements(allNoteTitles);
                for (WebElement card : cards) {
                    try {
                        if (card.getText().trim().equalsIgnoreCase(title.trim())) {
                            return false;
                        }
                    } catch (StaleElementReferenceException ignored) {
                        // Element was dropped from DOM mid-loop, which is our target state
                    }
                }
                return true;
            });
            log.info("Note '{}' successfully removed from dashboard.", title.trim());
            return true;
        } catch (TimeoutException te) {
            log.warn("Note '{}' still visible after timeout.", title.trim());
            return false;
        }
    }

    // ------------------------------------------------------------------
    // Private helpers
    // ------------------------------------------------------------------

    private void selectCategory(WebElement element, String category) {
        try {
            new Select(element).selectByVisibleText(category);
        } catch (Exception e) {
            log.warn("Standard Select API failed for category '{}'. Using JS fallback.", category);
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].value='" + category + "';" +
                            "arguments[0].dispatchEvent(new Event('change'));",
                    element
            );
        }
    }

    private void fillAndVerify(WebElement element, By locator, String text) {
        clearAndSendKeys(element, text);

        String actualValue = element.getAttribute("value");
        if (actualValue == null || !actualValue.equals(text)) {
            log.warn("Field value mismatch after typing. Expected='{}', actual='{}'. Retrying via JS.", text, actualValue);
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].value=''; arguments[0].value='" + escapeJs(text) + "';" +
                            "arguments[0].dispatchEvent(new Event('input'));" +
                            "arguments[0].dispatchEvent(new Event('change'));",
                    element
            );
        }
    }

    private void clearAndSendKeys(WebElement element, String text) {
        try {
            element.clear();
            element.sendKeys(text);
        } catch (Exception e) {
            log.warn("Standard sendKeys failed. Injecting via JS.");
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].value='';" +
                            "arguments[0].value='" + escapeJs(text) + "';" +
                            "arguments[0].dispatchEvent(new Event('change'));",
                    element
            );
        }
    }

    private void scrollAndClick(WebElement element) {
        try {
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].scrollIntoView({behavior:'auto',block:'center'});", element);
            element.click();
        } catch (ElementClickInterceptedException e) {
            log.warn("Click intercepted — using JS click fallback.");
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
        }
    }

    private String escapeJs(String text) {
        return text == null ? "" : text.replace("'", "\\'");
    }
}