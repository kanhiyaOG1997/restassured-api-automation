package com.kanhaiya.api.models;

/**
 * POJO (Plain Old Java Object) for a booking.
 * RestAssured + Jackson automatically convert this object to JSON (serialization)
 * when sent as a request body, and JSON back to this object (deserialization).
 */
public class Booking {

    private String firstname;
    private String lastname;
    private int totalprice;
    private boolean depositpaid;
    private BookingDates bookingdates;   // nested JSON object
    private String additionalneeds;

    public Booking() {
    }

    public Booking(String firstname, String lastname, int totalprice, boolean depositpaid,
                   BookingDates bookingdates, String additionalneeds) {
        this.firstname = firstname;
        this.lastname = lastname;
        this.totalprice = totalprice;
        this.depositpaid = depositpaid;
        this.bookingdates = bookingdates;
        this.additionalneeds = additionalneeds;
    }

    public String getFirstname() { return firstname; }
    public void setFirstname(String firstname) { this.firstname = firstname; }

    public String getLastname() { return lastname; }
    public void setLastname(String lastname) { this.lastname = lastname; }

    public int getTotalprice() { return totalprice; }
    public void setTotalprice(int totalprice) { this.totalprice = totalprice; }

    public boolean isDepositpaid() { return depositpaid; }
    public void setDepositpaid(boolean depositpaid) { this.depositpaid = depositpaid; }

    public BookingDates getBookingdates() { return bookingdates; }
    public void setBookingdates(BookingDates bookingdates) { this.bookingdates = bookingdates; }

    public String getAdditionalneeds() { return additionalneeds; }
    public void setAdditionalneeds(String additionalneeds) { this.additionalneeds = additionalneeds; }
}
