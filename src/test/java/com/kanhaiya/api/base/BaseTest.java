package com.kanhaiya.api.base;

import com.kanhaiya.api.utils.ConfigReader;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.testng.annotations.BeforeClass;

import java.util.Map;

import static io.restassured.RestAssured.given;

/**
 * Parent class for all tests. Common setup lives here so every test class can reuse it.
 */
public class BaseTest {

    // Reusable request settings (base URL, JSON headers, logging) shared by all tests
    protected RequestSpecification requestSpec;
    protected String token;

    // Max time (ms) to wait for the server to connect or reply before the request fails
    private static final int TIMEOUT_MS = 15_000;

    // alwaysRun = true so setup still runs when tests are filtered by group
    @BeforeClass(alwaysRun = true)
    public void setUp() {
        // Print request + response details only when a validation fails (keeps logs clean)
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        // Start from the global config (keeps the logging setting above) and add timeouts
        RestAssuredConfig timeoutConfig = RestAssured.config()
                .httpClient(HttpClientConfig.httpClientConfig()
                        .setParam("http.connection.timeout", TIMEOUT_MS)   // time to connect
                        .setParam("http.socket.timeout", TIMEOUT_MS));     // time to wait for the reply

        requestSpec = new RequestSpecBuilder()
                .setBaseUri(ConfigReader.get("baseUrl"))
                .setConfig(timeoutConfig)           // every request using this spec gets the timeouts
                .setContentType(ContentType.JSON)   // Content-Type: application/json
                .setAccept("application/json")      // exact value; ContentType.JSON sends a list and POST /booking returns 418
                .build();

        token = generateToken();
    }

    // Calls POST /auth and returns the token needed for PUT and DELETE requests
    protected String generateToken() {
        Map<String, String> credentials = Map.of(
                "username", ConfigReader.get("username"),
                "password", ConfigReader.get("password"));

        return given()
                .spec(requestSpec)
                .body(credentials)                  // Map is converted to JSON automatically
        .when()
                .post("/auth")
        .then()
                .statusCode(200)
                .extract().path("token");           // read "token" field from response JSON
    }
}
