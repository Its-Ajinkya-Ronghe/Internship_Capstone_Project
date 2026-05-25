package com.expandtesting.notes.tests.e2e;

import base.BaseAPI;
import base.BaseTest;
import com.expandtesting.notes.pages.LoginPage;
import com.expandtesting.notes.pages.NoteModalPage;
import com.expandtesting.notes.utils.ExcelReader;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class E2ETest extends BaseTest {

    private static final org.apache.logging.log4j.Logger log = org.apache.logging.log4j.LogManager.getLogger(E2ETest.class);

    @BeforeClass
    public void setupAPILayer() {
        BaseAPI.initializeAPI();
        BaseAPI.loginAndSetToken();
    }

    @DataProvider(name = "E2EExcelData")
    public Object[][] getE2EData() {
        String excelPath = System.getProperty("user.dir") + "/src/test/resources/TestData.xlsx";
        // Reads parameter matrices dynamically from the tab named 'E2EData'
        return ExcelReader.getSheetData(excelPath, "E2ETest");
    }

    @Test(dataProvider = "E2EExcelData")
    public void validateCrossLayerSynchronization(
            String testCaseId,
            String scenarioId,
            String description,
            String username,
            String password,
            String uiCategory,
            String uiTitle,
            String uiDescription,
            String expectedStatus) throws InterruptedException {

        log.info("Starting execution sequence for E2E Cross-Layer Pipeline: " + testCaseId + " -> " + description);

        // =================================================================
        // STEP 1: GLOBAL BASELINE LOGIN (UI Authentication)
        // =================================================================
        LoginPage loginPage = new LoginPage(getDriver());
        loginPage.login(username, password);

        NoteModalPage noteModal = new NoteModalPage(getDriver());

        // =================================================================
        // 🔀 DYNAMIC TEST ROUTING LAYER BASED ON TEST CASE ID
        // =================================================================

        if (testCaseId.equals("TC-E2E-01")) {
            // 🔄 TC-E2E-01: UI -> API CONSISTENCY VERIFICATION
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

        } else if (testCaseId.equals("TC-E2E-02")) {
            // 🔒 TC-E2E-02: REVERSE SYNC - NOTE DELETION EXPOSURE (API -> UI)
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

            RestAssured.given()
                    .spec(BaseAPI.requestSpec)
                    .header("x-auth-token", BaseAPI.authToken)
                    .delete("/notes/" + noteId)
                    .then()
                    .statusCode(200);

            // Verifies visibility WITHOUT refreshing browser frame context to expose BUG-002
            boolean isNoteStillPresent = noteModal.isNoteVisibleByTitle(uiTitle);
            Assert.assertFalse(isNoteStillPresent,
                    "BUG-002: Note deleted via API remains visible on the UI Dashboard layout (Dynamic Sync/Ghost Data Failure).");

        } else if (testCaseId.equals("TC-E2E-03")) {
            // =================================================================
            // 📊 TC-E2E-03: VALIDATE MULTIPLE UI NOTES IN API (STABLE BATCH)
            // =================================================================
            String[] categories = uiCategory.split("\\|");
            String[] titles = uiTitle.split("\\|");
            String[] descriptions = uiDescription.split("\\|");

            for (int i = 0; i < titles.length; i++) {
                log.info("Batch producing UI note index row item [" + i + "]: " + titles[i]);

                // 1. Submit the note form fields completely
                noteModal.createNewNote(categories[i].trim(), titles[i].trim(), descriptions[i].trim());

                // 🌟 THE CRITICAL STABILITY FIX: Give the frontend app layer
                // a dedicated window to close the pop-up and paint the new grid card
                Thread.sleep(2000);

                // Alternative check if you have a loader/backdrop locator:
                // WaitUtils.waitForElementToBeInvisibile(driver, By.className("modal-backdrop"), 5);
            }

            // 2. Fire backend snapshot query to confirm presence of all elements
            Response multiResponse = RestAssured.given()
                    .spec(BaseAPI.requestSpec)
                    .header("x-auth-token", BaseAPI.authToken)
                    .get("/notes");

            Assert.assertEquals(multiResponse.getStatusCode(), 200, "Backend failed list retrieval validation.");

            for (String targetTitle : titles) {
                String fetchedTitle = multiResponse.jsonPath().getString("data.find { it.title.trim() == '" + targetTitle.trim() + "' }.title");
                Assert.assertNotNull(fetchedTitle, "Batch Synchronization Failure: Target title [" + targetTitle + "] is missing from API database cluster registry.");
            }
        } else if (testCaseId.equals("TC-E2E-04")) {
            // 🔒 TC-E2E-04: UI-DRIVEN HYBRID SYNC COMPLETE 5-STEP PIPELINE

            // Step 2: Create Note (UI) [Step 1 Login executed globally at top]
            noteModal.createNewNote(uiCategory, uiTitle, uiDescription);

            // Step 3: Validate via API
            Response getResponse = RestAssured.given()
                    .spec(BaseAPI.requestSpec)
                    .header("x-auth-token", BaseAPI.authToken)
                    .get("/notes");

            Assert.assertEquals(getResponse.getStatusCode(), 200, "Failed to fetch notes via API.");

            String noteId = getResponse.jsonPath().getString("data.find { it.title.trim() == '" + uiTitle.trim() + "' }.id");
            String backendTitle = getResponse.jsonPath().getString("data.find { it.title.trim() == '" + uiTitle.trim() + "' }.title");

            Assert.assertNotNull(noteId, "API Verification Error: Note created via UI was not found in the backend database.");
            Assert.assertEquals(backendTitle.trim(), uiTitle.trim(), "API Verification Error: Title mismatch on backend.");

            // Step 4: Delete via API
            RestAssured.given()
                    .spec(BaseAPI.requestSpec)
                    .header("x-auth-token", BaseAPI.authToken)
                    .delete("/notes/" + noteId)
                    .then()
                    .statusCode(200);

            // Step 5: Refresh the page and Verify UI
            getDriver().navigate().refresh();
            Thread.sleep(2000); // UI frame transition buffer hold

            boolean isNoteStillPresent = noteModal.isNoteVisibleByTitle(uiTitle);
            Assert.assertFalse(isNoteStillPresent,
                    "Sync Error: Note was deleted via the backend API and page refreshed, but the element is still visible on the UI dashboard!");

        } else {
            // 🛑 CATCH-ALL ARCHITECTURE GUARD
            Assert.fail("Automation Architecture Alert: No execution routing logic defined for Test Case ID: " + testCaseId);
        }
    }
}