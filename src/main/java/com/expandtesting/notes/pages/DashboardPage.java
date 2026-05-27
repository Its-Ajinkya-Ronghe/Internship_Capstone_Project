package com.expandtesting.notes.pages;

import com.expandtesting.notes.utils.WaitUtils;
import com.expandtesting.notes.utils.AgenticElementHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;

public class DashboardPage {

    private final WebDriver driver;
    private static final Logger log = LogManager.getLogger(DashboardPage.class);

    private final By welcomeMessageHeader  = By.xpath("//h1[@id='welcome-header-unstable']");
    private final By logOutButton          = By.xpath("//button[@id='logout-btn-unstable']");
    private final By deleteAccountButton   = By.xpath("//button[@id='delete-acc-btn-unstable']");
    private final By editTitleInput        = By.id("title");
    private final By editDescriptionInput  = By.id("description");
    private final By saveChangesButton     = By.xpath("//button[@id='save-changes-unstable']");

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
    // Note interactions (Cleaned from fragile handlers)
    // ------------------------------------------------------------------

    public void clickEditNoteIcon(String noteTitle) {
        log.info("Clicking edit icon for note: '{}'", noteTitle);
        By editIconLocator = noteCardActionLocator(noteTitle, "delete"); // Dynamic relative xpath pivot

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        WebElement editIcon = wait.until(ExpectedConditions.elementToBeClickable(editIconLocator));
        clickElementRobustly(editIcon);

        WaitUtils.waitForElementToBeVisible(driver, editTitleInput, 10);
    }

    public void updateNoteDetails(String newTitle, String newDescription) {
        WebElement titleField = WaitUtils.waitForElementToBeVisible(driver, editTitleInput, 5);
        titleField.clear();
        titleField.sendKeys(newTitle);

        WebElement descField = WaitUtils.waitForElementToBeVisible(driver, editDescriptionInput, 5);
        descField.clear();
        descField.sendKeys(newDescription);

        WebElement saveBtn = AgenticElementHandler.findAndHealElement(
                driver,
                saveChangesButton,
                "note-submit",
                "button"
        );
        clickElementRobustly(saveBtn);
        WaitUtils.waitForModalToDisappear(driver, 10);
    }

    /**
     * 🔥 FIXED: Native execution path completely free of failing attributes
     */
    public void clickDeleteNoteIcon(String noteTitle) {
        log.info("Executing clean UI deletion sequence for note: '{}'", noteTitle);

        // 🌟 FIXED XPATH: Relaxes element tag checks to match text string nodes safely inside card structures
        By nativeTrashIcon = By.xpath(String.format(
                "//div[@data-testid='note-card' and .//*[@data-testid='note-card-title' and normalize-space(text())='%s']]//button[@data-testid='note-delete']",
                noteTitle
        ));

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        // Locates and clicks the specific delete icon smoothly
        WebElement trashBtn = wait.until(ExpectedConditions.elementToBeClickable(nativeTrashIcon));
        clickElementRobustly(trashBtn);
        log.info("Trash icon clicked safely.");

        // Wait for the confirmation modal backdrop animation context to slide into place
        By modalFullyOpen = By.cssSelector(".modal.show, .modal.fade.show, div.modal");
        wait.until(ExpectedConditions.visibilityOfElementLocated(modalFullyOpen));

        // Select and submit the confirmation button safely via javascript layer
        By realDeleteConfirmLocator = By.xpath("//button[@data-testid='note-delete-confirm']");
        WebElement confirmBtn = wait.until(ExpectedConditions.elementToBeClickable(realDeleteConfirmLocator));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", confirmBtn);

        wait.until(ExpectedConditions.invisibilityOfElementLocated(modalFullyOpen));
        log.info("Delete action completed seamlessly.");
    }

    public void filterNotesByCategory(String categoryName) {
        String normalised = categoryName.substring(0, 1).toUpperCase() + categoryName.substring(1).toLowerCase();
        By filterLocator = By.xpath("//*[@id='unstable-category-" + normalised.toLowerCase() + "']");

        WebElement tabFilter = AgenticElementHandler.findAndHealElement(
                driver,
                filterLocator,
                "category-" + normalised.toLowerCase(),
                "*"
        );
        clickElementRobustly(tabFilter);
    }

    public void clickLogOut() {
        WebElement logoutBtn = AgenticElementHandler.findAndHealElement(driver, logOutButton, "logout-button", "button");
        clickElementRobustly(logoutBtn);
        WaitUtils.waitForUrlToContain(driver, "login", 10);
    }

    public void clickDeleteAccountLink() {
        WebElement delBtn = AgenticElementHandler.findAndHealElement(driver, deleteAccountButton, "delete-account-button", "button");
        clickElementRobustly(delBtn);
    }

    private By noteCardActionLocator(String noteTitle, String actionType) {
        return By.xpath(String.format(
                "//div[@data-testid='note-card' and .//strong[normalize-space(text())='%s']]//button[@data-testid='note-%s']",
                noteTitle, actionType
        ));
    }

    private void clickElementRobustly(WebElement element) {
        try {
            element.click();
        } catch (ElementClickInterceptedException e) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
        }
    }
}