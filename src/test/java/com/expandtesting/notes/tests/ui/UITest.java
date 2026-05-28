package com.expandtesting.notes.tests.ui;

import base.BaseTest;
import com.expandtesting.notes.pages.DashboardPage;
import com.expandtesting.notes.pages.LoginPage;
import com.expandtesting.notes.pages.NoteModalPage;
import com.expandtesting.notes.utils.ExcelReader;
import com.expandtesting.notes.utils.WaitUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * UITest — Data-driven UI functional validation test suite.
 * Fully migrated to native page object invocation loops for robust execution tracking.
 */
public class UITest extends BaseTest {

    private static final Logger log = LogManager.getLogger(UITest.class);
    private static final By ADD_NOTE_BUTTON = By.xpath("//button[@data-testid='add-new-note']");

    @DataProvider(name = "UITestExcelData")
    public Object[][] getUITestDataFromExcel() {
        return ExcelReader.getSheetData(
                System.getProperty("user.dir") + "/src/test/resources/TestData.xlsx",
                "UITest"
        );
    }

    @Test(dataProvider = "UITestExcelData")
    public void validateComprehensiveUIFunctionalFlows(
            String testCaseId, String scenarioId, String description,
            String username, String password,
            String uiCategory, String uiTitle, String uiDescription,
            String expectedStatus) {

        log.info("▶ Executing UI Layer: {} — {}", testCaseId, description);

        LoginPage loginPage         = new LoginPage(getDriver());
        NoteModalPage noteModal     = new NoteModalPage(getDriver());
        DashboardPage dashboardPage = new DashboardPage(getDriver());

        // 🌟 FIXED: Standard native UI authentication call (Bypassed the hardcoded simulation block)
        loginPage.login(username, password);

        switch (testCaseId) {

            // TC-UI-01: Valid login → URL must contain "app"
            case "TC-UI-01":
                WaitUtils.waitForUrlToContain(getDriver(), "app", 15);
                Assert.assertTrue(
                        getDriver().getCurrentUrl().contains("app"),
                        "FR-01 Failure: Post-login URL does not contain 'app'."
                );
                break;

            // TC-UI-02: Invalid credentials → error message mismatch check
            case "TC-UI-02":
                Assert.assertEquals(
                        loginPage.getErrorMessageText().trim(),
                        expectedStatus.trim(),
                        "FR-01 Failure: Login error message mismatch."
                );
                break;

            // TC-UI-03 / TC-UI-05: Create note → card visible after safe refresh
            case "TC-UI-03":
            case "TC-UI-05":
                waitForDashboardReady();

                // 🌟 FIXED: Native page object execution block
                noteModal.createNewNote(uiCategory, uiTitle, uiDescription);

                safeRefresh();
                waitForDashboardReady();
                Assert.assertTrue(
                        noteModal.isNoteVisibleByTitle(uiTitle),
                        "FR-02 Failure: Note card '" + uiTitle + "' missing after creation."
                );
                break;

            // TC-UI-04: Create note → confirmation text page validation
            case "TC-UI-04":
                waitForDashboardReady();
                noteModal.createNewNote("Home", uiTitle, uiDescription);

                Assert.assertTrue(
                        getDriver().getPageSource().contains(expectedStatus),
                        "FR-02 Failure: Validation message '" + expectedStatus + "' not found."
                );
                break;

            // TC-UI-06: Create → edit → verify modification matches
            case "TC-UI-06":
                waitForDashboardReady();
                noteModal.createNewNote(uiCategory, uiTitle, uiDescription);

                String modifiedTitle = (uiTitle + " - Updated");
                dashboardPage.clickEditNoteIcon(uiTitle);
                dashboardPage.updateNoteDetails(modifiedTitle, uiDescription + " - Modified Content String");

                Assert.assertTrue(
                        noteModal.isNoteVisibleByTitle(modifiedTitle),
                        "FR-02 Failure: Updated title '" + modifiedTitle + "' not visible."
                );
                break;

            // TC-UI-07: Create → delete → card disappearance confirmation
            case "TC-UI-07":
                waitForDashboardReady();
                noteModal.createNewNote(uiCategory, uiTitle, uiDescription);

                safeRefresh();
                waitForDashboardReady();

                Assert.assertTrue(
                        noteModal.isNoteVisibleByTitle(uiTitle),
                        "Pre-condition failure: Note '" + uiTitle + "' not found before delete."
                );

                dashboardPage.clickDeleteNoteIcon(uiTitle);

                Assert.assertTrue(
                        noteModal.waitForNoteToDisappear(uiTitle),
                        "FR-07 Failure: Note '" + uiTitle + "' still visible after deletion."
                );
                break;

            // TC-UI-08: Create → filter layout by category
            case "TC-UI-08":
                waitForDashboardReady();
                noteModal.createNewNote(uiCategory, uiTitle, uiDescription);

                safeRefresh();
                waitForDashboardReady();

                dashboardPage.filterNotesByCategory(uiCategory);

                Assert.assertTrue(
                        noteModal.isNoteVisibleByTitle(uiTitle),
                        "FR-03 Failure: Note '" + uiTitle + "' not visible after category filter."
                );
                break;

            default:
                Assert.fail("Unmapped test case ID: " + testCaseId);
        }

        log.info("✔ Passed: {}", testCaseId);
    }

    // -----------------------------------------------------------------------
    // Private Helpers
    // -----------------------------------------------------------------------

    /**
     * Two-stage dashboard readiness verification.
     */
    private void waitForDashboardReady() {
        log.info("Waiting for dashboard — Stage 1: URL redirect.");
        WaitUtils.waitForUrlToContain(getDriver(), "app", 15);
        log.info("Waiting for dashboard — Stage 2: React grid mount.");
        WaitUtils.waitForElementToBeClickable(getDriver(), ADD_NOTE_BUTTON, 15);
        log.info("Dashboard fully ready.");
    }

    /**
     * Refresh implementation featuring an automatic single-retry fallback loop for renderer timeouts.
     */
    private void safeRefresh() {
        log.info("Refreshing page viewport.");
        try {
            getDriver().navigate().refresh();
        } catch (TimeoutException e) {
            log.warn("Renderer timeout on first refresh attempt. Retrying once.");
            try {
                getDriver().navigate().refresh();
                log.info("Retry refresh succeeded.");
            } catch (TimeoutException retryEx) {
                log.error("Retry refresh also timed out. Server unreachable.");
                throw retryEx;
            }
        }
    }
}