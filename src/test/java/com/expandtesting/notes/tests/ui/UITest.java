package com.expandtesting.notes.tests.ui;

import base.BaseTest;
import com.expandtesting.notes.pages.DashboardPage;
import com.expandtesting.notes.pages.LoginPage;
import com.expandtesting.notes.pages.NoteModalPage;
import com.expandtesting.notes.utils.ExcelReader;
import com.expandtesting.notes.utils.WaitUtils;
import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * UITest — Data-driven UI functional test suite.
 *
 * Why previous versions kept failing:
 *
 *  1. implicitWait(5s) in BaseTest conflicted with FluentWait in WaitUtils.
 *     When both run together, Selenium merges their polling in undefined ways —
 *     FluentWait's until() lambdas get delayed by implicitWait before throwing
 *     NoSuchElementException, so a 5s FluentWait effectively became 10s+ and
 *     negative conditions (element gone) never resolved correctly.
 *     FIX → implicitWait removed from BaseTest entirely.
 *
 *  2. Session-aware branching was wrong.
 *     BaseTest.setUp() navigates to /login before EVERY test row, so
 *     currentUrl is ALWAYS "/login" at the start of each row — the
 *     currentUrl.contains("/app") branch never fired. The else-branch
 *     always ran, calling loginPage.login() correctly, but the email
 *     field timed out because implicitWait was fighting FluentWait.
 *     FIX → session-aware branching removed. Every row does a clean
 *     login (BaseTest guarantees the starting URL is /login).
 *
 *  3. waitForDashboardReady() was calling waitForUrlToContain("app") after
 *     login but before the redirect completed for slow rows.
 *     FIX → still in place, now reliable without implicitWait interference.
 */
public class UITest extends BaseTest {

    private static final org.apache.logging.log4j.Logger log =
            org.apache.logging.log4j.LogManager.getLogger(UITest.class);

    // The add-note button being clickable = dashboard grid fully mounted by React.
    // This is the reliable readiness signal after login redirect or page refresh.
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

        // ----------------------------------------------------------------
        // Step 1 — Login
        //
        // BaseTest.setUp() opens /login before every single row.
        // No session-aware branching needed — always start with a fresh login.
        //
        // TC-UI-01 → valid credentials  → redirect to /app
        // TC-UI-02 → invalid credentials → stay on /login, show error
        // TC-UI-03 to TC-UI-08 → valid credentials → redirect to /app
        // ----------------------------------------------------------------
        loginPage.login(username, password);

        // ----------------------------------------------------------------
        // Step 2 — Test case routing
        // ----------------------------------------------------------------
        switch (testCaseId) {

            // TC-UI-01: Successful login → URL must contain "app"
            case "TC-UI-01":
                WaitUtils.waitForUrlToContain(getDriver(), "app", 15);
                Assert.assertTrue(
                        getDriver().getCurrentUrl().contains("app"),
                        "FR-01 Failure: Post-login URL does not contain 'app'."
                );
                break;

            // TC-UI-02: Invalid credentials → error message must match expected
            case "TC-UI-02":
                Assert.assertEquals(
                        loginPage.getErrorMessageText().trim(),
                        expectedStatus.trim(),
                        "FR-01 Failure: Login error message mismatch."
                );
                break;

            // TC-UI-03 / TC-UI-05: Create note → card must appear on dashboard
            case "TC-UI-03":
            case "TC-UI-05":
                waitForDashboardReady();
                noteModal.createNewNote(uiCategory, uiTitle, uiDescription);
                // Refresh confirms the note actually persisted to the server
                getDriver().navigate().refresh();
                waitForDashboardReady();
                Assert.assertTrue(
                        noteModal.isNoteVisibleByTitle(uiTitle),
                        "FR-02 Failure: Note card '" + uiTitle + "' missing from dashboard after creation."
                );
                break;

            // TC-UI-04: Submit note → validation/confirmation text in page source
            case "TC-UI-04":
                waitForDashboardReady();
                noteModal.createNewNote("Home", uiTitle, uiDescription);
                Assert.assertTrue(
                        getDriver().getPageSource().contains(expectedStatus),
                        "FR-02 Failure: Validation message '" + expectedStatus + "' not found in page source."
                );
                break;

            // TC-UI-06: Create → edit → updated title must be visible
            case "TC-UI-06":
                waitForDashboardReady();
                noteModal.createNewNote(uiCategory, uiTitle, uiDescription);
                // createNewNote() waits for modal to fully close before returning

                String modifiedTitle = uiTitle + " - Updated";
                dashboardPage.clickEditNoteIcon(uiTitle);
                // clickEditNoteIcon() waits for edit modal to open before returning

                dashboardPage.updateNoteDetails(modifiedTitle, uiDescription + " - Modified Content String");
                // updateNoteDetails() waits for modal to close before returning

                Assert.assertTrue(
                        noteModal.isNoteVisibleByTitle(modifiedTitle),
                        "FR-02 Failure: Updated title '" + modifiedTitle + "' not visible on dashboard."
                );
                break;

            // TC-UI-07: Create → delete → card must be gone
            case "TC-UI-07":
                waitForDashboardReady();
                noteModal.createNewNote(uiCategory, uiTitle, uiDescription);
                getDriver().navigate().refresh();
                waitForDashboardReady();

                Assert.assertTrue(
                        noteModal.isNoteVisibleByTitle(uiTitle),
                        "Pre-condition failure: Note '" + uiTitle + "' not found before delete attempt."
                );

                dashboardPage.clickDeleteNoteIcon(uiTitle);
                // clickDeleteNoteIcon() confirms deletion and waits for modal to close

                Assert.assertTrue(
                        noteModal.waitForNoteToDisappear(uiTitle),
                        "FR-07 Failure: Note card '" + uiTitle + "' still visible after deletion."
                );
                break;

            // TC-UI-08: Create → filter by category → note must still be visible
            case "TC-UI-08":
                waitForDashboardReady();
                noteModal.createNewNote(uiCategory, uiTitle, uiDescription);
                getDriver().navigate().refresh();
                waitForDashboardReady();

                dashboardPage.filterNotesByCategory(uiCategory);
                // filterNotesByCategory() waits for active-filter DOM marker before returning

                Assert.assertTrue(
                        noteModal.isNoteVisibleByTitle(uiTitle),
                        "FR-03 Failure: Note '" + uiTitle + "' not visible after applying category filter."
                );
                break;

            default:
                Assert.fail("Unmapped test case ID: " + testCaseId
                        + ". Add a case block or update the data sheet.");
        }

        log.info("✔ Passed: {}", testCaseId);
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /**
     * Two-stage dashboard readiness check.
     *
     * Stage 1 — waitForUrlToContain("app"):
     *   Blocks until the post-login redirect completes. Without this,
     *   Stage 2 runs while still on /login where the button doesn't exist.
     *
     * Stage 2 — waitForElementToBeClickable(ADD_NOTE_BUTTON):
     *   Blocks until React has fully mounted the dashboard grid.
     *   document.readyState fires before React renders components —
     *   the add-note button being clickable is the true readiness signal.
     */
    private void waitForDashboardReady() {
        log.info("Stage 1 — waiting for redirect to /app.");
        WaitUtils.waitForUrlToContain(getDriver(), "app", 15);
        log.info("Stage 2 — waiting for dashboard grid to mount.");
        WaitUtils.waitForElementToBeClickable(getDriver(), ADD_NOTE_BUTTON, 15);
        log.info("Dashboard fully ready.");
    }
}
