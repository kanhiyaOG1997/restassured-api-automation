package com.kanhaiya.api.tests;

import com.kanhaiya.api.base.BaseTest;
import org.testng.annotations.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Smoke tests: quick checks that the API is up and the main endpoints answer.
 * Run these first - if they fail, there is no point running the rest.
 */
public class SmokeTest extends BaseTest {

    @Test(groups = "smoke", description = "Health check - GET /ping")
    public void healthCheck() {
        given()
                .spec(requestSpec)
        .when()
                .get("/ping")
        .then()
                // Known API behaviour: health check returns 201 Created instead of 200 OK
                .statusCode(201);
    }

    @Test(groups = "smoke", description = "GET /booking returns a non-empty list of ids")
    public void getAllBookingIds() {
        given()
                .spec(requestSpec)
        .when()
                .get("/booking")
        .then()
                .statusCode(200)
                .body("size()", greaterThan(0))                      // at least one booking exists
                .body("bookingid", everyItem(notNullValue()));
    }

    @Test(groups = "smoke", description = "GET /booking/{id} for an existing id returns booking details")
    public void getExistingBooking() {
        // Pick the first id from the list instead of hard-coding one
        int firstId = given()
                .spec(requestSpec)
        .when()
                .get("/booking")
        .then()
                .statusCode(200)
                .extract().path("[0].bookingid");                    // first element of the array

        given()
                .spec(requestSpec)
                .pathParam("id", firstId)
        .when()
                .get("/booking/{id}")
        .then()
                .statusCode(200)
                .body("firstname", notNullValue());
    }
}
