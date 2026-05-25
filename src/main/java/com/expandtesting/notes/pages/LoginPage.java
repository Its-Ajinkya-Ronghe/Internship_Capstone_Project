package com.expandtesting.notes.pages;

import com.expandtesting.notes.utils.WaitUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class LoginPage {

    private final WebDriver driver;
    private static final Logger log = LogManager.getLogger(LoginPage.class);

    // Locators
    private final By emailInput        = By.id("email");
    private final By passwordInput     = By.id("password");
    private final By loginSubmitButton = By.xpath("//button[@type='submit']");
    private final By errorMessageAlert = By.xpath("//div[@data-testid='alert-message']");

    public LoginPage(WebDriver driver) {
        this.driver = driver;
    }

    // =================================================================
    // Public API
    // =================================================================

    /**
     * Full login sequence.
     *
     * BaseTest.setUp() navigates to /login before every test row, so the
     * login page is always the starting point. This method does NOT need to
     * navigate — it just waits for the form to be ready and fills it in.
     *
     * Timeout raised to 15s on the email field to absorb any slow initial
     * page load without relying on implicitWait (which is intentionally
     * removed from BaseTest because it conflicts with FluentWait).
     */
    public void login(String email, String password) {
        log.info("Login sequence starting for: {}", email);
        enterUsername(email);
        enterPassword(password);
        clickSubmit();
        log.info("Login form submitted.");
    }

    public String getErrorMessageText() {
        log.debug("Fetching error alert text.");
        // Raised to 10s — the server round-trip for an invalid login takes time
        return WaitUtils.waitForElementToBeVisible(driver, errorMessageAlert, 10).getText();
    }

    public String getCurrentPageUrl() {
        return driver.getCurrentUrl();
    }

    // =================================================================
    // Private actions
    // =================================================================

    private void enterUsername(String email) {
        log.info("Entering email.");
        // 15s timeout: absorbs slow initial page loads without implicitWait
        WebElement emailElement = WaitUtils.waitForElementToBeVisible(driver, emailInput, 15);
        clearAndType(emailElement, email);
    }

    private void enterPassword(String password) {
        log.info("Entering password.");
        WebElement passwordElement = WaitUtils.waitForElementToBeVisible(driver, passwordInput, 10);
        clearAndType(passwordElement, password);
    }

    private void clickSubmit() {
        log.info("Clicking submit.");
        WebElement submitBtn = WaitUtils.waitForElementToBeClickable(driver, loginSubmitButton, 10);
        scrollToCenter(submitBtn);
        try {
            submitBtn.click();
        } catch (Exception e) {
            log.warn("Standard click intercepted. Using JS click fallback.");
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", submitBtn);
        }
    }

    private void clearAndType(WebElement element, String text) {
        try {
            element.clear();
            element.sendKeys(text);
        } catch (Exception e) {
            log.warn("sendKeys failed. Injecting via JS.");
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript("arguments[0].value='';", element);
            js.executeScript("arguments[0].value='" + text + "';", element);
            js.executeScript("arguments[0].dispatchEvent(new Event('input'));", element);
            js.executeScript("arguments[0].dispatchEvent(new Event('change'));", element);
        }
    }

    private void scrollToCenter(WebElement element) {
        try {
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].scrollIntoView({behavior:'auto',block:'center'});", element);
        } catch (Exception e) {
            log.warn("Scroll failed (non-fatal): {}", e.getMessage());
        }
    }
}
