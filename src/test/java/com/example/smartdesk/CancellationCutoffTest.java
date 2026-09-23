package com.example.smartdesk;

import com.example.smartdesk.dto.request.BookingRequest;
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
public class CancellationCutoffTest {

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

    private User user;
    private Floor floor;
    private Desk desk;
    private ZoneId kolkataZone = ZoneId.of("Asia/Kolkata");

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
        teamQuotaRepository.deleteAll();
        deskRepository.deleteAll();
        userRepository.deleteAll();
        floorRepository.deleteAll();
        teamRepository.deleteAll();

        Team team = teamRepository.save(new Team("Cutoff Team", "CUT", "Testing Cutoff"));
        floor = floorRepository.save(new Floor(7, "Floor 7", 50, "Asia/Kolkata"));
        desk = deskRepository.save(new Desk("D-701", floor, DeskType.HOT, 1.0, 1.0, null));
        user = userRepository.save(new User("Cutoff User", "cutoff@test.com", "Asia/Kolkata", team));
    }

    @Test
    @DisplayName("Cancellation Allowed: 1 second BEFORE cutoff deadline (07:59:59 for 10:00 start with 2h cutoff)")
    void testCancellationAllowedBeforeCutoff() {
        LocalDate bookingDate = LocalDate.now().plusDays(1);
        LocalTime startTime = LocalTime.of(10, 0);
        LocalTime endTime = LocalTime.of(18, 0);

        // Simulated time: 07:59:59 (1 second before 08:00 cutoff deadline)
        Instant simulatedNow = ZonedDateTime.of(bookingDate, LocalTime.of(7, 59, 59), kolkataZone).toInstant();
        Clock fixedClock = Clock.fixed(simulatedNow, kolkataZone);

        BookingService serviceWithClock = new BookingService(
            bookingRepository, deskRepository, floorRepository, userRepository,
            deskAssignmentService, quotaService, fixedClock, 2
        );

        Booking booking = bookingRepository.save(
            new Booking(user, desk, floor, bookingDate, startTime, endTime)
        );

        // Cancel at 07:59:59 -> SHOULD SUCCEED
        Booking cancelled = serviceWithClock.cancelBooking(booking.getId(), user.getId());
        assertEquals(BookingStatus.CANCELLED, cancelled.getStatus(), "Cancellation should succeed before cutoff");
    }

    @Test
    @DisplayName("Cancellation Rejected: EXACTLY at cutoff deadline (08:00:00 for 10:00 start with 2h cutoff)")
    void testCancellationRejectedExactlyAtCutoff() {
        LocalDate bookingDate = LocalDate.now().plusDays(1);
        LocalTime startTime = LocalTime.of(10, 0);
        LocalTime endTime = LocalTime.of(18, 0);

        // Simulated time: 08:00:00 (Exact boundary cutoff deadline)
        Instant simulatedNow = ZonedDateTime.of(bookingDate, LocalTime.of(8, 0, 0), kolkataZone).toInstant();
        Clock fixedClock = Clock.fixed(simulatedNow, kolkataZone);

        BookingService serviceWithClock = new BookingService(
            bookingRepository, deskRepository, floorRepository, userRepository,
            deskAssignmentService, quotaService, fixedClock, 2
        );

        // Create booking earlier
        Booking booking = bookingRepository.save(new Booking(user, desk, floor, bookingDate, startTime, endTime));

        // Cancel at 08:00:00 -> MUST THROW CutoffExceededException
        CutoffExceededException ex = assertThrows(CutoffExceededException.class, () -> {
            serviceWithClock.cancelBooking(booking.getId(), user.getId());
        });

        assertTrue(ex.getMessage().contains("Cancellation cutoff deadline has passed"));
    }

    @Test
    @DisplayName("Cancellation Rejected: After start time (10:30 for 10:00 start)")
    void testCancellationRejectedAfterStartTime() {
        LocalDate bookingDate = LocalDate.now().plusDays(1);
        LocalTime startTime = LocalTime.of(10, 0);
        LocalTime endTime = LocalTime.of(18, 0);

        // Simulated time: 10:30:00 (After start time)
        Instant simulatedNow = ZonedDateTime.of(bookingDate, LocalTime.of(10, 30, 0), kolkataZone).toInstant();
        Clock fixedClock = Clock.fixed(simulatedNow, kolkataZone);

        BookingService serviceWithClock = new BookingService(
            bookingRepository, deskRepository, floorRepository, userRepository,
            deskAssignmentService, quotaService, fixedClock, 2
        );

        Booking booking = bookingRepository.save(new Booking(user, desk, floor, bookingDate, startTime, endTime));

        assertThrows(CutoffExceededException.class, () -> {
            serviceWithClock.cancelBooking(booking.getId(), user.getId());
        });
    }
}
