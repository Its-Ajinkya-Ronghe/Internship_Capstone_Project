package base;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import org.hamcrest.Matchers;

public class BaseAPI {
    public static RequestSpecification requestSpec;
    protected static ResponseSpecification responseSpec;
    public static String authToken = "";

    public static void initializeAPI() {
        // Points directly to the swagger base documentation path
        RestAssured.baseURI = "https://practice.expandtesting.com/notes/api";

        requestSpec = new RequestSpecBuilder()
                .setContentType(ContentType.JSON)
                .addHeader("Accept", "application/json")
                .build();

        responseSpec = new ResponseSpecBuilder()
                .expectResponseTime(Matchers.lessThan(2000L)) // FR-08: Performance assertion < 2s
                .build();
    }

    // Static helper to quickly obtain token for subsequent requests
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