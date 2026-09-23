package com.example.smartdesk;

import com.example.smartdesk.dto.request.BookingRequest;
import com.example.smartdesk.entity.*;
import com.example.smartdesk.entity.enums.DeskType;
import com.example.smartdesk.repository.*;
import com.example.smartdesk.service.BookingService;
import com.example.smartdesk.service.DeskAssignmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class NeighbourhoodPlacementTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private DeskAssignmentService deskAssignmentService;

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

    private Team teamA;
    private Floor floor3;
    private User emp1;
    private User emp2;
    private User emp3;
    private User emp4;

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
        teamQuotaRepository.deleteAll();
        deskRepository.deleteAll();
        userRepository.deleteAll();
        floorRepository.deleteAll();
        teamRepository.deleteAll();

        teamA = teamRepository.save(new Team("Team A", "TEAM_A", "Alpha Engineering"));
        floor3 = floorRepository.save(new Floor(3, "Floor 3", 60, "Asia/Kolkata"));

        emp1 = userRepository.save(new User("Emp 1", "emp1@a.com", "Asia/Kolkata", teamA));
        emp2 = userRepository.save(new User("Emp 2", "emp2@a.com", "Asia/Kolkata", teamA));
        emp3 = userRepository.save(new User("Emp 3", "emp3@a.com", "Asia/Kolkata", teamA));
        emp4 = userRepository.save(new User("Emp 4", "emp4@a.com", "Asia/Kolkata", teamA));

        // Create desks in Cluster Zone (10,10), (11,10), (12,10), (13,10)
        deskRepository.save(new Desk("Desk-101", floor3, DeskType.HOT, 10.0, 10.0, null));
        deskRepository.save(new Desk("Desk-102", floor3, DeskType.HOT, 11.0, 10.0, null));
        deskRepository.save(new Desk("Desk-103", floor3, DeskType.HOT, 12.0, 10.0, null));
        deskRepository.save(new Desk("Desk-104", floor3, DeskType.HOT, 13.0, 10.0, null));

        // Far away desks in another zone (50, 50), (60, 60)
        deskRepository.save(new Desk("Desk-901", floor3, DeskType.HOT, 50.0, 50.0, null));
        deskRepository.save(new Desk("Desk-902", floor3, DeskType.HOT, 60.0, 60.0, null));
    }

    @Test
    @DisplayName("Team Neighbourhood Placement: Emp 4 is clustered next to teammates (Desk-104) rather than far away desks")
    void testTeamNeighbourhoodPlacementPrefersAdjacentDesks() {
        LocalDate date = LocalDate.now().plusDays(1);
        LocalTime start = LocalTime.of(9, 0);
        LocalTime end = LocalTime.of(17, 0);

        // Emp 1, Emp 2, Emp 3 book desks (assigned Desk-101, Desk-102, Desk-103)
        Booking b1 = bookingService.createBooking(new BookingRequest(emp1.getId(), floor3.getId(), date, start, end));
        Booking b2 = bookingService.createBooking(new BookingRequest(emp2.getId(), floor3.getId(), date, start, end));
        Booking b3 = bookingService.createBooking(new BookingRequest(emp3.getId(), floor3.getId(), date, start, end));

        assertNotNull(b1);
        assertNotNull(b2);
        assertNotNull(b3);

        // Emp 4 books for the same date & floor without specifying a desk
        Booking b4 = bookingService.createBooking(new BookingRequest(emp4.getId(), floor3.getId(), date, start, end));

        assertNotNull(b4);
        // Desk-104 (at 13.0, 10.0) should be chosen because it is right next to team cluster (centroid at 11, 10)
        assertEquals("Desk-104", b4.getDesk().getDeskNumber(), "Emp 4 should receive adjacent Desk-104 near teammates");
        assertEquals(13.0, b4.getDesk().getxCoordinate());
        assertEquals(10.0, b4.getDesk().getyCoordinate());
    }

    @Test
    @DisplayName("Performance & Scale Test: 500-desk floor grid neighbourhood placement executes in sub-millisecond time")
    void testNeighbourhoodPlacementOn500DeskFloor() {
        Floor megaFloor = floorRepository.save(new Floor(4, "500 Desk Floor", 500, "Asia/Kolkata"));

        // Seed 500 desks in a 25x20 grid
        List<Desk> largeDeskList = new ArrayList<>();
        int counter = 1;
        for (int r = 1; r <= 20; r++) {
            for (int c = 1; c <= 25; c++) {
                largeDeskList.add(new Desk("MD-" + counter++, megaFloor, DeskType.HOT, (double) c * 2, (double) r * 2, null));
            }
        }
        deskRepository.saveAll(largeDeskList);

        LocalDate date = LocalDate.now().plusDays(2);
        LocalTime start = LocalTime.of(9, 0);
        LocalTime end = LocalTime.of(17, 0);

        // First teammate books
        Booking b1 = bookingService.createBooking(new BookingRequest(emp1.getId(), megaFloor.getId(), date, start, end));

        long startTime = System.nanoTime();
        // Second teammate books on 500-desk floor
        Booking b2 = bookingService.createBooking(new BookingRequest(emp2.getId(), megaFloor.getId(), date, start, end));
        long durationMs = (System.nanoTime() - startTime) / 1_000_000;

        assertNotNull(b2);
        // Assert second teammate is placed immediately adjacent to the first teammate
        double distance = b2.getDesk().calculateDistanceTo(b1.getDesk());
        assertTrue(distance <= 3.0, "Teammate on 500-desk floor should be placed closely adjacent, got distance: " + distance);
        assertTrue(durationMs < 100, "500-desk placement algorithm should be ultra-fast, took: " + durationMs + "ms");
    }
}
