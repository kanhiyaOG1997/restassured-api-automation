package com.kanhaiya.api.tests;

import com.kanhaiya.api.base.BaseTest;
import com.kanhaiya.api.models.Booking;
import com.kanhaiya.api.models.BookingDates;
import com.kanhaiya.api.utils.ConfigReader;
import com.kanhaiya.api.utils.TestDataBuilder;
import io.restassured.http.ContentType;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Edge cases and boundary values for the booking API.
 * Every test is independent: it creates its own booking when it needs one.
 */
// Allure labels: group this class in the report and set its default importance
@Epic("Restful Booker API")
@Feature("Edge Cases")
@Severity(SeverityLevel.NORMAL)
public class BookingEdgeCaseTest extends BaseTest {

    // Helper: builds the Base64 part of a Basic auth header from config credentials
    private String basicAuthValue() {
        String credentials = ConfigReader.get("username") + ":" + ConfigReader.get("password");
        return Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    @Test(groups = "regression", description = "depositpaid=false and no additionalneeds is accepted")
    public void createWithoutAdditionalNeeds() {
        // Use a Map so "additionalneeds" is completely left out of the JSON (not sent as null)
        Booking base = TestDataBuilder.validBooking();
        Map<String, Object> body = Map.of(
                "firstname", base.getFirstname(),
                "lastname", base.getLastname(),
                "totalprice", base.getTotalprice(),
                "depositpaid", false,
                "bookingdates", base.getBookingdates());

        given()
                .spec(requestSpec)
                .body(body)
        .when()
                .post("/booking")
        .then()
                .statusCode(200)
                .body("booking.depositpaid", equalTo(false))
                .body("booking", not(hasKey("additionalneeds")));   // field is absent in response
    }

    @Test(groups = "regression", description = "totalprice = 0 is accepted and echoed back")
    public void createWithZeroPrice() {
        Booking booking = TestDataBuilder.validBooking();
        booking.setTotalprice(0);                                    // boundary value

        given()
                .spec(requestSpec)
                .body(booking)
        .when()
                .post("/booking")
        .then()
                .statusCode(200)
                .body("booking.totalprice", equalTo(0));
    }

    @Test(groups = "regression", description = "Very long firstname (255 chars) is accepted")
    public void createWithLongFirstname() {
        String longName = "a".repeat(255);                           // 255-character string
        Booking booking = TestDataBuilder.validBooking();
        booking.setFirstname(longName);

        given()
                .spec(requestSpec)
                .body(booking)
        .when()
                .post("/booking")
        .then()
                .statusCode(200)
                .body("booking.firstname", equalTo(longName))
                .body("booking.firstname.length()", equalTo(255));
    }

    @Test(groups = "regression", description = "Unicode / special characters in name are echoed back")
    public void createWithUnicodeName() {
        Booking booking = TestDataBuilder.validBooking();
        booking.setFirstname("José");
        booking.setLastname("Ñoño");

        // Deserialize the nested "booking" object into our POJO
        Booking created = given()
                .spec(requestSpec)
                .body(booking)
        .when()
                .post("/booking")
        .then()
                .statusCode(200)
                .extract().jsonPath().getObject("booking", Booking.class);

        Assert.assertEquals(created.getFirstname(), "José");
        Assert.assertEquals(created.getLastname(), "Ñoño");
    }

    @Test(groups = "regression", description = "Checkout before checkin - documents actual behaviour")
    public void createWithCheckoutBeforeCheckin() {
        Booking booking = TestDataBuilder.validBooking();
        booking.setBookingdates(new BookingDates("2026-12-10", "2026-12-01"));   // checkout < checkin

        // Known API behaviour: no date validation, the invalid range is accepted with 200
        given()
                .spec(requestSpec)
                .body(booking)
        .when()
                .post("/booking")
        .then()
                .statusCode(200)
                .body("booking.bookingdates.checkin", equalTo("2026-12-10"))
                .body("booking.bookingdates.checkout", equalTo("2026-12-01"));
    }

    @Test(groups = "regression", description = "Filter bookings by checkin date returns a list")
    public void filterByCheckinDate() {
        Booking booking = TestDataBuilder.validBooking();
        createBooking(booking);                                      // make sure data exists

        given()
                .spec(requestSpec)
                .queryParam("checkin", booking.getBookingdates().getCheckin())   // ?checkin=yyyy-MM-dd
        .when()
                .get("/booking")
        .then()
                .statusCode(200)
                .body("$", instanceOf(java.util.List.class))         // response root is a JSON array
                .body("bookingid", everyItem(notNullValue()));
    }

    @Test(groups = "regression", description = "Filter with a name that does not exist returns empty list")
    public void filterByUnknownName() {
        given()
                .spec(requestSpec)
                .queryParam("firstname", "NoSuchName" + System.nanoTime())   // unique, never used
        .when()
                .get("/booking")
        .then()
                .statusCode(200)
                .body("size()", equalTo(0));                         // empty JSON array []
    }

    @Test(groups = "regression", description = "Update using Basic auth header instead of cookie token")
    public void updateWithBasicAuth() {
        int id = createBooking(TestDataBuilder.validBooking());
        Booking updated = TestDataBuilder.validBooking();
        updated.setFirstname("BasicAuth");

        given()
                .spec(requestSpec)
                // Basic auth = "Basic " + Base64("username:password"), an alternative to the Cookie token
                .header("Authorization", "Basic " + basicAuthValue())
                .pathParam("id", id)
                .body(updated)
        .when()
                .put("/booking/{id}")
        .then()
                .statusCode(200)
                .body("firstname", equalTo("BasicAuth"));
    }

    @Test(groups = "regression", description = "PATCH a booking that does not exist")
    public void patchNonExistentBooking() {
        given()
                .spec(requestSpec)
                .header("Cookie", "token=" + token)
                .pathParam("id", 999999999)                          // id that does not exist
                .body(Map.of("firstname", "Ghost"))
        .when()
                .patch("/booking/{id}")
        .then()
                // Known API behaviour: returns 405 Method Not Allowed instead of 404 Not Found
                .statusCode(405);
    }

    @Test(groups = "regression", description = "Deleting the same booking twice")
    public void deleteBookingTwice() {
        int id = createBooking(TestDataBuilder.validBooking());

        // First delete succeeds (this API returns 201 on delete)
        given()
                .spec(requestSpec)
                .header("Cookie", "token=" + token)
                .pathParam("id", id)
        .when()
                .delete("/booking/{id}")
        .then()
                .statusCode(201);

        // Second delete: booking is already gone
        given()
                .spec(requestSpec)
                .header("Cookie", "token=" + token)
                .pathParam("id", id)
        .when()
                .delete("/booking/{id}")
        .then()
                // Known API behaviour: returns 405 Method Not Allowed instead of 404 Not Found
                .statusCode(405);
    }

    @Test(groups = "regression", description = "Wrong Content-Type (text/plain) on create is rejected")
    public void createWithWrongContentType() {
        given()
                .spec(requestSpec)
                .contentType(ContentType.TEXT)                       // overrides JSON from requestSpec
                .body("{\"firstname\":\"Plain\"}")
        .when()
                .post("/booking")
        .then()
                // Known API behaviour: returns 500 Internal Server Error instead of 400/415
                .statusCode(not(200));
    }

    @Test(groups = "regression", description = "POST /auth responds in under 3 seconds")
    public void authResponseTime() {
        given()
                .spec(requestSpec)
                .body(Map.of(
                        "username", ConfigReader.get("username"),
                        "password", ConfigReader.get("password")))
        .when()
                .post("/auth")
        .then()
                .statusCode(200)
                .time(lessThan(3L), TimeUnit.SECONDS);               // response time assertion
    }
}
