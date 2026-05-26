package com.expandtesting.notes.tests.api;

import base.BaseAPI;
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

/**
 * APITest — Data-driven API verification suite.
 * Optimized to satisfy Section 3.5 Performance SLAs and standardized tool mappings.
 */
public class APITest extends BaseAPI {

    private static final org.apache.logging.log4j.Logger log = org.apache.logging.log4j.LogManager.getLogger(APITest.class);
    private static final long PERFORMANCE_SLA_MS = 2000L; // ⏱️ Section 3.5 SLA Boundary Constraint

    public void deleteAllNotesViaAPI() {
        log.info("Initiating global API purge sequence to delete all active notes.");

        Response response = RestAssured.given()
                .spec(requestSpec)
                .header("x-auth-token", authToken)
                .get("/notes");

        if (response.getStatusCode() == 200) {
            java.util.List<String> noteIds = response.jsonPath().getList("data.id");
            log.info("Found " + noteIds.size() + " active note(s) slated for erasure.");

            for (String id : noteIds) {
                RestAssured.given()
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
            String testCaseId, String scenarioId, String description,
            String username, String password,
            String uiCategory, String uiTitle, String uiDescription,
            String expectedStatus) throws InterruptedException {

        log.info("▶ Executing API Layer Step: " + testCaseId + " -> " + description);

        // Safe Numeric Parsing Guard for expected HTTP Status Codes
        int expectedStatusCode = 200;
        if (expectedStatus.matches("\\d+(\\.\\d+)?")) {
            expectedStatusCode = (int) Double.parseDouble(expectedStatus);
        }

        // Initialize shared mapping payloads conforming to uniform schema layouts
        Map<String, String> notePayload = new HashMap<>();
        if (uiCategory != null && !uiCategory.equals("-")) notePayload.put("category", uiCategory);
        if (uiTitle != null && !uiTitle.equals("-")) notePayload.put("title", uiTitle);
        if (uiDescription != null && !uiDescription.equals("-")) notePayload.put("description", uiDescription);

        switch (testCaseId) {

            // TC-API-01: Validate POST /login yields authentication tokens
            case "TC-API-01":
                Map<String, String> loginPayload = new HashMap<>();
                loginPayload.put("email", username);
                loginPayload.put("password", password);

                Response loginResponse = RestAssured.given()
                        .spec(requestSpec)
                        .body(loginPayload)
                        .post("/users/login");

                // Check assertions and enforce execution timers under 2 seconds
                loginResponse.then().time(Matchers.lessThan(PERFORMANCE_SLA_MS));
                Assert.assertEquals(loginResponse.getStatusCode(), expectedStatusCode, "Login failed.");
                Assert.assertNotNull(loginResponse.jsonPath().getString("data.token"), "Authentication token missing.");
                break;

            // TC-API-02: Validate POST /notes generates entry records
            case "TC-API-02":
                Response createResponse = RestAssured.given()
                        .spec(requestSpec)
                        .header("x-auth-token", authToken)
                        .body(notePayload)
                        .post("/notes");

                createResponse.then().time(Matchers.lessThan(PERFORMANCE_SLA_MS));
                Assert.assertEquals(createResponse.getStatusCode(), 200, "Note creation failed.");
                Assert.assertNotNull(createResponse.jsonPath().getString("data.id"), "Note ID missing from response context.");
                break;

            // TC-API-03: Validate GET /notes returns collection matrices
            case "TC-API-03":
                RestAssured.given()
                        .spec(requestSpec)
                        .header("x-auth-token", authToken)
                        .get("/notes")
                        .then()
                        .statusCode(expectedStatusCode)
                        .time(Matchers.lessThan(PERFORMANCE_SLA_MS))
                        .body("message", Matchers.containsString("Notes successfully retrieved"));
                break;

            // TC-API-04: Validate downstream availability via background injection flows
            case "TC-API-04":
                log.info("Executing TC-API-04: Injecting test note via background REST request to verify retrieval pipelines.");

                Response directCreateResp = RestAssured.given()
                        .spec(requestSpec)
                        .header("x-auth-token", authToken)
                        .body(notePayload)
                        .post("/notes");
                Assert.assertEquals(directCreateResp.getStatusCode(), 200, "API-driven pre-requisite note creation failed.");

                Response listResponse = RestAssured.given()
                        .spec(requestSpec)
                        .header("x-auth-token", authToken)
                        .get("/notes");

                listResponse.then().time(Matchers.lessThan(PERFORMANCE_SLA_MS));
                String backendTitle = listResponse.jsonPath().getString("data.find { it.title.trim() == '" + uiTitle.trim() + "' }.title");
                Assert.assertNotNull(backendTitle, "Cross-Layer validation failed: Note is missing from API database cluster registry.");
                break;

            // TC-API-05: Validate DELETE /notes discards active elements smoothly
            case "TC-API-05":
                Response prepNoteResp = RestAssured.given()
                        .spec(requestSpec)
                        .header("x-auth-token", authToken)
                        .body(notePayload)
                        .post("/notes");
                String targetNoteId = prepNoteResp.jsonPath().getString("data.id");

                Response deleteResp = RestAssured.given()
                        .spec(requestSpec)
                        .header("x-auth-token", authToken)
                        .delete("/notes/" + targetNoteId);

                deleteResp.then().time(Matchers.lessThan(PERFORMANCE_SLA_MS));
                int actualDeleteCode = deleteResp.getStatusCode();
                Assert.assertTrue(actualDeleteCode == 200 || actualDeleteCode == 204, "Deletion failed. Code received: " + actualDeleteCode);
                break;

            // TC-API-06: Explicit Performance SLA Validation Checkpoint Matrix
            case "TC-API-06":
                Response perfResponse = RestAssured.given()
                        .spec(requestSpec)
                        .header("x-auth-token", authToken)
                        .get("/notes");

                long responseTimeMs = perfResponse.getTimeIn(TimeUnit.MILLISECONDS);
                log.info("Measured operational round-trip response performance time: " + responseTimeMs + " ms");

                Assert.assertTrue(responseTimeMs < PERFORMANCE_SLA_MS,
                        "Performance SLA Regression: Endpoint roundtrip exceeded maximum limit! Actual: " + responseTimeMs + "ms");
                break;

            default:
                Assert.fail("Automation Engine Alert: Unmapped execution path profile for API Target Case ID: " + testCaseId);
        }

        log.info("✔ Passed: " + testCaseId);
    }
}