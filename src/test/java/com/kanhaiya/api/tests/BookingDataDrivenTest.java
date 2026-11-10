package com.kanhaiya.api.tests;

import com.kanhaiya.api.base.BaseTest;
import com.kanhaiya.api.models.Booking;
import com.kanhaiya.api.models.BookingDates;
import com.kanhaiya.api.utils.ConfigReader;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Data-driven tests: one test method runs once per row of its @DataProvider.
 * Adding a new case = adding one row, no new test code.
 */
// Allure labels: group this class in the report and set its default importance
@Epic("Restful Booker API")
@Feature("Data-Driven")
@Severity(SeverityLevel.NORMAL)
public class BookingDataDrivenTest extends BaseTest {

    // ---------- Valid bookings: create, then GET returns the same data ----------

    // Each row: case name, booking to send
    @DataProvider(name = "validBookings")
    public Object[][] validBookings() {
        return new Object[][]{
                {"deposit paid, with needs", new Booking("Anna", "Paid", 250, true,
                        new BookingDates("2027-03-01", "2027-03-04"), "Breakfast")},
                {"deposit not paid", new Booking("Ben", "Unpaid", 180, false,
                        new BookingDates("2027-04-10", "2027-04-12"), "Dinner")},
                {"minimum price 1", new Booking("Cara", "Cheap", 1, true,
                        new BookingDates("2027-05-01", "2027-05-02"), "None")},
                {"high price 99999", new Booking("Dev", "Luxury", 99999, true,
                        new BookingDates("2027-06-01", "2027-06-30"), "Spa")},
                {"one-night stay", new Booking("Eve", "Short", 75, false,
                        new BookingDates("2027-07-15", "2027-07-16"), "Late checkout")},
        };
    }

    @Test(groups = "regression", dataProvider = "validBookings",
            description = "Valid booking variations are saved exactly as sent")
    public void validBookingRoundTrip(String caseName, Booking sent) {
        int id = createBooking(sent);                   // shared helper from BaseTest

        // Read it back and compare every field
        assertSameBooking(getBooking(id), sent);
    }

    // ---------- Malformed JSON body is rejected with 400 ----------

    // Each row: case name, raw (broken) request body
    @DataProvider(name = "malformedBodies")
    public Object[][] malformedBodies() {
        return new Object[][]{
                {"cut off mid-object", "{\"firstname\":"},
                {"plain text", "not json"},
                {"trailing comma", "{\"firstname\":\"A\",}"},
                {"single quotes", "{'firstname':'A'}"},
        };
    }

    @Test(groups = "regression", dataProvider = "malformedBodies",
            description = "Malformed JSON body returns 400 Bad Request")
    public void malformedJsonIsRejected(String caseName, String rawBody) {
        given()
                .spec(requestSpec)
                .body(rawBody)                      // a String is sent as-is, not converted to JSON
        .when()
                .post("/booking")
        .then()
                .statusCode(400);
    }

    // ---------- Invalid login variations ----------

    // Each row: case name, request body (wrong password for admin is already in AuthTest)
    @DataProvider(name = "invalidLogins")
    public Object[][] invalidLogins() {
        String password = ConfigReader.get("password");
        Map<String, String> missingUsername = Map.of("password", password);
        return new Object[][]{
                {"unknown user", Map.of("username", "nobody", "password", password)},
                {"username in upper case", Map.of("username", "ADMIN", "password", password)},
                {"username with leading space", Map.of("username", " admin", "password", password)},
                {"empty strings", Map.of("username", "", "password", "")},
                {"missing username", missingUsername},
        };
    }

    @Test(groups = "regression", dataProvider = "invalidLogins",
            description = "Invalid login variations get 'Bad credentials' and no token")
    public void invalidLoginIsRejected(String caseName, Map<String, String> credentials) {
        given()
                .spec(requestSpec)
                .body(credentials)
        .when()
                .post("/auth")
        .then()
                .body("reason", equalTo("Bad credentials"))
                .body("token", nullValue());        // most important: no token is issued
    }

    // ---------- Known API issues (wrong behaviour, asserted as it really is) ----------

    // Helper: a complete valid booking as a Map, so single fields can be removed or broken
    private static Map<String, Object> bookingMap() {
        Map<String, Object> body = new HashMap<>();
        body.put("firstname", "Known");
        body.put("lastname", "Issue");
        body.put("totalprice", 100);
        body.put("depositpaid", true);
        body.put("bookingdates", Map.of("checkin", "2027-08-01", "checkout", "2027-08-03"));
        return body;
    }

    // Each row: case name, body, expected status, JSON path to check (or null), expected value
    @DataProvider(name = "knownIssues")
    public Object[][] knownIssues() {
        Map<String, Object> missingFirstname = bookingMap();
        missingFirstname.remove("firstname");

        Map<String, Object> priceAsText = bookingMap();
        priceAsText.put("totalprice", "abc");

        Map<String, Object> depositAsText = bookingMap();
        depositAsText.put("depositpaid", "yes");

        return new Object[][]{
                {"missing firstname", missingFirstname, 500, null, null},
                {"empty object", new HashMap<String, Object>(), 500, null, null},
                {"totalprice as text", priceAsText, 200, "booking.totalprice", null},
                {"depositpaid as text", depositAsText, 200, "booking.depositpaid", true},
        };
    }

    @Severity(SeverityLevel.MINOR)   // documents existing API bugs
    @Test(groups = {"regression", "known-issues"}, dataProvider = "knownIssues",
            description = "Documents known API bugs in booking validation")
    public void knownValidationIssues(String caseName, Map<String, Object> body,
                                      int expectedStatus, String path, Object expectedValue) {
        // Known API behaviour: missing fields crash the server (500 instead of 400), and
        // wrong types are silently accepted ("abc" price saved as null, "yes" saved as true)
        var response = given()
                .spec(requestSpec)
                .body(body)
        .when()
                .post("/booking")
        .then()
                .statusCode(expectedStatus);

        if (path != null) {
            response.body(path, equalTo(expectedValue));   // check what the API actually saved
        }
    }
}
