package com.example.smartdesk;

import com.example.smartdesk.entity.*;
import com.example.smartdesk.entity.enums.BookingStatus;
import com.example.smartdesk.entity.enums.DeskType;
import com.example.smartdesk.exception.CutoffExceededException;
import com.example.smartdesk.repository.*;
import com.example.smartdesk.service.BookingService;
import com.example.smartdesk.service.DeskAssignmentService;
import com.example.smartdesk.service.QuotaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class TimezoneBookingTest {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private DeskRepository deskRepository;

    @Autowired
    private FloorRepository floorRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TeamQuotaRepository teamQuotaRepository;

    @Autowired
    private DeskAssignmentService deskAssignmentService;

    @Autowired
    private QuotaService quotaService;

    private User nyUser;
    private Floor tokyoFloor;
    private Desk desk;

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
        teamQuotaRepository.deleteAll();
        deskRepository.deleteAll();
        userRepository.deleteAll();
        floorRepository.deleteAll();
        teamRepository.deleteAll();

        Team team = teamRepository.save(new Team("Global Team", "GLOBAL", "Worldwide Team"));
        tokyoFloor = floorRepository.save(new Floor(1, "Tokyo HQ Floor", 50, "Asia/Tokyo"));
        desk = deskRepository.save(new Desk("T-101", tokyoFloor, DeskType.HOT, 1.0, 1.0, null));
        // Employee situated in New York timezone (America/New_York)
        nyUser = userRepository.save(new User("NY Employee", "ny@global.com", "America/New_York", team));
    }

    @Test
    @DisplayName("Timezone Edge Case: Cancellation cutoff evaluates accurately across New York and Tokyo timezones")
    void testTimezoneAwareCancellationCutoff() {
        LocalDate bookingDate = LocalDate.now().plusDays(2);
        LocalTime startTime = LocalTime.of(9, 0); // 09:00 AM NY time
        LocalTime endTime = LocalTime.of(17, 0);

        ZoneId nyZone = ZoneId.of("America/New_York");

        // 2 hours before 09:00 AM NY time is 07:00 AM NY time.
        // Simulated instant: 06:59:00 AM NY time (1 minute before cutoff) -> Allowed
        Instant beforeCutoff = ZonedDateTime.of(bookingDate, LocalTime.of(6, 59, 0), nyZone).toInstant();
        Clock clockBefore = Clock.fixed(beforeCutoff, nyZone);

        BookingService serviceBefore = new BookingService(
            bookingRepository, deskRepository, floorRepository, userRepository,
            deskAssignmentService, quotaService, clockBefore, 2
        );

        Booking booking = bookingRepository.save(new Booking(nyUser, desk, tokyoFloor, bookingDate, startTime, endTime));

        Booking cancelled = serviceBefore.cancelBooking(booking.getId(), nyUser.getId());
        assertEquals(BookingStatus.CANCELLED, cancelled.getStatus());

        // Simulated instant: 07:01:00 AM NY time (1 minute after cutoff) -> Rejected
        Instant afterCutoff = ZonedDateTime.of(bookingDate, LocalTime.of(7, 1, 0), nyZone).toInstant();
        Clock clockAfter = Clock.fixed(afterCutoff, nyZone);

        BookingService serviceAfter = new BookingService(
            bookingRepository, deskRepository, floorRepository, userRepository,
            deskAssignmentService, quotaService, clockAfter, 2
        );

        Booking booking2 = bookingRepository.save(new Booking(nyUser, desk, tokyoFloor, bookingDate, startTime, endTime));

        assertThrows(CutoffExceededException.class, () -> {
            serviceAfter.cancelBooking(booking2.getId(), nyUser.getId());
        });
    }
}
