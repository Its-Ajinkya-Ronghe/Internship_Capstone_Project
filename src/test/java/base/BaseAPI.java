package base;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import org.hamcrest.Matchers;

public class BaseAPI {
    public static RequestSpecification requestSpec;
    protected static ResponseSpecification responseSpec;
    public static String authToken = "";

    public static void initializeAPI() {
        RestAssured.baseURI = "https://practice.expandtesting.com/notes/api";

        // 🛠️ NETWORK SESSIONS TIMEOUT BREAKER MATRIX
        // Prevents RestAssured from waiting indefinitely if connection packets drop
        RestAssuredConfig timeoutConfig = RestAssuredConfig.config()
                .httpClient(HttpClientConfig.httpClientConfig()
                        .setParam("http.connection.timeout", 10000)     // 10s Connection Timeout
                        .setParam("http.socket.timeout", 10000));       // 10s Data Socket Timeout

        requestSpec = new RequestSpecBuilder()
                .setContentType(ContentType.JSON)
                .addHeader("Accept", "application/json")
                .setConfig(timeoutConfig) // Attaching timeouts globally
                .build();

        responseSpec = new ResponseSpecBuilder()
                .expectResponseTime(Matchers.lessThan(2000L)) // FR-08: Performance assertion < 2s
                .build();
    }

    public static void loginAndSetToken() {
        if (authToken.isEmpty()) {
            initializeAPI();
            String loginPayload = "{\"email\":\"rongheajinkya72@gmail.com\",\"password\":\"ajinkya72\"}";

            authToken = RestAssured.given()
                    .spec(requestSpec)
                    .body(loginPayload)
                    .post("/users/login")
                    .then()
                    .statusCode(200)
                    .extract()
                    .path("data.token");
        }
    }
}