package com.kanhaiya.api.utils;

import com.kanhaiya.api.models.Booking;
import com.kanhaiya.api.models.BookingDates;

import java.time.LocalDate;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Builds test data so each test can create its own fresh booking.
 */
public final class TestDataBuilder {

    private TestDataBuilder() {
    }

    // A valid booking with a random name, price and future dates
    public static Booking validBooking() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        LocalDate checkin = LocalDate.now().plusDays(random.nextInt(10, 60));
        LocalDate checkout = checkin.plusDays(random.nextInt(1, 7));

        return new Booking(
                "User" + random.nextInt(1000, 9999),
                "Test" + random.nextInt(1000, 9999),
                random.nextInt(100, 5000),
                true,
                new BookingDates(checkin.toString(), checkout.toString()),
                "Breakfast");
    }
}
