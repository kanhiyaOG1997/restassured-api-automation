package com.kanhaiya.api.tests;

import com.kanhaiya.api.base.BaseTest;
import com.kanhaiya.api.models.Booking;
import com.kanhaiya.api.models.BookingDates;
import org.testng.Assert;
import org.testng.annotations.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * End-to-end CRUD flow for bookings. Tests run in order (priority) and share bookingId
 * (request chaining): the id created in test 1 is used by tests 2 to 5.
 */
public class BookingTest extends BaseTest {

    private int bookingId;   // saved after create, reused by the next tests

    @Test(priority = 1, groups = "smoke", description = "Create a booking - POST /booking")
    public void createBooking() {
        Booking booking = new Booking("Kanhaiya", "Tester", 1500, true,
                new BookingDates("2026-12-01", "2026-12-05"), "Breakfast");

        bookingId = given()
                .spec(requestSpec)
                .body(booking)                          // POJO -> JSON (serialization)
        .when()
                .post("/booking")
        .then()
                .statusCode(200)
                .body("bookingid", notNullValue())
                .body("booking.firstname", equalTo("Kanhaiya"))
                .body("booking.totalprice", equalTo(1500))
                .body("booking.bookingdates.checkin", equalTo("2026-12-01"))
                .extract().path("bookingid");           // save id for next tests
    }

    @Test(priority = 2, dependsOnMethods = "createBooking",
            description = "Get the created booking - GET /booking/{id}")
    public void getBooking() {
        // JSON -> POJO (deserialization), then assert using normal Java getters
        Booking response = given()
                .spec(requestSpec)
                .pathParam("id", bookingId)             // replaces {id} in the URL
        .when()
                .get("/booking/{id}")
        .then()
                .statusCode(200)
                .extract().as(Booking.class);

        Assert.assertEquals(response.getFirstname(), "Kanhaiya");
        Assert.assertEquals(response.getLastname(), "Tester");
        Assert.assertTrue(response.isDepositpaid());
    }

    @Test(priority = 3, dependsOnMethods = "createBooking",
            description = "Fully update the booking - PUT /booking/{id} (needs token)")
    public void updateBooking() {
        Booking updated = new Booking("Kanhaiya", "Updated", 2000, false,
                new BookingDates("2026-12-10", "2026-12-15"), "Lunch");

        given()
                .spec(requestSpec)
                .header("Cookie", "token=" + token)     // auth token from BaseTest
                .pathParam("id", bookingId)
                .body(updated)
        .when()
                .put("/booking/{id}")
        .then()
                .statusCode(200)
                .body("lastname", equalTo("Updated"))
                .body("totalprice", equalTo(2000))
                .body("additionalneeds", equalTo("Lunch"));
    }

    @Test(priority = 4, dependsOnMethods = "createBooking",
            description = "Delete the booking - DELETE /booking/{id} (needs token)")
    public void deleteBooking() {
        given()
                .spec(requestSpec)
                .header("Cookie", "token=" + token)
                .pathParam("id", bookingId)
        .when()
                .delete("/booking/{id}")
        .then()
                .statusCode(201);                       // this API returns 201 Created on delete
    }

    @Test(priority = 5, dependsOnMethods = "deleteBooking",
            description = "Deleted booking should not be found - GET returns 404")
    public void verifyBookingDeleted() {
        given()
                .spec(requestSpec)
                .pathParam("id", bookingId)
        .when()
                .get("/booking/{id}")
        .then()
                .statusCode(404);
    }

    @Test(priority = 6, description = "Update without token should be forbidden - 403")
    public void updateWithoutTokenIsForbidden() {
        Booking booking = new Booking("No", "Token", 100, true,
                new BookingDates("2026-12-01", "2026-12-02"), "None");

        given()
                .spec(requestSpec)
                .pathParam("id", 1)
                .body(booking)                          // no Cookie header on purpose
        .when()
                .put("/booking/{id}")
        .then()
                .statusCode(403);
    }
}
