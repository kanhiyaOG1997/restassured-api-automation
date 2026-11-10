package com.kanhaiya.api.base;

import com.kanhaiya.api.utils.ConfigReader;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
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

    @BeforeClass
    public void setUp() {
        // Print request + response details only when a validation fails (keeps logs clean)
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        requestSpec = new RequestSpecBuilder()
                .setBaseUri(ConfigReader.get("baseUrl"))
                .setContentType(ContentType.JSON)   // Content-Type: application/json
                .setAccept("application/json")      // Accept: application/json
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
