package com.example.smartdesk.dto.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

public class BookingRequest {

    @NotNull(message = "userId is required")
    private Long userId;

    @NotNull(message = "floorId is required")
    private Long floorId;

    @NotNull(message = "bookingDate is required")
    @FutureOrPresent(message = "bookingDate cannot be in the past")
    private LocalDate bookingDate;

    @NotNull(message = "startTime is required")
    private LocalTime startTime;

    @NotNull(message = "endTime is required")
    private LocalTime endTime;

    /**
     * Optional: If null, the smart neighbourhood placement algorithm automatically selects
     * the best available desk near teammates. If specified, system attempts to book the specified desk.
     */
    private Long deskId;

    public BookingRequest() {
    }

    public BookingRequest(Long userId, Long floorId, LocalDate bookingDate, LocalTime startTime, LocalTime endTime, Long deskId) {
        this.userId = userId;
        this.floorId = floorId;
        this.bookingDate = bookingDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.deskId = deskId;
    }

    public BookingRequest(Long userId, Long floorId, LocalDate bookingDate, LocalTime startTime, LocalTime endTime) {
        this(userId, floorId, bookingDate, startTime, endTime, null);
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getFloorId() {
        return floorId;
    }

    public void setFloorId(Long floorId) {
        this.floorId = floorId;
    }

    public LocalDate getBookingDate() {
        return bookingDate;
    }

    public void setBookingDate(LocalDate bookingDate) {
        this.bookingDate = bookingDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public Long getDeskId() {
        return deskId;
    }

    public void setDeskId(Long deskId) {
        this.deskId = deskId;
    }
}
