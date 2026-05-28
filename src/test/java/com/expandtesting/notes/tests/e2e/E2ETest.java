package com.expandtesting.notes.tests.e2e;

import base.BaseAPI;
import base.BaseTest;
import com.expandtesting.notes.pages.LoginPage;
import com.expandtesting.notes.pages.NoteModalPage;
import com.expandtesting.notes.utils.ExcelReader;
import com.expandtesting.notes.utils.PerformanceUtils; // 🌟 Section 3.5 Performance Integration
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.hamcrest.Matchers;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import java.util.HashMap;
import java.util.Map;

/**
 * E2ETest — Cross-layer UI and API dynamic synchronization test suite.
 * Cleaned from hardcoded simulation blocks to prepare for native framework scaling.
 */
public class E2ETest extends BaseTest {

    private static final org.apache.logging.log4j.Logger log = org.apache.logging.log4j.LogManager.getLogger(E2ETest.class);
    private static final long PERFORMANCE_SLA_MS = 2000L; // ⏱️ Section 3.5 SLA Limit

    @BeforeClass
    public void setupAPILayer() {
        BaseAPI.initializeAPI();
        BaseAPI.loginAndSetToken();
    }

    @DataProvider(name = "E2EExcelData")
    public Object[][] getE2EData() {
        String excelPath = System.getProperty("user.dir") + "/src/test/resources/TestData.xlsx";
        return ExcelReader.getSheetData(excelPath, "E2ETest");
    }

    @Test(dataProvider = "E2EExcelData")
    public void validateCrossLayerSynchronization(
            String testCaseId, String scenarioId, String description,
            String username, String password,
            String uiCategory, String uiTitle, String uiDescription,
            String expectedStatus) throws InterruptedException {

        log.info("▶ Executing Hybrid E2E Layer: " + testCaseId + " -> " + description);

        // Instantiating standard page object layers natively
        LoginPage loginPage = new LoginPage(getDriver());
        NoteModalPage noteModal = new NoteModalPage(getDriver());

        // 🌟 FIXED: Direct, standard Page Object UI authentication call (Bypassed simulation manager)
        loginPage.login(username, password);

        switch (testCaseId) {

            // TC-E2E-01: UI Note Creation -> API Verification Consistency Flow
            case "TC-E2E-01":
                // 🌟 FIXED: Replaced simulated tool dispatcher with native page object invocation
                noteModal.createNewNote(uiCategory, uiTitle, uiDescription);

                Response response = RestAssured.given()
                        .spec(BaseAPI.requestSpec)
                        .header("x-auth-token", BaseAPI.authToken)
                        .get("/notes");

                Assert.assertEquals(response.getStatusCode(), 200, "Backend API pipeline failed to reach database records.");

                String backendTitle = response.jsonPath().getString("data.find { it.title.trim() == '" + uiTitle.trim() + "' }.title");
                String backendDesc = response.jsonPath().getString("data.find { it.title.trim() == '" + uiTitle.trim() + "' }.description");

                Assert.assertEquals(backendTitle.trim(), uiTitle.trim(), "UI to API verification failed: Title mismatch.");
                Assert.assertEquals(backendDesc.trim(), uiDescription.trim(), "UI to API verification failed: Description mismatch.");
                break;

            // TC-E2E-02: Reverse Dynamic Sync Execution (API Deletion -> UI Ghost Data Check)
            case "TC-E2E-02":
                String notePayload = String.format("{\"title\":\"%s\",\"description\":\"%s\",\"category\":\"%s\"}",
                        uiTitle, uiDescription, uiCategory);

                Response createResp = RestAssured.given()
                        .spec(BaseAPI.requestSpec)
                        .header("x-auth-token", BaseAPI.authToken)
                        .body(notePayload)
                        .post("/notes");

                String noteId = createResp.jsonPath().getString("data.id");

                getDriver().navigate().refresh();

                Assert.assertTrue(noteModal.isNoteVisibleByTitle(uiTitle),
                        "E2E Test Setup Failure: Pre-requisite note element was not displayed on the dashboard workspace layer.");

                Response deleteResp = RestAssured.given()
                        .spec(BaseAPI.requestSpec)
                        .header("x-auth-token", BaseAPI.authToken)
                        .delete("/notes/" + noteId);

                Assert.assertEquals(deleteResp.getStatusCode(), 200, "API Delete operation rejected code blocks.");

                boolean isNoteStillPresent = noteModal.isNoteVisibleByTitle(uiTitle);
                Assert.assertFalse(isNoteStillPresent,
                        "BUG-002: Note deleted via API remains visible on the UI Dashboard layout (Dynamic Sync/Ghost Data Failure).");
                break;

            // TC-E2E-03: Validate Stable Batch Creation Flow
            case "TC-E2E-03":
                String[] categories = uiCategory.split("\\|");
                String[] titles = uiTitle.split("\\|");
                String[] descriptions = uiDescription.split("\\|");

                for (int i = 0; i < titles.length; i++) {
                    log.info("Batch processing UI note context index array entry [" + i + "]: " + titles[i]);
                    // 🌟 FIXED: Direct native call inside execution loops
                    noteModal.createNewNote(categories[i].trim(), titles[i].trim(), descriptions[i].trim());
                }

                Response multiResponse = RestAssured.given()
                        .spec(BaseAPI.requestSpec)
                        .header("x-auth-token", BaseAPI.authToken)
                        .get("/notes");

                Assert.assertEquals(multiResponse.getStatusCode(), 200, "Backend failed list retrieval validation.");

                for (String targetTitle : titles) {
                    String fetchedTitle = multiResponse.jsonPath().getString("data.find { it.title.trim() == '" + targetTitle.trim() + "' }.title");
                    Assert.assertNotNull(fetchedTitle, "Batch Synchronization Failure: Target title [" + targetTitle + "] is missing from API database cluster registry.");
                }
                break;

            // TC-E2E-04: UI-Driven Hybrid Synchronization Complete 5-Step Pipeline
            case "TC-E2E-04":
                // 🌟 FIXED: Direct native execution mapping
                noteModal.createNewNote(uiCategory, uiTitle, uiDescription);

                Response getResponse = RestAssured.given()
                        .spec(BaseAPI.requestSpec)
                        .header("x-auth-token", BaseAPI.authToken)
                        .get("/notes");

                Assert.assertEquals(getResponse.getStatusCode(), 200, "Failed to fetch notes list via backend API.");

                String e2eNoteId = getResponse.jsonPath().getString("data.find { it.title.trim() == '" + uiTitle.trim() + "' }.id");
                String e2eBackendTitle = getResponse.jsonPath().getString("data.find { it.title.trim() == '" + uiTitle.trim() + "' }.title");

                Assert.assertNotNull(e2eNoteId, "API Verification Error: Note created via UI was not found in the backend database.");
                Assert.assertEquals(e2eBackendTitle.trim(), uiTitle.trim(), "API Verification Error: Title mismatch on backend.");

                Response cleanDeleteResp = RestAssured.given()
                        .spec(BaseAPI.requestSpec)
                        .header("x-auth-token", BaseAPI.authToken)
                        .delete("/notes/" + e2eNoteId);

                Assert.assertEquals(cleanDeleteResp.getStatusCode(), 200);

                log.info("Executing optimized JavaScript non-blocking view refresh to enforce cross-layer synchronization.");
                ((org.openqa.selenium.JavascriptExecutor) getDriver()).executeScript("history.go(0);");

                boolean isSyncNotePresent = noteModal.isNoteVisibleByTitle(uiTitle);
                Assert.assertFalse(isSyncNotePresent,
                        "Sync Error: Note was deleted via the backend API and page refreshed, but the element is still visible on the UI dashboard!");
                break;

            // TC-E2E-05: Backend API POST -> UI Rendering Instant Synchronization Validation
            case "TC-E2E-05":
                String batchPayload = String.format("{\"title\":\"%s\",\"description\":\"%s\",\"category\":\"%s\"}",
                        uiTitle, uiDescription, uiCategory);

                Response postResp = RestAssured.given()
                        .spec(BaseAPI.requestSpec)
                        .header("x-auth-token", BaseAPI.authToken)
                        .body(batchPayload)
                        .post("/notes");

                Assert.assertEquals(postResp.getStatusCode(), 200, "API Note pre-requisite creation failed.");

                boolean isNoteInstantlyVisible = noteModal.isNoteVisibleByTitle(uiTitle);
                Assert.assertTrue(isNoteInstantlyVisible,
                        "BUG-005: Note created via API POST does not appear instantly on the UI layout grid (Dynamic Sync Failure).");
                break;

            // TC-E2E-06: Backend API PUT -> UI Modification Real-time Synchronization
            case "TC-E2E-06":
                String originalTitle = uiTitle;
                String updatedTitle = uiTitle + " - API Updated String";

                String initPayload = String.format("{\"title\":\"%s\",\"description\":\"%s\",\"category\":\"%s\"}",
                        originalTitle, uiDescription, uiCategory);

                Response initCreateResp = RestAssured.given()
                        .spec(BaseAPI.requestSpec)
                        .header("x-auth-token", BaseAPI.authToken)
                        .body(initPayload)
                        .post("/notes");

                String targetId = initCreateResp.jsonPath().getString("data.id");

                getDriver().navigate().refresh();
                Assert.assertTrue(noteModal.isNoteVisibleByTitle(originalTitle), "Pre-condition: Note card failed to render on UI workspace layout.");

                String modPayload = String.format("{\"title\":\"%s\",\"description\":\"%s\",\"category\":\"%s\",\"completed\":false}",
                        updatedTitle, uiDescription, uiCategory);

                Response putResp = RestAssured.given()
                        .spec(BaseAPI.requestSpec)
                        .header("x-auth-token", BaseAPI.authToken)
                        .body(modPayload)
                        .put("/notes/" + targetId);

                Assert.assertEquals(putResp.getStatusCode(), 200, "API PUT data update operation was rejected.");

                boolean isUpdatedTitleVisible = noteModal.isNoteVisibleByTitle(updatedTitle);
                Assert.assertTrue(isUpdatedTitleVisible,
                        "BUG-006: Note updates pushed via API PUT do not reflect dynamically on the UI Dashboard layout.");
                break;

            default:
                Assert.fail("Automation Architecture Alert: No execution routing logic defined for Test Case ID: " + testCaseId);
        }

        // 🌟 SECTION 3.5: Capture and log the UI timing performance metrics
        PerformanceUtils.collectUiPerformanceMetrics(getDriver(), testCaseId);
        log.info("✔ Passed Cross-Layer Validation Flow: {}", testCaseId);
    }
}