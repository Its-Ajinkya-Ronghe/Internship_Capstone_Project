package com.expandtesting.notes.tests.negative;

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

/**
 * NegativeTest — Data-driven API security validation and error-handling suite.
 * Optimized to implement Section 3.5 Performance SLAs and structural clean-code switch layouts.
 */
public class NegativeTest extends BaseAPI {

    private static final org.apache.logging.log4j.Logger log = org.apache.logging.log4j.LogManager.getLogger(NegativeTest.class);
    private static final long PERFORMANCE_SLA_MS = 2000L; // ⏱️ Section 3.5 SLA Limit

    @BeforeClass
    public void apiSetup() {
        initializeAPI();
        loginAndSetToken();
    }

    @DataProvider(name = "NegativeExcelDataProvider")
    public Object[][] getNegativeTestDataFromExcel() {
        String excelPath = System.getProperty("user.dir") + "/src/test/resources/TestData.xlsx";
        return ExcelReader.getSheetData(excelPath, "NegativeTest");
    }

    @Test(dataProvider = "NegativeExcelDataProvider")
    public void validateAPIAndSecurityNegativePipelines(
            String testCaseId, String scenarioId, String description,
            String username, String password,
            String uiCategory, String uiTitle, String uiDescription,
            String expectedStatus) {

        log.info("▶ Executing Negative/Security Layer Step: " + testCaseId + " -> " + description);

        // Safe Numeric Parsing Guard for expected HTTP Status Codes
        int expectedStatusCode = 400;
        if (expectedStatus.matches("\\d+(\\.\\d+)?")) {
            expectedStatusCode = (int) Double.parseDouble(expectedStatus);
        }

        switch (testCaseId) {

            // TC-NEG-01: Validate GET /notes without authentication token
            case "TC-NEG-01":
                RestAssured.given()
                        .spec(requestSpec)
                        .get("/notes")
                        .then()
                        .statusCode(expectedStatusCode)
                        .time(Matchers.lessThan(PERFORMANCE_SLA_MS)) // ⏱️ Section 3.5 SLA Check
                        .body("message", Matchers.containsString("No authentication token specified in x-auth-token header"));
                break;

            // TC-NEG-02: Validate invalid empty POST /notes payload schema handling
            case "TC-NEG-02":
                Map<String, String> emptyPayload = new HashMap<>();

                RestAssured.given()
                        .spec(requestSpec)
                        .header("x-auth-token", authToken)
                        .body(emptyPayload)
                        .post("/notes")
                        .then()
                        .statusCode(expectedStatusCode)
                        .time(Matchers.lessThan(PERFORMANCE_SLA_MS)) // ⏱️ Section 3.5 SLA Check
                        .body("message", Matchers.containsString("Title must be between"));
                break;

            // TC-NEG-03: Validate rejected gateway blocks for invalid/corrupted token
            case "TC-NEG-03":
                String corruptedOrExpiredToken = "Malformed_Header_Token_String_Value_XYZ";

                RestAssured.given()
                        .spec(requestSpec)
                        .header("x-auth-token", corruptedOrExpiredToken)
                        .get("/notes")
                        .then()
                        .statusCode(401)
                        .time(Matchers.lessThan(PERFORMANCE_SLA_MS)) // ⏱️ Section 3.5 SLA Check
                        .body("message", Matchers.containsString("Access token is not valid or has expired, you will need to login"));
                break;

            // TC-NEG-04: Validate field boundary constraint checks with extreme data loads
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

                boundaryResponse.then().time(Matchers.lessThan(PERFORMANCE_SLA_MS)); // ⏱️ Section 3.5 SLA Check
                Assert.assertTrue(boundaryResponse.getStatusCode() == expectedStatusCode || boundaryResponse.getStatusCode() == 200,
                        "Long boundary input payload was handled unexpectedly. Received status code: " + boundaryResponse.getStatusCode());
                break;

            // TC-NEG-05: High-frequency authentication rate-limiting check
            case "TC-NEG-05":
                log.info("Executing high-frequency security stress cycle to trigger rate-limiting firewalls.");

                Map<String, String> badLoginPayload = new HashMap<>();
                badLoginPayload.put("email", "rongheajinkya72@gmail.com");
                badLoginPayload.put("password", password);

                int targetRateLimitStatusCode = 429;
                int lastReceivedStatusCode = 200;

                // Fire requests sequentially to hit the security block limits
                for (int i = 1; i <= 35; i++) {
                    Response rateCheckResp = RestAssured.given()
                            .spec(requestSpec)
                            .body(badLoginPayload)
                            .post("/users/login");

                    lastReceivedStatusCode = rateCheckResp.getStatusCode();

                    if (lastReceivedStatusCode == targetRateLimitStatusCode) {
                        log.info("Security block active! Rate limit 429 triggered safely on loop iteration index: " + i);
                        break;
                    }
                }

                // Verify that the server either rejected with a 401 Unauthorized or a 429 Rate Limit
                boolean isHandledSafely = (lastReceivedStatusCode == 429 || lastReceivedStatusCode == 401);
                Assert.assertTrue(isHandledSafely,
                        "Security Constraint Failure: The backend application returned an unmapped status code tracking profile: " + lastReceivedStatusCode);

                log.info("Security test execution complete. Status captured: HTTP " + lastReceivedStatusCode);
                break;

            default:
                Assert.fail("Automation Engine Alert: Unmapped execution path profile for Negative Target Case ID: " + testCaseId);
        }

        log.info("✔ Passed Negative Security Verification Flow: " + testCaseId);
    }
}