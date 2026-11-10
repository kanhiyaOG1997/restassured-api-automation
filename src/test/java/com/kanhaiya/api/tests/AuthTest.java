package com.kanhaiya.api.tests;

import com.kanhaiya.api.base.BaseTest;
import org.testng.annotations.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Tests for the authentication endpoint: POST /auth
 */
public class AuthTest extends BaseTest {

    @Test(description = "Valid credentials should return a token")
    public void validLoginReturnsToken() {
        given()
                .spec(requestSpec)
                .body(Map.of("username", "admin", "password", "password123"))
        .when()
                .post("/auth")
        .then()
                .statusCode(200)
                .body("token", notNullValue())          // token must be present
                .body("token", not(emptyString()))      // and not empty
                .time(lessThan(10000L));                // response within 10 seconds
    }

    @Test(description = "Wrong password should return 'Bad credentials'")
    public void invalidLoginReturnsBadCredentials() {
        given()
                .spec(requestSpec)
                .body(Map.of("username", "admin", "password", "wrongPassword"))
        .when()
                .post("/auth")
        .then()
                .statusCode(200)                        // this API returns 200 even for wrong login
                .body("reason", equalTo("Bad credentials"))
                .body("token", nullValue());            // no token should be given
    }
}
