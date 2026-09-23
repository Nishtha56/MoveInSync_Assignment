package com.example.smartdesk.scheduler;

import com.example.smartdesk.entity.Booking;
import com.example.smartdesk.entity.enums.BookingStatus;
import com.example.smartdesk.repository.BookingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;

@Component
public class NoShowReleaseScheduler {

    private static final Logger log = LoggerFactory.getLogger(NoShowReleaseScheduler.class);

    private final BookingRepository bookingRepository;
    private final Clock clock;
    private final int gracePeriodMinutes;

    public NoShowReleaseScheduler(
        BookingRepository bookingRepository,
        Clock clock,
        @Value("${smartdesk.booking.grace-period-minutes:30}") int gracePeriodMinutes
    ) {
        this.bookingRepository = bookingRepository;
        this.clock = clock;
        this.gracePeriodMinutes = gracePeriodMinutes;
    }

    /**
     * Periodically inspects confirmed bookings. If an employee has not checked in within
     * the configured grace period after the booking start time, the booking is automatically
     * transitioned to RELEASED and the desk is returned to the available pool.
     */
    @Scheduled(fixedRateString = "${smartdesk.scheduler.no-show-rate-ms:30000}")
    @Transactional
    public void processNoShowReleases() {
        ZonedDateTime now = ZonedDateTime.now(clock);
        LocalDate currentDate = now.toLocalDate();
        LocalTime graceThresholdTime = now.toLocalTime().minusMinutes(gracePeriodMinutes);

        List<Booking> unconfirmedBookings = bookingRepository.findUnconfirmedBookingsPastGrace(
            BookingStatus.CONFIRMED, currentDate, graceThresholdTime
        );

        if (!unconfirmedBookings.isEmpty()) {
            log.info("Processing {} no-show booking(s) for automatic desk release", unconfirmedBookings.size());
            for (Booking booking : unconfirmedBookings) {
                booking.setStatus(BookingStatus.RELEASED);
                bookingRepository.save(booking);
                log.info("Auto-released booking id={} (User '{}', Desk '{}', Date={}, StartTime={}) due to no-show",
                    booking.getId(),
                    booking.getUser() != null ? booking.getUser().getName() : "Unknown",
                    booking.getDesk() != null ? booking.getDesk().getDeskNumber() : "Unknown",
                    booking.getBookingDate(),
                    booking.getStartTime()
                );
            }
        }
    }
}
