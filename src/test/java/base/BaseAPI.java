package base;

import com.expandtesting.notes.utils.ConfigReader;
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
        RestAssured.baseURI = ConfigReader.getProperty("api.base.url");

        long slaMs = Long.parseLong(ConfigReader.getProperty("api.max.response.time.ms"));

        RestAssuredConfig timeoutConfig = RestAssuredConfig.config()
                .httpClient(HttpClientConfig.httpClientConfig()
                        .setParam("http.connection.timeout", 10000)
                        .setParam("http.socket.timeout", 10000));

        requestSpec = new RequestSpecBuilder()
                .setContentType(ContentType.JSON)
                .addHeader("Accept", "application/json")
                .setConfig(timeoutConfig)
                .build();

        responseSpec = new ResponseSpecBuilder()
                .expectResponseTime(Matchers.lessThan(slaMs)) // FR-08: driven by config
                .build();
    }

    public static void loginAndSetToken() {
        if (authToken.isEmpty()) {
            initializeAPI();

            String email    = ConfigReader.getProperty("default.username");
            String password = ConfigReader.getProperty("default.password");

            String loginPayload = String.format(
                    "{\"email\":\"%s\",\"password\":\"%s\"}", email, password
            );

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