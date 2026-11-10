package com.kanhaiya.api.tests;

import com.kanhaiya.api.base.BaseTest;
import com.kanhaiya.api.utils.ConfigReader;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.annotations.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Tests for the authentication endpoint: POST /auth
 */
// Allure labels: group this class in the report and set its default importance
@Epic("Restful Booker API")
@Feature("Authentication")
@Severity(SeverityLevel.CRITICAL)
public class AuthTest extends BaseTest {

    @Severity(SeverityLevel.BLOCKER)   // smoke test: nothing works without login
    @Test(groups = "smoke", description = "Valid credentials should return a token")
    public void validLoginReturnsToken() {
        given()
                .spec(requestSpec)
                .body(Map.of(
                        "username", ConfigReader.get("username"),       // credentials from config
                        "password", ConfigReader.get("password")))
        .when()
                .post("/auth")
        .then()
                .statusCode(200)
                .body("token", notNullValue())          // token must be present
                .body("token", not(emptyString()));     // and not empty
    }

    @Test(description = "Wrong password should return 'Bad credentials'")
    public void invalidLoginReturnsBadCredentials() {
        given()
                .spec(requestSpec)
                .body(Map.of(
                        "username", ConfigReader.get("username"),
                        "password", "wrongPassword"))                  // deliberately wrong
        .when()
                .post("/auth")
        .then()
                .statusCode(200)                        // this API returns 200 even for wrong login
                .body("reason", equalTo("Bad credentials"))
                .body("token", nullValue());            // no token should be given
    }
}
