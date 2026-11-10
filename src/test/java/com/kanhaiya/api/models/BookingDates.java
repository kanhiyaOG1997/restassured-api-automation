package com.kanhaiya.api.models;

/**
 * POJO for the "bookingdates" JSON object: { "checkin": "...", "checkout": "..." }
 */
public class BookingDates {

    private String checkin;
    private String checkout;

    // Empty constructor is needed by Jackson to convert JSON back into this object
    public BookingDates() {
    }

    public BookingDates(String checkin, String checkout) {
        this.checkin = checkin;
        this.checkout = checkout;
    }

    public String getCheckin() { return checkin; }
    public void setCheckin(String checkin) { this.checkin = checkin; }

    public String getCheckout() { return checkout; }
    public void setCheckout(String checkout) { this.checkout = checkout; }
}
