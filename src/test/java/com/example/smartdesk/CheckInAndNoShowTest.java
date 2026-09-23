package com.example.smartdesk;

import com.example.smartdesk.dto.request.BookingRequest;
import com.example.smartdesk.entity.*;
import com.example.smartdesk.entity.enums.BookingStatus;
import com.example.smartdesk.entity.enums.DeskType;
import com.example.smartdesk.exception.CutoffExceededException;
import com.example.smartdesk.exception.InvalidBookingException;
import com.example.smartdesk.repository.*;
import com.example.smartdesk.scheduler.NoShowReleaseScheduler;
import com.example.smartdesk.service.BookingService;
import com.example.smartdesk.service.CheckInService;
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
public class CheckInAndNoShowTest {

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
    private BookingService bookingService;

    private User user;
    private User otherUser;
    private Floor floor;
    private Desk desk;
    private ZoneId zone = ZoneId.of("Asia/Kolkata");

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
        teamQuotaRepository.deleteAll();
        deskRepository.deleteAll();
        userRepository.deleteAll();
        floorRepository.deleteAll();
        teamRepository.deleteAll();

        Team team = teamRepository.save(new Team("CheckIn Team", "CHK", "Testing CheckIn"));
        floor = floorRepository.save(new Floor(8, "Floor 8", 20, "Asia/Kolkata"));
        desk = deskRepository.save(new Desk("D-801", floor, DeskType.HOT, 1.0, 1.0, null));
        user = userRepository.save(new User("CheckIn User", "checkin@test.com", "Asia/Kolkata", team));
        otherUser = userRepository.save(new User("Other User", "other@test.com", "Asia/Kolkata", team));
    }

    @Test
    @DisplayName("Check-in Allowed: Within valid check-in window (e.g. 09:15 for 09:00 start with 30m grace)")
    void testCheckInSuccessfulWithinWindow() {
        LocalDate date = LocalDate.now().plusDays(1);
        LocalTime start = LocalTime.of(9, 0);
        LocalTime end = LocalTime.of(17, 0);

        // Simulated time: 09:15 AM
        Instant simulatedNow = ZonedDateTime.of(date, LocalTime.of(9, 15), zone).toInstant();
        Clock fixedClock = Clock.fixed(simulatedNow, zone);
        CheckInService checkInService = new CheckInService(bookingRepository, fixedClock, 30);

        Booking booking = bookingRepository.save(new Booking(user, desk, floor, date, start, end));

        Booking checkedInBooking = checkInService.checkIn(booking.getId());

        assertEquals(BookingStatus.CHECKED_IN, checkedInBooking.getStatus());
        assertNotNull(checkedInBooking.getCheckedInAt());
    }

    @Test
    @DisplayName("Check-in Rejected: Too early (e.g. 07:30 for 09:00 start)")
    void testCheckInTooEarlyRejected() {
        LocalDate date = LocalDate.now().plusDays(1);
        LocalTime start = LocalTime.of(9, 0);
        LocalTime end = LocalTime.of(17, 0);

        // Simulated time: 07:30 AM (90 mins before start)
        Instant simulatedNow = ZonedDateTime.of(date, LocalTime.of(7, 30), zone).toInstant();
        Clock fixedClock = Clock.fixed(simulatedNow, zone);
        CheckInService checkInService = new CheckInService(bookingRepository, fixedClock, 30);

        Booking booking = bookingRepository.save(new Booking(user, desk, floor, date, start, end));

        assertThrows(InvalidBookingException.class, () -> {
            checkInService.checkIn(booking.getId());
        });
    }

    @Test
    @DisplayName("Check-in Expired: Past grace period (e.g. 09:35 for 09:00 start with 30m grace) -> Auto-Released")
    void testCheckInPastGracePeriodAutoReleased() {
        LocalDate date = LocalDate.now().plusDays(1);
        LocalTime start = LocalTime.of(9, 0);
        LocalTime end = LocalTime.of(17, 0);

        // Simulated time: 09:35 AM (35 mins after start)
        Instant simulatedNow = ZonedDateTime.of(date, LocalTime.of(9, 35), zone).toInstant();
        Clock fixedClock = Clock.fixed(simulatedNow, zone);
        CheckInService checkInService = new CheckInService(bookingRepository, fixedClock, 30);

        Booking booking = bookingRepository.save(new Booking(user, desk, floor, date, start, end));

        assertThrows(CutoffExceededException.class, () -> {
            checkInService.checkIn(booking.getId());
        });

        Booking updated = bookingRepository.findById(booking.getId()).orElseThrow();
        assertEquals(BookingStatus.RELEASED, updated.getStatus(), "Booking should be marked RELEASED");
    }

    @Test
    @DisplayName("No-Show Scheduler: Auto-releases unconfirmed bookings past grace period and frees desk for others")
    void testNoShowSchedulerReleasesDesk() {
        LocalDate date = LocalDate.now().plusDays(1);
        LocalTime start = LocalTime.of(9, 0);
        LocalTime end = LocalTime.of(17, 0);

        // User books desk
        Booking booking = bookingRepository.save(new Booking(user, desk, floor, date, start, end));
        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());

        // Simulated time: 09:31 AM (past 30 min grace period)
        Instant simulatedNow = ZonedDateTime.of(date, LocalTime.of(9, 31), zone).toInstant();
        Clock fixedClock = Clock.fixed(simulatedNow, zone);

        NoShowReleaseScheduler scheduler = new NoShowReleaseScheduler(bookingRepository, fixedClock, 30);
        scheduler.processNoShowReleases();

        // Verify booking status is now RELEASED
        Booking releasedBooking = bookingRepository.findById(booking.getId()).orElseThrow();
        assertEquals(BookingStatus.RELEASED, releasedBooking.getStatus());

        // Now otherUser can successfully book that same desk!
        Booking newBooking = bookingService.createBooking(
            new BookingRequest(otherUser.getId(), floor.getId(), date, start, end, desk.getId())
        );
        assertNotNull(newBooking);
        assertEquals(BookingStatus.CONFIRMED, newBooking.getStatus());
        assertEquals(desk.getId(), newBooking.getDesk().getId());
    }
}
