package com.expandtesting.notes.pages;

import com.expandtesting.notes.utils.AgenticElementHandler; // 🌟 Route to your Agentic Utility
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

    // Unstable primary locators (Demonstrates dynamic healing under heavy parallel strain)
    private final By emailInput        = By.id("email-unstable-id");
    private final By passwordInput     = By.id("password-unstable-id");
    private final By loginSubmitButton = By.xpath("//button[@id='submit-unstable-id']");
    private final By errorMessageAlert = By.xpath("//div[@id='alert-unstable-id']");

    public LoginPage(WebDriver driver) {
        this.driver = driver;
    }

    // =================================================================
    // Public API
    // =================================================================

    /**
     * Full login sequence protected by Agentic self-healing locators.
     */
    public void login(String email, String password) {
        log.info("Login sequence starting under Agentic validation layers for: {}", email);
        enterUsername(email);
        enterPassword(password);
        clickSubmit();
        log.info("Login form submitted successfully.");
    }

    public String getErrorMessageText() {
        log.debug("Fetching error alert text using Agentic checkpoints.");
        // 🌟 AGENTIC LAYER 4: Safe recovery for error message alerts if template elements shift
        WebElement errorAlert = AgenticElementHandler.findAndHealElement(
                driver,
                errorMessageAlert,
                "alert-message", // Backup data-testid fallback attribute
                "div"
        );
        return errorAlert.getText();
    }

    public String getCurrentPageUrl() {
        return driver.getCurrentUrl();
    }

    // =================================================================
    // Private actions
    // =================================================================

    private void enterUsername(String email) {
        log.info("Entering email attribute.");
        // 🌟 AGENTIC LAYER 1: Proactively self-heals the email input field
        WebElement emailElement = AgenticElementHandler.findAndHealElement(
                driver,
                emailInput,
                "login-email", // Backup data-testid fallback attribute
                "input"
        );
        clearAndType(emailElement, email);
    }

    private void enterPassword(String password) {
        log.info("Entering password attribute.");
        // 🌟 AGENTIC LAYER 2: Proactively self-heals the password input field
        WebElement passwordElement = AgenticElementHandler.findAndHealElement(
                driver,
                passwordInput,
                "login-password", // Backup data-testid fallback attribute
                "input"
        );
        clearAndType(passwordElement, password);
    }

    private void clickSubmit() {
        log.info("Clicking submit button.");
        // 🌟 AGENTIC LAYER 3: Proactively self-heals the login submit button
        WebElement submitBtn = AgenticElementHandler.findAndHealElement(
                driver,
                loginSubmitButton,
                "login-submit", // Backup data-testid fallback attribute
                "button"
        );

        scrollToCenter(submitBtn);
        try {
            submitBtn.click();
        } catch (Exception e) {
            log.warn("Standard button click intercepted by background overlays. Activating JS fallback click.");
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", submitBtn);
        }
    }

    private void clearAndType(WebElement element, String text) {
        try {
            element.clear();
            element.sendKeys(text);
        } catch (Exception e) {
            log.warn("Standard typing broke due to element focus loss. Injecting string natively via JS execution.");
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
            log.warn("Scroll routine bypassed (non-fatal error): {}", e.getMessage());
        }
    }
}