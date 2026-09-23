package com.example.smartdesk;

import com.example.smartdesk.dto.request.BookingRequest;
import com.example.smartdesk.entity.*;
import com.example.smartdesk.entity.enums.BookingStatus;
import com.example.smartdesk.entity.enums.DeskType;
import com.example.smartdesk.exception.InvalidBookingException;
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
public class FixedVsHotDeskTest {

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

    private User alice;
    private User bob;
    private Floor floor;
    private Desk aliceFixedDesk;
    private Desk hotDesk;

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
        teamQuotaRepository.deleteAll();
        deskRepository.deleteAll();
        userRepository.deleteAll();
        floorRepository.deleteAll();
        teamRepository.deleteAll();

        Team team = teamRepository.save(new Team("Desk Type Team", "DT", "Testing"));
        floor = floorRepository.save(new Floor(9, "Floor 9", 20, "Asia/Kolkata"));

        alice = userRepository.save(new User("Alice", "alice@dt.com", "Asia/Kolkata", team));
        bob = userRepository.save(new User("Bob", "bob@dt.com", "Asia/Kolkata", team));

        // Fixed desk specifically assigned to Alice
        aliceFixedDesk = deskRepository.save(new Desk("FIXED-01", floor, DeskType.FIXED, 1.0, 1.0, alice));

        // Hot desk for general pool
        hotDesk = deskRepository.save(new Desk("HOT-01", floor, DeskType.HOT, 2.0, 2.0, null));
    }

    @Test
    @DisplayName("Fixed Desk Ownership: Bob cannot book Alice's fixed desk")
    void testNonOwnerCannotBookFixedDesk() {
        LocalDate date = LocalDate.now().plusDays(1);
        LocalTime start = LocalTime.of(9, 0);
        LocalTime end = LocalTime.of(17, 0);

        // Bob attempts to book Alice's fixed desk
        InvalidBookingException ex = assertThrows(InvalidBookingException.class, () -> {
            bookingService.createBooking(new BookingRequest(bob.getId(), floor.getId(), date, start, end, aliceFixedDesk.getId()));
        });

        assertTrue(ex.getMessage().contains("fixed desk reserved for another employee"));
    }

    @Test
    @DisplayName("Fixed Desk Auto-Allocation: Alice automatically gets her assigned fixed desk when booking on her floor")
    void testOwnerGetsFixedDesk() {
        LocalDate date = LocalDate.now().plusDays(1);
        LocalTime start = LocalTime.of(9, 0);
        LocalTime end = LocalTime.of(17, 0);

        // Alice books without specifying deskId
        Booking booking = bookingService.createBooking(new BookingRequest(alice.getId(), floor.getId(), date, start, end));

        assertNotNull(booking);
        assertEquals(aliceFixedDesk.getId(), booking.getDesk().getId(), "Alice should be assigned her fixed desk");
        assertEquals(DeskType.FIXED, booking.getDesk().getDeskType());
    }

    @Test
    @DisplayName("Hot Desk Pool: Bob can successfully book hot desks from the shared pool")
    void testHotDeskBooking() {
        LocalDate date = LocalDate.now().plusDays(1);
        LocalTime start = LocalTime.of(9, 0);
        LocalTime end = LocalTime.of(17, 0);

        // Bob books
        Booking booking = bookingService.createBooking(new BookingRequest(bob.getId(), floor.getId(), date, start, end));

        assertNotNull(booking);
        assertEquals(hotDesk.getId(), booking.getDesk().getId());
        assertEquals(DeskType.HOT, booking.getDesk().getDeskType());
    }
}
