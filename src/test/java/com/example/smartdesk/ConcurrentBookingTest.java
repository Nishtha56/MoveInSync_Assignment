package com.example.smartdesk;

import com.example.smartdesk.dto.request.BookingRequest;
import com.example.smartdesk.entity.*;
import com.example.smartdesk.entity.enums.BookingStatus;
import com.example.smartdesk.entity.enums.DeskType;
import com.example.smartdesk.exception.BookingConflictException;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class ConcurrentBookingTest {

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

    private Floor testFloor;
    private Desk lastAvailableDesk;
    private User employeeA;
    private User employeeB;

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
        teamQuotaRepository.deleteAll();
        deskRepository.deleteAll();
        userRepository.deleteAll();
        floorRepository.deleteAll();
        teamRepository.deleteAll();

        Team team = teamRepository.save(new Team("Concurrency Team", "CONC", "Testing Team"));
        testFloor = floorRepository.save(new Floor(10, "Concurrency Test Floor", 10, "Asia/Kolkata"));

        // Create exactly ONE available hot desk on this floor
        lastAvailableDesk = deskRepository.save(new Desk("D-999", testFloor, DeskType.HOT, 10.0, 10.0, null));

        employeeA = userRepository.save(new User("Employee A", "empA@company.com", "Asia/Kolkata", team));
        employeeB = userRepository.save(new User("Employee B", "empB@company.com", "Asia/Kolkata", team));
    }

    @Test
    @DisplayName("MANDATORY CONCURRENCY TEST: Two employees simultaneous booking for same desk/date/time -> Exactly 1 Winner, 1 Conflict")
    void testSimultaneousBookingSameDeskResultsInExactlyOneWinner() throws Exception {
        LocalDate bookingDate = LocalDate.now().plusDays(2);
        LocalTime startTime = LocalTime.of(9, 0);
        LocalTime endTime = LocalTime.of(17, 0);

        int numberOfThreads = 2;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(numberOfThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        List<Exception> errors = new CopyOnWriteArrayList<>();

        // Thread 1: Employee A
        executorService.submit(() -> {
            try {
                startLatch.await(); // Synchronize simultaneous release
                BookingRequest reqA = new BookingRequest(
                    employeeA.getId(), testFloor.getId(), bookingDate, startTime, endTime, lastAvailableDesk.getId()
                );
                bookingService.createBooking(reqA);
                successCount.incrementAndGet();
            } catch (BookingConflictException e) {
                conflictCount.incrementAndGet();
            } catch (Exception e) {
                errors.add(e);
            } finally {
                finishLatch.countDown();
            }
        });

        // Thread 2: Employee B
        executorService.submit(() -> {
            try {
                startLatch.await(); // Synchronize simultaneous release
                BookingRequest reqB = new BookingRequest(
                    employeeB.getId(), testFloor.getId(), bookingDate, startTime, endTime, lastAvailableDesk.getId()
                );
                bookingService.createBooking(reqB);
                successCount.incrementAndGet();
            } catch (BookingConflictException e) {
                conflictCount.incrementAndGet();
            } catch (Exception e) {
                errors.add(e);
            } finally {
                finishLatch.countDown();
            }
        });

        // Trigger simultaneous execution
        startLatch.countDown();
        boolean finished = finishLatch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();

        assertTrue(finished, "Concurrent booking threads should finish within timeout");
        assertTrue(errors.isEmpty(), "Unexpected errors encountered: " + errors);

        // Assert: EXACTLY 1 booking succeeded and EXACTLY 1 received Conflict
        assertEquals(1, successCount.get(), "Exactly ONE employee booking request must succeed");
        assertEquals(1, conflictCount.get(), "Exactly ONE employee booking request must receive Conflict");

        // Assert: Database contains exactly ONE valid booking for that desk/time
        List<Booking> activeBookings = bookingRepository.findOverlappingBookingsForDesk(
            lastAvailableDesk.getId(), bookingDate, startTime, endTime,
            List.of(BookingStatus.CONFIRMED, BookingStatus.CHECKED_IN)
        );
        assertEquals(1, activeBookings.size(), "Database must contain exactly ONE active booking for the desk");
    }

    @Test
    @DisplayName("HIGH-CONCURRENCY STRESS TEST: 10 employees concurrently requesting auto-assignment on a floor with only 1 hot desk")
    void testMultiThreadedContentionForSingleAvailableHotDesk() throws Exception {
        LocalDate bookingDate = LocalDate.now().plusDays(3);
        LocalTime startTime = LocalTime.of(10, 0);
        LocalTime endTime = LocalTime.of(18, 0);

        int employeeCount = 10;
        List<User> employees = new ArrayList<>();
        Team team = teamRepository.findAll().get(0);
        for (int i = 1; i <= employeeCount; i++) {
            employees.add(userRepository.save(new User("Stress Emp " + i, "stressemp" + i + "@company.com", "Asia/Kolkata", team)));
        }

        ExecutorService executor = Executors.newFixedThreadPool(employeeCount);
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(employeeCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        for (User emp : employees) {
            executor.submit(() -> {
                try {
                    startSignal.await();
                    BookingRequest request = new BookingRequest(
                        emp.getId(), testFloor.getId(), bookingDate, startTime, endTime
                    );
                    bookingService.createBooking(request);
                    successCount.incrementAndGet();
                } catch (BookingConflictException e) {
                    conflictCount.incrementAndGet();
                } catch (Exception ignored) {
                } finally {
                    doneSignal.countDown();
                }
            });
        }

        startSignal.countDown();
        doneSignal.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertEquals(1, successCount.get(), "Exactly ONE out of 10 concurrent requests must win the single hot desk");
        assertEquals(9, conflictCount.get(), "Exactly 9 losing concurrent requests must receive Conflict");

        List<Booking> allActiveBookings = bookingRepository.findOverlappingBookingsForDesk(
            lastAvailableDesk.getId(), bookingDate, startTime, endTime,
            List.of(BookingStatus.CONFIRMED, BookingStatus.CHECKED_IN)
        );
        assertEquals(1, allActiveBookings.size(), "Database must contain exactly ONE active booking");
    }
}
