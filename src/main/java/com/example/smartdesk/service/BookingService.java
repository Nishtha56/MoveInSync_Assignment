package com.example.smartdesk.service;

import com.example.smartdesk.dto.request.BookingRequest;
import com.example.smartdesk.entity.Booking;
import com.example.smartdesk.entity.Desk;
import com.example.smartdesk.entity.Floor;
import com.example.smartdesk.entity.User;
import com.example.smartdesk.entity.enums.BookingStatus;
import com.example.smartdesk.entity.enums.DeskType;
import com.example.smartdesk.exception.*;
import com.example.smartdesk.repository.BookingRepository;
import com.example.smartdesk.repository.DeskRepository;
import com.example.smartdesk.repository.FloorRepository;
import com.example.smartdesk.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.List;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final DeskRepository deskRepository;
    private final FloorRepository floorRepository;
    private final UserRepository userRepository;
    private final DeskAssignmentService deskAssignmentService;
    private final QuotaService quotaService;
    private final Clock clock;
    private final int cancellationCutoffHours;

    public BookingService(
        BookingRepository bookingRepository,
        DeskRepository deskRepository,
        FloorRepository floorRepository,
        UserRepository userRepository,
        DeskAssignmentService deskAssignmentService,
        QuotaService quotaService,
        Clock clock,
        @Value("${smartdesk.booking.cancellation-cutoff-hours:2}") int cancellationCutoffHours
    ) {
        this.bookingRepository = bookingRepository;
        this.deskRepository = deskRepository;
        this.floorRepository = floorRepository;
        this.userRepository = userRepository;
        this.deskAssignmentService = deskAssignmentService;
        this.quotaService = quotaService;
        this.clock = clock;
        this.cancellationCutoffHours = cancellationCutoffHours;
    }

    /**
     * Atomically creates a smart desk booking with multi-layered concurrency protection:
     * 1. Validates request parameters & time intervals.
     * 2. Checks user duplication.
     * 3. Enforces floor capacity & team quotas.
     * 4. Determines candidate desk (smart neighbourhood placement for HOT desks or ownership validation for FIXED desks).
     * 5. Locks candidate desk (PESSIMISTIC_WRITE) and re-verifies availability.
     * 6. Persists booking atomically.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Booking createBooking(BookingRequest request) {
        // 1. Time Window Validation
        if (request.getStartTime() == null || request.getEndTime() == null) {
            throw new InvalidBookingException("Start time and end time are required");
        }
        if (!request.getStartTime().isBefore(request.getEndTime())) {
            throw new InvalidBookingException("Start time (" + request.getStartTime() + ") must be strictly before end time (" + request.getEndTime() + ")");
        }

        LocalDate today = LocalDate.now(clock);
        if (request.getBookingDate().isBefore(today)) {
            throw new InvalidBookingException("Cannot book a desk in the past");
        }

        // 2. Validate User & Floor
        User user = userRepository.findById(request.getUserId())
            .orElseThrow(() -> new ResourceNotFoundException("User with id " + request.getUserId() + " not found"));

        Floor floor = floorRepository.findById(request.getFloorId())
            .orElseThrow(() -> new ResourceNotFoundException("Floor with id " + request.getFloorId() + " not found"));

        // 3. Prevent duplicate active booking by same user at overlapping time
        List<BookingStatus> activeStatuses = List.of(BookingStatus.CONFIRMED, BookingStatus.CHECKED_IN);
        List<Booking> userOverlaps = bookingRepository.findOverlappingBookingsForUser(
            user.getId(), request.getBookingDate(), request.getStartTime(), request.getEndTime(), activeStatuses
        );
        if (!userOverlaps.isEmpty()) {
            throw new BookingConflictException("User already has an active booking on " + request.getBookingDate() + " during " + request.getStartTime() + " - " + request.getEndTime());
        }

        // 4. Validate Floor Capacity & Team Quota
        quotaService.validateQuotas(user, floor, request.getBookingDate(), request.getStartTime(), request.getEndTime());

        // 5. Select Optimal Desk using Neighbourhood Placement Algorithm
        Desk candidateDesk = deskAssignmentService.findOptimalDesk(
            user, floor, request.getBookingDate(), request.getStartTime(), request.getEndTime(), request.getDeskId()
        );

        // 6. Pessimistic Lock on Desk to guarantee No Double-Booking under high concurrency
        Desk lockedDesk = deskRepository.findByIdWithLock(candidateDesk.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Desk no longer available"));

        // 7. Double check locked desk availability in current transaction
        List<Booking> deskOverlaps = bookingRepository.findOverlappingBookingsForDesk(
            lockedDesk.getId(), request.getBookingDate(), request.getStartTime(), request.getEndTime(), activeStatuses
        );
        if (!deskOverlaps.isEmpty()) {
            throw new BookingConflictException("Desk " + lockedDesk.getDeskNumber() + " was just booked by another employee. Please retry.");
        }

        // 8. Create and persist confirmed booking
        Booking booking = new Booking(
            user,
            lockedDesk,
            floor,
            request.getBookingDate(),
            request.getStartTime(),
            request.getEndTime()
        );
        booking.setStatus(BookingStatus.CONFIRMED);

        return bookingRepository.save(booking);
    }

    /**
     * Cancels an active booking before the cancellation cutoff deadline.
     * Enforces strict timezone-aware boundary checks.
     */
    @Transactional
    public Booking cancelBooking(Long bookingId, Long userId) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new ResourceNotFoundException("Booking with id " + bookingId + " not found"));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            return booking; // Idempotent
        }

        if (booking.getStatus() == BookingStatus.RELEASED) {
            throw new InvalidBookingException("Cannot cancel a booking that has already been released");
        }

        if (userId != null && !booking.getUser().getId().equals(userId)) {
            throw new InvalidBookingException("User " + userId + " is not authorized to cancel booking " + bookingId);
        }

        // Evaluate Timezone-aware Cutoff
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
        ZonedDateTime cutoffInstant = bookingStartZoned.minusHours(cancellationCutoffHours);

        // Exact boundary rule: Cancellation rejected at or after cutoff
        if (!nowZoned.isBefore(cutoffInstant)) {
            throw new CutoffExceededException(
                String.format("Cancellation cutoff deadline has passed (%s in %s). Cancellations must be made at least %d hours before start time.",
                    cutoffInstant.toLocalTime(), zoneId, cancellationCutoffHours)
            );
        }

        booking.setStatus(BookingStatus.CANCELLED);
        return bookingRepository.save(booking);
    }

    @Transactional(readOnly = true)
    public Booking getBookingById(Long id) {
        return bookingRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Booking with id " + id + " not found"));
    }

    @Transactional(readOnly = true)
    public List<Booking> getBookingsForUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User with id " + userId + " not found");
        }
        return bookingRepository.findByUserIdOrderByBookingDateDescStartTimeDesc(userId);
    }
}
