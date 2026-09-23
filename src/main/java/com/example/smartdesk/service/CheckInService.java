package com.example.smartdesk.service;

import com.example.smartdesk.entity.Booking;
import com.example.smartdesk.entity.enums.BookingStatus;
import com.example.smartdesk.exception.CutoffExceededException;
import com.example.smartdesk.exception.InvalidBookingException;
import com.example.smartdesk.exception.ResourceNotFoundException;
import com.example.smartdesk.repository.BookingRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;

@Service
public class CheckInService {

    private final BookingRepository bookingRepository;
    private final Clock clock;
    private final int gracePeriodMinutes;

    public CheckInService(
        BookingRepository bookingRepository,
        Clock clock,
        @Value("${smartdesk.booking.grace-period-minutes:30}") int gracePeriodMinutes
    ) {
        this.bookingRepository = bookingRepository;
        this.clock = clock;
        this.gracePeriodMinutes = gracePeriodMinutes;
    }

    /**
     * Checks in an employee for their confirmed booking.
     * Evaluates the check-in window relative to the employee's / floor's timezone.
     */
    @Transactional
    public Booking checkIn(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new ResourceNotFoundException("Booking with id " + bookingId + " not found"));

        if (booking.getStatus() == BookingStatus.CHECKED_IN) {
            return booking; // idempotent
        }

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new InvalidBookingException("Cannot check in to a cancelled booking");
        }

        if (booking.getStatus() == BookingStatus.RELEASED) {
            throw new CutoffExceededException("Booking has already been released due to check-in timeout");
        }

        // Evaluate Timezone-aware check-in window
        String timezoneStr = booking.getUser() != null && booking.getUser().getTimezone() != null
            ? booking.getUser().getTimezone()
            : (booking.getFloor() != null ? booking.getFloor().getTimezone() : "UTC");
        
        ZoneId zoneId;
        try {
            zoneId = ZoneId.of(timezoneStr);
        } catch (Exception e) {
            zoneId = ZoneId.of("UTC");
        }

        ZonedDateTime nowZoned = ZonedDateTime.now(clock).withZoneSameInstant(zoneId);
        ZonedDateTime bookingStartZoned = ZonedDateTime.of(booking.getBookingDate(), booking.getStartTime(), zoneId);
        ZonedDateTime earliestCheckIn = bookingStartZoned.minusMinutes(60); // 1 hr before start
        ZonedDateTime latestCheckIn = bookingStartZoned.plusMinutes(gracePeriodMinutes); // grace period

        if (nowZoned.isBefore(earliestCheckIn)) {
            throw new InvalidBookingException("Check-in is not yet open. You can check in starting 60 minutes before your booking.");
        }

        if (nowZoned.isAfter(latestCheckIn)) {
            booking.setStatus(BookingStatus.RELEASED);
            bookingRepository.save(booking);
            throw new CutoffExceededException("Check-in grace period expired (" + gracePeriodMinutes + " minutes). Desk has been released.");
        }

        booking.setStatus(BookingStatus.CHECKED_IN);
        booking.setCheckedInAt(Instant.now(clock));
        return bookingRepository.save(booking);
    }
}
