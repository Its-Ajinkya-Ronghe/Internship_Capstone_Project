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
 * UITest — Data-driven UI functional test suite.
 *
 * TC-05, TC-07, TC-08 were failing with:
 *   "timeout: Timed out receiving message from renderer: 29.930"
 *   Command: refresh {}
 *
 * This is NOT a Selenium wait bug. It means Chrome's renderer process
 * timed out waiting for practice.expandtesting.com to respond during
 * navigate().refresh(). The 30s pageLoad timeout in BaseTest was too
 * tight for this public practice server.
 *
 * Fixes:
 *   1. pageLoad raised to 60s in BaseTest.
 *   2. navigate().refresh() wrapped in safeRefresh() which retries once
 *      on TimeoutException — a single slow response won't kill the test.
 *   3. Chrome renderer flags added to BaseTest to prevent backgrounding.
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

        log.info("▶ Executing: {} — {}", testCaseId, description);

        LoginPage     loginPage     = new LoginPage(getDriver());
        NoteModalPage noteModal     = new NoteModalPage(getDriver());
        DashboardPage dashboardPage = new DashboardPage(getDriver());

        // BaseTest.setUp() always opens /login before each row — clean login every time
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

            // TC-UI-02: Invalid credentials → error message must match
            case "TC-UI-02":
                Assert.assertEquals(
                        loginPage.getErrorMessageText().trim(),
                        expectedStatus.trim(),
                        "FR-01 Failure: Login error message mismatch."
                );
                break;

            // TC-UI-03 / TC-UI-05: Create note → card must appear after refresh
            case "TC-UI-03":
            case "TC-UI-05":
                waitForDashboardReady();
                noteModal.createNewNote(uiCategory, uiTitle, uiDescription);
                safeRefresh();           // ← retry-wrapped refresh (fixes TC-05 renderer timeout)
                waitForDashboardReady();
                Assert.assertTrue(
                        noteModal.isNoteVisibleByTitle(uiTitle),
                        "FR-02 Failure: Note card '" + uiTitle + "' missing after creation."
                );
                break;

            // TC-UI-04: Create note → validation/confirmation text in page
            case "TC-UI-04":
                waitForDashboardReady();
                noteModal.createNewNote("Home", uiTitle, uiDescription);
                Assert.assertTrue(
                        getDriver().getPageSource().contains(expectedStatus),
                        "FR-02 Failure: Validation message '" + expectedStatus + "' not found."
                );
                break;

            // TC-UI-06: Create → edit → updated title visible
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

            // TC-UI-07: Create → delete → card must be gone
            case "TC-UI-07":
                waitForDashboardReady();
                noteModal.createNewNote(uiCategory, uiTitle, uiDescription);
                safeRefresh();           // ← retry-wrapped refresh (fixes TC-07 renderer timeout)
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

            // TC-UI-08: Create → filter → note still visible
            case "TC-UI-08":
                waitForDashboardReady();
                noteModal.createNewNote(uiCategory, uiTitle, uiDescription);
                safeRefresh();           // ← retry-wrapped refresh (fixes TC-08 renderer timeout)
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
    // Private helpers
    // -----------------------------------------------------------------------

    /**
     * Two-stage dashboard readiness:
     *   Stage 1 — wait for URL to contain "app" (redirect complete)
     *   Stage 2 — wait for add-note button clickable (React grid mounted)
     */
    private void waitForDashboardReady() {
        log.info("Waiting for dashboard — Stage 1: URL redirect.");
        WaitUtils.waitForUrlToContain(getDriver(), "app", 15);
        log.info("Waiting for dashboard — Stage 2: React grid mount.");
        WaitUtils.waitForElementToBeClickable(getDriver(), ADD_NOTE_BUTTON, 15);
        log.info("Dashboard fully ready.");
    }

    /**
     * Refresh with one automatic retry on renderer timeout.
     *
     * practice.expandtesting.com is a public practice server that occasionally
     * responds slowly. A single retry absorbs one bad response without failing
     * the test. If the retry also times out, the exception propagates normally
     * so the failure is still visible and reported correctly.
     */
    private void safeRefresh() {
        log.info("Refreshing page.");
        try {
            getDriver().navigate().refresh();
        } catch (TimeoutException e) {
            log.warn("Renderer timeout on first refresh attempt. Retrying once.");
            try {
                getDriver().navigate().refresh();
                log.info("Retry refresh succeeded.");
            } catch (TimeoutException retryEx) {
                log.error("Retry refresh also timed out. Server may be unavailable.");
                throw retryEx; // propagate so TestNG marks the test as failed
            }
        }
    }
}
