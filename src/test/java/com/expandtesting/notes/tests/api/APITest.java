package com.expandtesting.notes.tests.api;

import base.BaseAPI;
import com.expandtesting.notes.pages.LoginPage;
import com.expandtesting.notes.pages.NoteModalPage;
import com.expandtesting.notes.utils.ExcelReader;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.hamcrest.Matchers;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class APITest extends BaseAPI {

    private static final org.apache.logging.log4j.Logger log = org.apache.logging.log4j.LogManager.getLogger(APITest.class);
    public void deleteAllNotesViaAPI() {
        log.info("Initiating global API purge sequence to delete all active notes.");

        // 1. Fetch all current notes for the authenticated user
        Response response = io.restassured.RestAssured.given()
                .spec(requestSpec)
                .header("x-auth-token", authToken)
                .get("/notes");

        if (response.getStatusCode() == 200) {
            // 2. Extract the list of all note IDs dynamically
            java.util.List<String> noteIds = response.jsonPath().getList("data.id");

            log.info("Found " + noteIds.size() + " active note(s) slated for erasure.");

            // 3. Loop through and delete each resource context element sequentially
            for (String id : noteIds) {
                io.restassured.RestAssured.given()
                        .spec(requestSpec)
                        .header("x-auth-token", authToken)
                        .delete("/notes/" + id)
                        .then()
                        .statusCode(200);
            }
            log.info("Global API purge complete. Dashboard workspace layer is completely clear.");
        } else {
            log.error("Failed to fetch notes list for cleanup. Status code: " + response.getStatusCode());
        }
    }

    @BeforeClass
    public void apiSetup() {
        initializeAPI();
        loginAndSetToken();
        deleteAllNotesViaAPI();
    }

    @DataProvider(name = "APIExcelDataProvider", parallel = false)
    public Object[][] getAPITestDataFromExcel() {
        String excelPath = System.getProperty("user.dir") + "/src/test/resources/TestData.xlsx";
        return ExcelReader.getSheetData(excelPath, "APITest");
    }

    @Test(dataProvider = "APIExcelDataProvider")
    public void validateAPIResponsePipelines(
            String testCaseId,
            String scenarioId,
            String description,
            String username,
            String password,
            String uiCategory,
            String uiTitle,
            String uiDescription,
            String expectedStatus) throws InterruptedException {

        log.info("Starting execution sequence for API Step Profile: " + testCaseId + " -> " + description);

        // Safe Numeric Parsing Guard: Handles non-numeric strings safely
        int expectedStatusCode = 200;
        boolean isNumericStatus = expectedStatus.matches("\\d+(\\.\\d+)?");
        if (isNumericStatus) {
            expectedStatusCode = (int) Double.parseDouble(expectedStatus);
        }

        // Initialize shared maps for post payloads
        Map<String, String> notePayload = new HashMap<>();
        if (uiCategory != null && !uiCategory.equals("-")) notePayload.put("category", uiCategory);
        if (uiTitle != null && !uiTitle.equals("-")) notePayload.put("title", uiTitle);
        if (uiDescription != null && !uiDescription.equals("-")) notePayload.put("description", uiDescription);

        // =================================================================
        // 🔀 DYNAMIC RESTASSURED ROUTING LAYER BASED ON TEST CASE ID
        // =================================================================


        if (testCaseId.equals("TC-API-01")) {
            // 🔐 TC-API-01: Validate POST /login returns token
            Map<String, String> loginPayload = new HashMap<>();
            loginPayload.put("email", username);
            loginPayload.put("password", password);

            Response response = RestAssured.given()
                    .spec(requestSpec)
                    .body(loginPayload)
                    .post("/users/login");

            Assert.assertEquals(response.getStatusCode(), expectedStatusCode, "Login failed.");
            Assert.assertNotNull(response.jsonPath().getString("data.token"), "Token generation token missing.");

        } else if (testCaseId.equals("TC-API-02")) {
            // 📝 TC-API-02: Validate POST /notes creates note
            Response response = RestAssured.given()
                    .spec(requestSpec)
                    .header("x-auth-token", authToken)
                    .body(notePayload)
                    .post("/notes");

            Assert.assertEquals(response.getStatusCode(), 200, "Note creation failed.");
            Assert.assertNotNull(response.jsonPath().getString("data.id"), "Note ID missing from server payload response.");

        } else if (testCaseId.equals("TC-API-03")) {
            // 📊 TC-API-03: Validate GET /notes returns list
            RestAssured.given()
                    .spec(requestSpec)
                    .header("x-auth-token", authToken)
                    .get("/notes")
                    .then()
                    .statusCode(expectedStatusCode)
                    .body("message", Matchers.containsString("Notes successfully retrieved"));

        } else if (testCaseId.equals("TC-API-04")) {
            // 🔄 TC-API-04: Validate UI note appears in API (Simulated via Direct API Injection)
            log.info("Executing TC-API-04: Injecting test note via background REST request to verify retrieval pipelines.");

            // Step 1: Create the note via API backend directly
            Response createResponse = RestAssured.given()
                    .spec(requestSpec)
                    .header("x-auth-token", authToken)
                    .body(notePayload)
                    .post("/notes");

            Assert.assertEquals(createResponse.getStatusCode(), 200, "API-driven pre-requisite note creation failed.");

            // Step 2: Fetch list to verify its downstream availability array registry
            Response response = RestAssured.given()
                    .spec(requestSpec)
                    .header("x-auth-token", authToken)
                    .get("/notes");

            String backendTitle = response.jsonPath().getString("data.find { it.title.trim() == '" + uiTitle.trim() + "' }.title");
            Assert.assertNotNull(backendTitle, "Cross-Layer validation failed: Note is missing from API database cluster registry.");
        } else if (testCaseId.equals("TC-API-05")) {
            // 🗑️ TC-API-05: Validate DELETE /notes removes note
            Response createResp = RestAssured.given()
                    .spec(requestSpec)
                    .header("x-auth-token", authToken)
                    .body(notePayload)
                    .post("/notes");

            String targetNoteId = createResp.jsonPath().getString("data.id");

            Response deleteResp = RestAssured.given()
                    .spec(requestSpec)
                    .header("x-auth-token", authToken)
                    .delete("/notes/" + targetNoteId);

            // Handle dual successful code boundaries for deletions (200 OK or 204 No Content)
            int actualDeleteCode = deleteResp.getStatusCode();
            Assert.assertTrue(actualDeleteCode == 200 || actualDeleteCode == 204,
                    "Deletion failed. Received code: " + actualDeleteCode);

        } else if (testCaseId.equals("TC-API-06")) {
            // ⚡ TC-API-06: Validate API response < 2 seconds (Performance SLA)
            Response perfResponse = RestAssured.given()
                    .spec(requestSpec)
                    .header("x-auth-token", authToken)
                    .get("/notes");

            long responseTimeMs = perfResponse.getTimeIn(TimeUnit.MILLISECONDS);
            log.info("Measured operational round-trip response performance time: " + responseTimeMs + " ms");

            Assert.assertTrue(responseTimeMs < 2000,
                    "Performance SLA Regression: Endpoint roundtrip exceeded maximum 2-second limit boundary! Actual: " + responseTimeMs + "ms");
        } else {
            Assert.fail("Automation Engine Alert: Unmapped execution path profile for API Target Case ID: " + testCaseId);
        }
    }
}