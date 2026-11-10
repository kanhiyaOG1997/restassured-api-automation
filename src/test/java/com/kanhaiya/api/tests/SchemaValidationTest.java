package com.kanhaiya.api.tests;

import com.kanhaiya.api.base.BaseTest;
import com.kanhaiya.api.utils.ConfigReader;
import com.kanhaiya.api.utils.TestDataBuilder;
import org.testng.annotations.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;

/**
 * JSON schema validation: checks the STRUCTURE of each response
 * (required fields, data types, date format, no unexpected fields), not just values.
 * Schema files live in src/test/resources/schemas/.
 */
public class SchemaValidationTest extends BaseTest {

    // Helper: creates a booking and returns its id
    private int createBooking() {
        return given()
                .spec(requestSpec)
                .body(TestDataBuilder.validBooking())
        .when()
                .post("/booking")
        .then()
                .statusCode(200)
                .extract().path("bookingid");
    }

    @Test(groups = "regression", description = "POST /auth (valid) matches auth-token schema")
    public void authTokenSchema() {
        given()
                .spec(requestSpec)
                .body(Map.of(
                        "username", ConfigReader.get("username"),
                        "password", ConfigReader.get("password")))
        .when()
                .post("/auth")
        .then()
                .statusCode(200)
                // Compare the whole response body against the schema file on the classpath
                .body(matchesJsonSchemaInClasspath("schemas/auth-token.json"));
    }

    @Test(groups = "regression", description = "POST /auth (invalid) matches auth-error schema")
    public void authErrorSchema() {
        given()
                .spec(requestSpec)
                .body(Map.of("username", "schemaCheck", "password", "schemaCheck"))
        .when()
                .post("/auth")
        .then()
                .body(matchesJsonSchemaInClasspath("schemas/auth-error.json"));
    }

    @Test(groups = "regression", description = "POST /booking matches create-booking-response schema")
    public void createBookingSchema() {
        given()
                .spec(requestSpec)
                .body(TestDataBuilder.validBooking())
        .when()
                .post("/booking")
        .then()
                .statusCode(200)
                .body(matchesJsonSchemaInClasspath("schemas/create-booking-response.json"));
    }

    @Test(groups = "regression", description = "GET /booking/{id} matches booking schema")
    public void getBookingSchema() {
        int id = createBooking();                   // our own booking, so the data is known to be clean

        given()
                .spec(requestSpec)
                .pathParam("id", id)
        .when()
                .get("/booking/{id}")
        .then()
                .statusCode(200)
                .body(matchesJsonSchemaInClasspath("schemas/booking.json"));
    }

    @Test(groups = "regression", description = "GET /booking matches booking-ids schema")
    public void bookingIdsSchema() {
        given()
                .spec(requestSpec)
        .when()
                .get("/booking")
        .then()
                .statusCode(200)
                .body(matchesJsonSchemaInClasspath("schemas/booking-ids.json"));
    }

    @Test(groups = "regression", description = "PUT /booking/{id} response matches booking schema")
    public void updateBookingSchema() {
        int id = createBooking();

        given()
                .spec(requestSpec)
                .header("Cookie", "token=" + token)
                .pathParam("id", id)
                .body(TestDataBuilder.validBooking())
        .when()
                .put("/booking/{id}")
        .then()
                .statusCode(200)
                .body(matchesJsonSchemaInClasspath("schemas/booking.json"));
    }

    @Test(groups = "regression", description = "PATCH /booking/{id} response matches booking schema")
    public void patchBookingSchema() {
        int id = createBooking();

        given()
                .spec(requestSpec)
                .header("Cookie", "token=" + token)
                .pathParam("id", id)
                .body(Map.of("additionalneeds", "Schema check"))
        .when()
                .patch("/booking/{id}")
        .then()
                .statusCode(200)
                .body(matchesJsonSchemaInClasspath("schemas/booking.json"));
    }
}
