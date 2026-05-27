package com.expandtesting.notes.tests.api;

import base.BaseAPI;
import com.expandtesting.notes.utils.ConfigReader;
import com.expandtesting.notes.utils.ExcelReader;
import io.restassured.RestAssured;
import io.restassured.module.jsv.JsonSchemaValidator;
import io.restassured.response.Response;
import org.hamcrest.Matchers;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * APITest — Data-driven API verification suite.
 * Includes JSON Schema Validation (Section 2.3) and Performance SLAs (Section 3.5).
 */
public class APITest extends BaseAPI {

    private static final org.apache.logging.log4j.Logger log =
            org.apache.logging.log4j.LogManager.getLogger(APITest.class);

    private static final long PERFORMANCE_SLA_MS =
            Long.parseLong(ConfigReader.getProperty("api.max.response.time.ms"));

    // Schema file paths
    private static final String LOGIN_SCHEMA  =
            "src/test/resources/schemas/login_response_schema.json";
    private static final String NOTES_SCHEMA  =
            "src/test/resources/schemas/notes_list_schema.json";

    public void deleteAllNotesViaAPI() {
        log.info("Initiating global API purge sequence.");

        Response response = RestAssured.given()
                .spec(requestSpec)
                .header("x-auth-token", authToken)
                .get("/notes");

        if (response.getStatusCode() == 200) {
            java.util.List<String> noteIds = response.jsonPath().getList("data.id");
            log.info("Found " + noteIds.size() + " active note(s) for erasure.");

            for (String id : noteIds) {
                RestAssured.given()
                        .spec(requestSpec)
                        .header("x-auth-token", authToken)
                        .delete("/notes/" + id)
                        .then()
                        .statusCode(200);
            }
            log.info("API purge complete.");
        } else {
            log.error("Failed to fetch notes for cleanup. Status: " + response.getStatusCode());
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
        String excelPath = ConfigReader.getProperty("excel.testdata.path")
                .replace("./", System.getProperty("user.dir") + "/");
        return ExcelReader.getSheetData(excelPath, "APITest");
    }

    @Test(dataProvider = "APIExcelDataProvider")
    public void validateAPIResponsePipelines(
            String testCaseId, String scenarioId, String description,
            String username, String password,
            String uiCategory, String uiTitle, String uiDescription,
            String expectedStatus) throws InterruptedException {

        log.info("▶ Executing API Layer Step: " + testCaseId + " -> " + description);

        int expectedStatusCode = 200;
        if (expectedStatus.matches("\\d+(\\.\\d+)?")) {
            expectedStatusCode = (int) Double.parseDouble(expectedStatus);
        }

        Map<String, String> notePayload = new HashMap<>();
        if (uiCategory != null && !uiCategory.equals("-")) notePayload.put("category", uiCategory);
        if (uiTitle    != null && !uiTitle.equals("-"))    notePayload.put("title", uiTitle);
        if (uiDescription != null && !uiDescription.equals("-")) notePayload.put("description", uiDescription);

        switch (testCaseId) {

            // TC-API-01: POST /login — with JSON schema validation
            case "TC-API-01":
                Map<String, String> loginPayload = new HashMap<>();
                loginPayload.put("email", username);
                loginPayload.put("password", password);

                Response loginResponse = RestAssured.given()
                        .spec(requestSpec)
                        .body(loginPayload)
                        .post("/users/login");

                loginResponse.then()
                        .time(Matchers.lessThan(PERFORMANCE_SLA_MS))
                        .statusCode(expectedStatusCode)
                        // ✅ JSON Schema Validation
                        .body(JsonSchemaValidator.matchesJsonSchema(new File(LOGIN_SCHEMA)));

                Assert.assertNotNull(
                        loginResponse.jsonPath().getString("data.token"),
                        "Authentication token missing.");
                break;

            // TC-API-02: POST /notes
            case "TC-API-02":
                Response createResponse = RestAssured.given()
                        .spec(requestSpec)
                        .header("x-auth-token", authToken)
                        .body(notePayload)
                        .post("/notes");

                createResponse.then().time(Matchers.lessThan(PERFORMANCE_SLA_MS));
                Assert.assertEquals(createResponse.getStatusCode(), 200, "Note creation failed.");
                Assert.assertNotNull(
                        createResponse.jsonPath().getString("data.id"),
                        "Note ID missing.");
                break;

            // TC-API-03: GET /notes — with JSON schema validation
            case "TC-API-03":
                RestAssured.given()
                        .spec(requestSpec)
                        .header("x-auth-token", authToken)
                        .get("/notes")
                        .then()
                        .statusCode(expectedStatusCode)
                        .time(Matchers.lessThan(PERFORMANCE_SLA_MS))
                        .body("message", Matchers.containsString("Notes successfully retrieved"))
                        // ✅ JSON Schema Validation
                        .body(JsonSchemaValidator.matchesJsonSchema(new File(NOTES_SCHEMA)));
                break;

            // TC-API-04: Inject note and verify via GET
            case "TC-API-04":
                log.info("TC-API-04: Injecting test note via background REST.");

                Response directCreateResp = RestAssured.given()
                        .spec(requestSpec)
                        .header("x-auth-token", authToken)
                        .body(notePayload)
                        .post("/notes");
                Assert.assertEquals(directCreateResp.getStatusCode(), 200,
                        "Pre-requisite note creation failed.");

                Response listResponse = RestAssured.given()
                        .spec(requestSpec)
                        .header("x-auth-token", authToken)
                        .get("/notes");

                listResponse.then().time(Matchers.lessThan(PERFORMANCE_SLA_MS));
                String backendTitle = listResponse.jsonPath()
                        .getString("data.find { it.title.trim() == '" + uiTitle.trim() + "' }.title");
                Assert.assertNotNull(backendTitle, "Note missing from API response.");
                break;

            // TC-API-05: DELETE /notes
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
                Assert.assertTrue(
                        actualDeleteCode == 200 || actualDeleteCode == 204,
                        "Deletion failed. Code: " + actualDeleteCode);
                break;

            // TC-API-06: Performance SLA checkpoint
            case "TC-API-06":
                Response perfResponse = RestAssured.given()
                        .spec(requestSpec)
                        .header("x-auth-token", authToken)
                        .get("/notes");

                long responseTimeMs = perfResponse.getTimeIn(TimeUnit.MILLISECONDS);
                log.info("Response time: " + responseTimeMs + " ms");
                Assert.assertTrue(responseTimeMs < PERFORMANCE_SLA_MS,
                        "SLA breach! Actual: " + responseTimeMs + "ms");
                break;

            default:
                Assert.fail("Unmapped test case ID: " + testCaseId);
        }

        log.info("✔ Passed: " + testCaseId);
    }
}