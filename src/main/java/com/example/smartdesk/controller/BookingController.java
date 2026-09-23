package com.example.smartdesk.controller;

import com.example.smartdesk.dto.request.BookingRequest;
import com.example.smartdesk.dto.response.BookingResponse;
import com.example.smartdesk.entity.Booking;
import com.example.smartdesk.service.BookingService;
import com.example.smartdesk.service.CheckInService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@Tag(name = "Bookings", description = "Smart Desk Booking and Lifecycle APIs")
public class BookingController {

    private final BookingService bookingService;
    private final CheckInService checkInService;

    public BookingController(BookingService bookingService, CheckInService checkInService) {
        this.bookingService = bookingService;
        this.checkInService = checkInService;
    }

    @PostMapping
    @Operation(summary = "Book a desk", description = "Reserves a desk using smart neighbourhood placement or allocated fixed desk with race-condition protection.")
    public ResponseEntity<BookingResponse> createBooking(@Valid @RequestBody BookingRequest request) {
        Booking booking = bookingService.createBooking(request);
        String message = String.format("Desk %s booked successfully on Floor %d.",
            booking.getDesk().getDeskNumber(), booking.getFloor().getFloorNumber());
        return ResponseEntity.status(HttpStatus.CREATED).body(BookingResponse.fromEntity(booking, message));
    }

    @GetMapping("/{bookingId}")
    @Operation(summary = "Get booking details by ID")
    public ResponseEntity<BookingResponse> getBookingById(@PathVariable Long bookingId) {
        Booking booking = bookingService.getBookingById(bookingId);
        return ResponseEntity.ok(BookingResponse.fromEntity(booking));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get all bookings for a user")
    public ResponseEntity<List<BookingResponse>> getBookingsForUser(@PathVariable Long userId) {
        List<Booking> bookings = bookingService.getBookingsForUser(userId);
        List<BookingResponse> response = bookings.stream()
            .map(BookingResponse::fromEntity)
            .toList();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{bookingId}/cancel")
    @Operation(summary = "Cancel a booking before cutoff")
    public ResponseEntity<BookingResponse> cancelBooking(
        @PathVariable Long bookingId,
        @RequestParam(required = false) Long userId
    ) {
        Booking booking = bookingService.cancelBooking(bookingId, userId);
        return ResponseEntity.ok(BookingResponse.fromEntity(booking, "Booking cancelled successfully. Desk returned to pool."));
    }

    @PostMapping("/{bookingId}/check-in")
    @Operation(summary = "Check in for a confirmed booking")
    public ResponseEntity<BookingResponse> checkInBooking(@PathVariable Long bookingId) {
        Booking booking = checkInService.checkIn(bookingId);
        return ResponseEntity.ok(BookingResponse.fromEntity(booking, "Check-in successful. Enjoy your desk!"));
    }
}
