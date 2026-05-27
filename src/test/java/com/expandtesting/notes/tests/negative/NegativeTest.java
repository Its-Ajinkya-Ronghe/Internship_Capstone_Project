package com.expandtesting.notes.tests.negative;

import base.BaseAPI;
import com.expandtesting.notes.utils.ConfigReader;
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

public class NegativeTest extends BaseAPI {

    private static final org.apache.logging.log4j.Logger log =
            org.apache.logging.log4j.LogManager.getLogger(NegativeTest.class);
    private static final long PERFORMANCE_SLA_MS =
            Long.parseLong(ConfigReader.getProperty("api.max.response.time.ms")); // ← from config

    @BeforeClass
    public void apiSetup() {
        initializeAPI();
        loginAndSetToken();
    }

    @DataProvider(name = "NegativeExcelDataProvider")
    public Object[][] getNegativeTestDataFromExcel() {
        String excelPath = ConfigReader.getProperty("excel.testdata.path")
                .replace("./", System.getProperty("user.dir") + "/");
        return ExcelReader.getSheetData(excelPath, "NegativeTest");
    }

    @Test(dataProvider = "NegativeExcelDataProvider")
    public void validateAPIAndSecurityNegativePipelines(
            String testCaseId, String scenarioId, String description,
            String username, String password,
            String uiCategory, String uiTitle, String uiDescription,
            String expectedStatus) {

        log.info("▶ Executing Negative/Security Layer Step: " + testCaseId + " -> " + description);

        int expectedStatusCode = 400;
        if (expectedStatus.matches("\\d+(\\.\\d+)?")) {
            expectedStatusCode = (int) Double.parseDouble(expectedStatus);
        }

        switch (testCaseId) {

            case "TC-NEG-01":
                RestAssured.given()
                        .spec(requestSpec)
                        .get("/notes")
                        .then()
                        .statusCode(expectedStatusCode)
                        .time(Matchers.lessThan(PERFORMANCE_SLA_MS))
                        .body("message", Matchers.containsString(
                                "No authentication token specified in x-auth-token header"));
                break;

            case "TC-NEG-02":
                Map<String, String> emptyPayload = new HashMap<>();
                RestAssured.given()
                        .spec(requestSpec)
                        .header("x-auth-token", authToken)
                        .body(emptyPayload)
                        .post("/notes")
                        .then()
                        .statusCode(expectedStatusCode)
                        .time(Matchers.lessThan(PERFORMANCE_SLA_MS))
                        .body("message", Matchers.containsString("Title must be between"));
                break;

            case "TC-NEG-03":
                String corruptedOrExpiredToken = "Malformed_Header_Token_String_Value_XYZ";
                RestAssured.given()
                        .spec(requestSpec)
                        .header("x-auth-token", corruptedOrExpiredToken)
                        .get("/notes")
                        .then()
                        .statusCode(401)
                        .time(Matchers.lessThan(PERFORMANCE_SLA_MS))
                        .body("message", Matchers.containsString(
                                "Access token is not valid or has expired, you will need to login"));
                break;

            case "TC-NEG-04":
                Map<String, String> longPayload = new HashMap<>();
                longPayload.put("category", uiCategory);
                longPayload.put("title", uiTitle);
                longPayload.put("description", uiDescription);

                Response boundaryResponse = RestAssured.given()
                        .spec(requestSpec)
                        .header("x-auth-token", authToken)
                        .body(longPayload)
                        .post("/notes");

                boundaryResponse.then().time(Matchers.lessThan(PERFORMANCE_SLA_MS));
                Assert.assertTrue(
                        boundaryResponse.getStatusCode() == expectedStatusCode
                                || boundaryResponse.getStatusCode() == 200,
                        "Unexpected status: " + boundaryResponse.getStatusCode());
                break;

            case "TC-NEG-05":
                log.info("Executing high-frequency security stress cycle.");

                Map<String, String> badLoginPayload = new HashMap<>();
                badLoginPayload.put("email", ConfigReader.getProperty("default.username")); // ← from config
                badLoginPayload.put("password", password);

                int targetRateLimitStatusCode = 429;
                int lastReceivedStatusCode = 200;

                for (int i = 1; i <= 35; i++) {
                    Response rateCheckResp = RestAssured.given()
                            .spec(requestSpec)
                            .body(badLoginPayload)
                            .post("/users/login");

                    lastReceivedStatusCode = rateCheckResp.getStatusCode();

                    if (lastReceivedStatusCode == targetRateLimitStatusCode) {
                        log.info("Rate limit 429 triggered on iteration: " + i);
                        break;
                    }
                }

                boolean isHandledSafely = (lastReceivedStatusCode == 429 || lastReceivedStatusCode == 401);
                Assert.assertTrue(isHandledSafely,
                        "Unexpected status code: " + lastReceivedStatusCode);
                log.info("Security test complete. Status: HTTP " + lastReceivedStatusCode);
                break;

            default:
                Assert.fail("Unmapped test case ID: " + testCaseId);
        }

        log.info("✔ Passed: " + testCaseId);
    }
}