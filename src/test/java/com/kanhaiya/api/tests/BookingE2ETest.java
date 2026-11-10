package com.kanhaiya.api.tests;

import com.kanhaiya.api.base.BaseTest;
import com.kanhaiya.api.models.Booking;
import com.kanhaiya.api.models.BookingDates;
import com.kanhaiya.api.utils.TestDataBuilder;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * End-to-end tests: full user journeys that chain several API calls.
 * Each test is one complete journey and does not depend on any other test.
 */
// Allure labels: group this class in the report and set its default importance
@Epic("Restful Booker API")
@Feature("End-to-End")
@Severity(SeverityLevel.CRITICAL)
public class BookingE2ETest extends BaseTest {

    // Helper: deletes a booking with the given token (201 = deleted on this API)
    private void deleteBooking(int id, String authToken) {
        given()
                .spec(requestSpec)
                .header("Cookie", "token=" + authToken)
                .pathParam("id", id)
        .when()
                .delete("/booking/{id}")
        .then()
                .statusCode(201);
    }

    // Helper: returns all booking ids matching a last name
    private List<Integer> findIdsByLastname(String lastname) {
        return given()
                .spec(requestSpec)
                .queryParam("lastname", lastname)
        .when()
                .get("/booking")
        .then()
                .statusCode(200)
                .extract().jsonPath().getList("bookingid", Integer.class);
    }

    @Test(groups = "e2e", description = "Guest journey: login, create, search, view, edit, cancel")
    public void fullGuestJourney() {
        // Step 1: login and get a fresh token
        String userToken = generateToken();

        // Step 2: create a booking with a unique last name so we can search for it
        Booking booking = TestDataBuilder.validBooking();
        booking.setLastname("Journey" + System.nanoTime());
        int id = createBooking(booking);

        // Step 3: search by name - our booking must be found
        Assert.assertTrue(findIdsByLastname(booking.getLastname()).contains(id),
                "Created booking should be found by last name");

        // Step 4: view the booking
        given()
                .spec(requestSpec)
                .pathParam("id", id)
        .when()
                .get("/booking/{id}")
        .then()
                .statusCode(200)
                .body("firstname", equalTo(booking.getFirstname()));

        // Step 5: add a special request with PATCH
        given()
                .spec(requestSpec)
                .header("Cookie", "token=" + userToken)
                .pathParam("id", id)
                .body(Map.of("additionalneeds", "Late checkout"))
        .when()
                .patch("/booking/{id}")
        .then()
                .statusCode(200);

        // Step 6: view again - the change is saved, other fields untouched
        Booking afterPatch = given()
                .spec(requestSpec)
                .pathParam("id", id)
        .when()
                .get("/booking/{id}")
        .then()
                .statusCode(200)
                .extract().as(Booking.class);
        Assert.assertEquals(afterPatch.getAdditionalneeds(), "Late checkout");
        Assert.assertEquals(afterPatch.getLastname(), booking.getLastname());

        // Step 7: cancel the booking
        deleteBooking(id, userToken);

        // Step 8: booking is gone
        given()
                .spec(requestSpec)
                .pathParam("id", id)
        .when()
                .get("/booking/{id}")
        .then()
                .statusCode(404);
    }

    @Test(groups = "e2e", description = "Change dates: PUT new dates, then find booking by new checkin")
    public void changeDatesAndSearchByNewCheckin() {
        int id = createBooking(TestDataBuilder.validBooking());

        // Move the stay to new dates
        LocalDate newCheckin = LocalDate.now().plusDays(200);
        Booking moved = TestDataBuilder.validBooking();
        moved.setBookingdates(new BookingDates(newCheckin.toString(), newCheckin.plusDays(3).toString()));

        given()
                .spec(requestSpec)
                .header("Cookie", "token=" + token)
                .pathParam("id", id)
                .body(moved)
        .when()
                .put("/booking/{id}")
        .then()
                .statusCode(200)
                .body("bookingdates.checkin", equalTo(newCheckin.toString()));

        // Known API behaviour: ?checkin= returns bookings with checkin strictly AFTER the date,
        // so we search from the day before the new checkin to find our booking
        given()
                .spec(requestSpec)
                .queryParam("checkin", newCheckin.minusDays(1).toString())
        .when()
                .get("/booking")
        .then()
                .statusCode(200)
                .body("bookingid", hasItem(id));
    }

    @Test(groups = "e2e", description = "One token can update and delete two different bookings")
    public void oneTokenForManyActions() {
        String sharedToken = generateToken();                        // single login for the whole flow
        int firstId = createBooking(TestDataBuilder.validBooking());
        int secondId = createBooking(TestDataBuilder.validBooking());

        // Update both bookings with the same token
        for (int id : List.of(firstId, secondId)) {
            given()
                    .spec(requestSpec)
                    .header("Cookie", "token=" + sharedToken)
                    .pathParam("id", id)
                    .body(Map.of("totalprice", 999))
            .when()
                    .patch("/booking/{id}")
            .then()
                    .statusCode(200)
                    .body("totalprice", equalTo(999));
        }

        // Delete both bookings with the same token
        deleteBooking(firstId, sharedToken);
        deleteBooking(secondId, sharedToken);
    }

    @Test(groups = "e2e", description = "Create 3 bookings, find exactly 3, delete all, find 0")
    public void multipleBookingsLifecycle() {
        String groupName = "Group" + System.nanoTime();              // unique last name for this test
        List<Integer> createdIds = new ArrayList<>();

        // Create 3 bookings for the same guest group
        for (int i = 0; i < 3; i++) {
            Booking booking = TestDataBuilder.validBooking();
            booking.setLastname(groupName);
            createdIds.add(createBooking(booking));
        }

        // Search returns exactly our 3 bookings
        List<Integer> found = findIdsByLastname(groupName);
        Assert.assertEquals(found.size(), 3, "Should find exactly 3 bookings");
        Assert.assertTrue(found.containsAll(createdIds), "Found ids should match created ids");

        // Delete all of them
        for (int id : createdIds) {
            deleteBooking(id, token);
        }

        // Search now returns nothing
        Assert.assertTrue(findIdsByLastname(groupName).isEmpty(), "No bookings should remain");
    }
}
