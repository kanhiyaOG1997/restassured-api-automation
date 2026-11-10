package com.kanhaiya.api.tests;

import com.kanhaiya.api.base.BaseTest;
import com.kanhaiya.api.models.Booking;
import com.kanhaiya.api.utils.TestDataBuilder;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Sanity tests: each main feature works correctly, one feature per test.
 * Every test creates its own data, so they can run in any order.
 */
// Allure labels: group this class in the report and set its default importance
@Epic("Restful Booker API")
@Feature("Sanity")
@Severity(SeverityLevel.CRITICAL)
public class SanityTest extends BaseTest {

    // Helper: creates a booking and returns its id
    private int createBooking(Booking booking) {
        return given()
                .spec(requestSpec)
                .body(booking)
        .when()
                .post("/booking")
        .then()
                .statusCode(200)
                .extract().path("bookingid");
    }

    // Helper: reads a booking back as a POJO
    private Booking getBooking(int id) {
        return given()
                .spec(requestSpec)
                .pathParam("id", id)
        .when()
                .get("/booking/{id}")
        .then()
                .statusCode(200)
                .extract().as(Booking.class);
    }

    // Helper: compares every field of two bookings, including the nested dates
    private void assertSameBooking(Booking actual, Booking expected) {
        Assert.assertEquals(actual.getFirstname(), expected.getFirstname(), "firstname");
        Assert.assertEquals(actual.getLastname(), expected.getLastname(), "lastname");
        Assert.assertEquals(actual.getTotalprice(), expected.getTotalprice(), "totalprice");
        Assert.assertEquals(actual.isDepositpaid(), expected.isDepositpaid(), "depositpaid");
        Assert.assertEquals(actual.getBookingdates().getCheckin(), expected.getBookingdates().getCheckin(), "checkin");
        Assert.assertEquals(actual.getBookingdates().getCheckout(), expected.getBookingdates().getCheckout(), "checkout");
        Assert.assertEquals(actual.getAdditionalneeds(), expected.getAdditionalneeds(), "additionalneeds");
    }

    @Test(groups = "sanity", description = "Created booking is saved with all fields exactly as sent")
    public void createdBookingMatchesAllFields() {
        Booking sent = TestDataBuilder.validBooking();
        int id = createBooking(sent);

        assertSameBooking(getBooking(id), sent);                     // read back and compare all 7 values
    }

    @Test(groups = "sanity", description = "PATCH changes only the given field, others stay the same")
    public void partialUpdateChangesOnlyGivenField() {
        Booking original = TestDataBuilder.validBooking();
        int id = createBooking(original);

        given()
                .spec(requestSpec)
                .header("Cookie", "token=" + token)
                .pathParam("id", id)
                .body(Map.of("firstname", "Patched"))                // send only one field
        .when()
                .patch("/booking/{id}")
        .then()
                .statusCode(200)
                .body("firstname", equalTo("Patched"));

        // Everything except firstname must be unchanged
        original.setFirstname("Patched");
        assertSameBooking(getBooking(id), original);
    }

    @Test(groups = "sanity", description = "PUT update is really saved (verified with GET)")
    public void fullUpdateIsPersisted() {
        int id = createBooking(TestDataBuilder.validBooking());
        Booking updated = TestDataBuilder.validBooking();            // completely new values

        given()
                .spec(requestSpec)
                .header("Cookie", "token=" + token)
                .pathParam("id", id)
                .body(updated)
        .when()
                .put("/booking/{id}")
        .then()
                .statusCode(200);

        assertSameBooking(getBooking(id), updated);                  // GET proves it was stored
    }

    @Test(groups = "sanity", description = "Filter by firstname + lastname finds the created booking")
    public void filterByNameFindsBooking() {
        Booking booking = TestDataBuilder.validBooking();
        booking.setLastname("Sanity" + System.nanoTime());          // unique, so only our booking matches
        int id = createBooking(booking);

        given()
                .spec(requestSpec)
                .queryParam("firstname", booking.getFirstname())
                .queryParam("lastname", booking.getLastname())
        .when()
                .get("/booking")
        .then()
                .statusCode(200)
                .body("bookingid", hasItem(id));                     // our id is in the result list
    }

    @Test(groups = "sanity", description = "Filter by checkout date returns a list")
    public void filterByCheckoutDate() {
        Booking booking = TestDataBuilder.validBooking();
        createBooking(booking);

        given()
                .spec(requestSpec)
                .queryParam("checkout", booking.getBookingdates().getCheckout())
        .when()
                .get("/booking")
        .then()
                .statusCode(200)
                .body("$", instanceOf(java.util.List.class))         // response root is a JSON array
                .body("bookingid", everyItem(notNullValue()));
    }

    @Test(groups = "sanity", description = "GET a booking id that does not exist returns 404")
    public void getNonExistentBooking() {
        given()
                .spec(requestSpec)
                .pathParam("id", 999999999)
        .when()
                .get("/booking/{id}")
        .then()
                .statusCode(404);
    }

    @Test(groups = "sanity", description = "DELETE without a token is forbidden")
    public void deleteWithoutTokenIsForbidden() {
        int id = createBooking(TestDataBuilder.validBooking());

        given()
                .spec(requestSpec)
                .pathParam("id", id)                                 // no Cookie header on purpose
        .when()
                .delete("/booking/{id}")
        .then()
                .statusCode(403);
    }

    @Test(groups = "sanity", description = "PATCH without a token is forbidden")
    public void patchWithoutTokenIsForbidden() {
        int id = createBooking(TestDataBuilder.validBooking());

        given()
                .spec(requestSpec)
                .pathParam("id", id)                                 // no Cookie header on purpose
                .body(Map.of("firstname", "NoToken"))
        .when()
                .patch("/booking/{id}")
        .then()
                .statusCode(403);
    }

    @Test(groups = "sanity", description = "PUT with an invalid token is forbidden")
    public void updateWithInvalidTokenIsForbidden() {
        int id = createBooking(TestDataBuilder.validBooking());

        given()
                .spec(requestSpec)
                .header("Cookie", "token=invalidToken123")           // fake token
                .pathParam("id", id)
                .body(TestDataBuilder.validBooking())
        .when()
                .put("/booking/{id}")
        .then()
                .statusCode(403);
    }

    @Test(groups = "sanity", description = "Response Content-Type header is JSON")
    public void responseContentTypeIsJson() {
        int id = createBooking(TestDataBuilder.validBooking());

        given()
                .spec(requestSpec)
                .pathParam("id", id)
        .when()
                .get("/booking/{id}")
        .then()
                .statusCode(200)
                .header("Content-Type", containsString("application/json"));   // "application/json; charset=utf-8"
    }
}
