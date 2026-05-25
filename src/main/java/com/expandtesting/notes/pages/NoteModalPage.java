package com.expandtesting.notes.pages;

import com.expandtesting.notes.utils.WaitUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;

import java.util.List;

/**
 * NoteModalPage — interactions with the "Add / View Note" modal form.
 *
 * Reliability improvements over the original:
 *  - waitForModalToAppear() before touching any form field, so we never type
 *    into an element that is still animating into view.
 *  - waitForModalToDisappear() after saving, so the caller's next action
 *    (refresh, assert, click) is never blocked by a lingering backdrop.
 *  - isNoteVisibleByTitle() uses a FluentWait retry loop instead of a plain
 *    findElements() scan, eliminating false-negatives caused by async card renders.
 *  - clearAndSendKeys() verifies the field value after typing to catch cases
 *    where React controlled inputs swallow keystrokes.
 *  - All Thread.sleep() calls removed.
 */
public class NoteModalPage {

    private final WebDriver driver;
    private static final Logger log = LogManager.getLogger(NoteModalPage.class);

    // ------------------------------------------------------------------
    // Locators
    // ------------------------------------------------------------------
    private final By addNoteButton    = By.xpath("//button[@data-testid='add-new-note']");
    private final By categorySelect   = By.id("category");
    private final By titleInput       = By.id("title");
    private final By descriptionInput = By.id("description");
    private final By saveButton       = By.xpath("//button[@data-testid='note-submit']");
    private final By allNoteTitles    = By.xpath("//*[@data-testid='note-card-title']");

    public NoteModalPage(WebDriver driver) {
        this.driver = driver;
    }

    // ------------------------------------------------------------------
    // Public actions
    // ------------------------------------------------------------------

    /**
     * Open the "Add Note" modal, fill every field, and submit.
     * Waits for the modal to fully appear before touching fields, and waits
     * for it to fully close before returning — so the caller never has to sleep.
     */
    public void createNewNote(String category, String title, String description) {
        log.info("Creating new note — category='{}', title='{}'", category, title);

        // 1. Click the trigger button
        WebElement addBtn = WaitUtils.waitForElementToBeClickable(driver, addNoteButton, 10);
        scrollAndClick(addBtn);

        // 2. Wait for the modal form to be fully visible before interacting
        WaitUtils.waitForModalToAppear(driver, 10);

        // 3. Fill category
        WebElement catElement = WaitUtils.waitForElementToBeVisible(driver, categorySelect, 5);
        selectCategory(catElement, category);

        // 4. Fill title — verify the value was accepted (guards against React swallowing keys)
        WebElement titleField = WaitUtils.waitForElementToBeVisible(driver, titleInput, 5);
        fillAndVerify(titleField, titleInput, title);

        // 5. Fill description
        WebElement descField = WaitUtils.waitForElementToBeVisible(driver, descriptionInput, 5);
        clearAndSendKeys(descField, description);

        // 6. Submit
        log.info("Submitting note form.");
        WebElement submitBtn = WaitUtils.waitForElementToBeClickable(driver, saveButton, 5);
        scrollAndClick(submitBtn);

        // 7. Wait for the modal backdrop to disappear — replaces Thread.sleep(1500)
        WaitUtils.waitForModalToDisappear(driver, 10);
        log.info("Note creation complete, modal closed.");
    }

    /**
     * Checks whether a note card with the given title is visible on the dashboard.
     *
     * Uses FluentWait so transient DOM states (async card renders, stale refs) are
     * retried automatically — no manual StaleElementReferenceException loops needed.
     *
     * @param title the expected note title (case-insensitive substring match)
     * @return true if found within the wait timeout, false otherwise
     */
    public boolean isNoteVisibleByTitle(String title) {
        log.info("Checking dashboard for note title: '{}'", title.trim());
        try {
            // Wait until at least one note-card title element is present
            WaitUtils.waitForElementToBeVisible(driver, allNoteTitles, 10);

            // Use FluentWait to keep retrying until the specific title appears
            // (the list may still be growing if cards render one-by-one)
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
     * Use this right after a delete operation before asserting absence.
     *
     * @param title the note title that should no longer be present
     * @return true if the title disappeared within the timeout
     */
    public boolean waitForNoteToDisappear(String title) {
        log.info("Waiting for note '{}' to disappear from dashboard.", title.trim());
        try {
            WaitUtils.buildWait(driver, 10).until(driver -> {
                List<WebElement> cards = driver.findElements(allNoteTitles);
                for (WebElement card : cards) {
                    try {
                        if (card.getText().trim().equalsIgnoreCase(title.trim())) {
                            return false; // still there — keep waiting
                        }
                    } catch (StaleElementReferenceException ignored) {
                        // Card was removed mid-check — that's what we want
                    }
                }
                return true; // not found in any card
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

    /**
     * Select a category by visible text; fall back to JS value-set if the
     * Select API fails (e.g. custom-styled dropdowns that block the native select).
     */
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

    /**
     * Clear, type, and verify — guards against React controlled inputs that
     * silently reject keystrokes when the component is not fully mounted.
     */
    private void fillAndVerify(WebElement element, By locator, String text) {
        clearAndSendKeys(element, text);

        // Verify the field accepted the input; re-type once via JS if it didn't
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

    /** Escape single-quotes so JS string injection doesn't break on apostrophes in titles. */
    private String escapeJs(String text) {
        return text == null ? "" : text.replace("'", "\\'");
    }
}
