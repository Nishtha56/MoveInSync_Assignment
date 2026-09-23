package com.example.smartdesk;

import com.example.smartdesk.dto.request.BookingRequest;
import com.example.smartdesk.entity.*;
import com.example.smartdesk.entity.enums.DeskType;
import com.example.smartdesk.exception.QuotaExceededException;
import com.example.smartdesk.repository.*;
import com.example.smartdesk.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class QuotaValidationTest {

    @Autowired
    private BookingService bookingService;

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

    private Team teamAlpha;
    private Team teamBeta;
    private Floor limitedFloor;
    private User u1, u2, u3;
    private User betaUser;

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
        teamQuotaRepository.deleteAll();
        deskRepository.deleteAll();
        userRepository.deleteAll();
        teamRepository.deleteAll();
        floorRepository.deleteAll();

        teamAlpha = teamRepository.save(new Team("Team Alpha", "ALPHA", "Eng"));
        teamBeta = teamRepository.save(new Team("Team Beta", "BETA", "Product"));

        // Limited floor with capacity = 2
        limitedFloor = floorRepository.save(new Floor(1, "Small Floor", 2, "Asia/Kolkata"));

        deskRepository.save(new Desk("D-1", limitedFloor, DeskType.HOT, 1.0, 1.0, null));
        deskRepository.save(new Desk("D-2", limitedFloor, DeskType.HOT, 2.0, 1.0, null));
        deskRepository.save(new Desk("D-3", limitedFloor, DeskType.HOT, 3.0, 1.0, null));

        u1 = userRepository.save(new User("Alpha 1", "a1@test.com", "Asia/Kolkata", teamAlpha));
        u2 = userRepository.save(new User("Alpha 2", "a2@test.com", "Asia/Kolkata", teamAlpha));
        u3 = userRepository.save(new User("Alpha 3", "a3@test.com", "Asia/Kolkata", teamAlpha));
        betaUser = userRepository.save(new User("Beta 1", "b1@test.com", "Asia/Kolkata", teamBeta));
    }

    @Test
    @DisplayName("Floor Capacity Test: Reject booking when floor capacity is exceeded")
    void testFloorCapacityExceeded() {
        LocalDate date = LocalDate.now().plusDays(1);
        LocalTime start = LocalTime.of(9, 0);
        LocalTime end = LocalTime.of(17, 0);

        bookingService.createBooking(new BookingRequest(u1.getId(), limitedFloor.getId(), date, start, end));
        bookingService.createBooking(new BookingRequest(betaUser.getId(), limitedFloor.getId(), date, start, end));

        // 3rd booking on floor with capacity 2 should throw QuotaExceededException
        QuotaExceededException ex = assertThrows(QuotaExceededException.class, () -> {
            bookingService.createBooking(new BookingRequest(u2.getId(), limitedFloor.getId(), date, start, end));
        });

        assertTrue(ex.getMessage().contains("Floor 1 capacity reached"));
    }

    @Test
    @DisplayName("Team Quota Test: Reject booking when team quota on a floor is exceeded")
    void testTeamQuotaExceeded() {
        // Floor capacity = 50, but Team Alpha quota on this floor = 2
        Floor bigFloor = floorRepository.save(new Floor(5, "Big Floor", 50, "Asia/Kolkata"));
        for (int i = 1; i <= 10; i++) {
            deskRepository.save(new Desk("BD-" + i, bigFloor, DeskType.HOT, (double) i, 1.0, null));
        }
        teamQuotaRepository.save(new TeamQuota(teamAlpha, bigFloor, 2));

        LocalDate date = LocalDate.now().plusDays(1);
        LocalTime start = LocalTime.of(9, 0);
        LocalTime end = LocalTime.of(17, 0);

        bookingService.createBooking(new BookingRequest(u1.getId(), bigFloor.getId(), date, start, end));
        bookingService.createBooking(new BookingRequest(u2.getId(), bigFloor.getId(), date, start, end));

        // 3rd booking from Team Alpha on this floor should fail due to quota
        QuotaExceededException ex = assertThrows(QuotaExceededException.class, () -> {
            bookingService.createBooking(new BookingRequest(u3.getId(), bigFloor.getId(), date, start, end));
        });

        assertTrue(ex.getMessage().contains("reached its maximum quota of 2 desks"));
    }

    @Test
    @DisplayName("Quota Recovery Test: Released/Cancelled booking immediately restores available quota")
    void testQuotaRestoredAfterCancellation() {
        Floor floor = floorRepository.save(new Floor(6, "Quota Recovery Floor", 10, "Asia/Kolkata"));
        for (int i = 1; i <= 5; i++) {
            deskRepository.save(new Desk("QD-" + i, floor, DeskType.HOT, (double) i, 1.0, null));
        }
        teamQuotaRepository.save(new TeamQuota(teamAlpha, floor, 1)); // Quota = 1

        LocalDate date = LocalDate.now().plusDays(1);
        LocalTime start = LocalTime.of(10, 0);
        LocalTime end = LocalTime.of(18, 0);

        // Alpha 1 books the only quota slot
        Booking b1 = bookingService.createBooking(new BookingRequest(u1.getId(), floor.getId(), date, start, end));

        // Alpha 2 is rejected
        assertThrows(QuotaExceededException.class, () -> {
            bookingService.createBooking(new BookingRequest(u2.getId(), floor.getId(), date, start, end));
        });

        // Alpha 1 cancels booking
        bookingService.cancelBooking(b1.getId(), u1.getId());

        // Alpha 2 can now successfully book within quota!
        Booking b2 = bookingService.createBooking(new BookingRequest(u2.getId(), floor.getId(), date, start, end));
        assertNotNull(b2);
        assertEquals(u2.getId(), b2.getUser().getId());
    }
}
