package com.example.smartdesk.dto.response;

import com.example.smartdesk.entity.Booking;
import com.example.smartdesk.entity.enums.BookingStatus;
import com.example.smartdesk.entity.enums.DeskType;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

public class BookingResponse {

    private Long bookingId;
    private Long userId;
    private String userName;
    private Long teamId;
    private String teamName;
    private Long deskId;
    private String deskNumber;
    private DeskType deskType;
    private Double deskX;
    private Double deskY;
    private Long floorId;
    private Integer floorNumber;
    private String floorName;
    private LocalDate bookingDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private BookingStatus status;
    private Instant createdAt;
    private Instant checkedInAt;
    private String message;

    public BookingResponse() {
    }

    public static BookingResponse fromEntity(Booking booking, String message) {
        BookingResponse resp = new BookingResponse();
        resp.setBookingId(booking.getId());
        if (booking.getUser() != null) {
            resp.setUserId(booking.getUser().getId());
            resp.setUserName(booking.getUser().getName());
            if (booking.getUser().getTeam() != null) {
                resp.setTeamId(booking.getUser().getTeam().getId());
                resp.setTeamName(booking.getUser().getTeam().getName());
            }
        }
        if (booking.getDesk() != null) {
            resp.setDeskId(booking.getDesk().getId());
            resp.setDeskNumber(booking.getDesk().getDeskNumber());
            resp.setDeskType(booking.getDesk().getDeskType());
            resp.setDeskX(booking.getDesk().getxCoordinate());
            resp.setDeskY(booking.getDesk().getyCoordinate());
        }
        if (booking.getFloor() != null) {
            resp.setFloorId(booking.getFloor().getId());
            resp.setFloorNumber(booking.getFloor().getFloorNumber());
            resp.setFloorName(booking.getFloor().getName());
        }
        resp.setBookingDate(booking.getBookingDate());
        resp.setStartTime(booking.getStartTime());
        resp.setEndTime(booking.getEndTime());
        resp.setStatus(booking.getStatus());
        resp.setCreatedAt(booking.getCreatedAt());
        resp.setCheckedInAt(booking.getCheckedInAt());
        resp.setMessage(message);
        return resp;
    }

    public static BookingResponse fromEntity(Booking booking) {
        return fromEntity(booking, null);
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public Long getDeskId() {
        return deskId;
    }

    public void setDeskId(Long deskId) {
        this.deskId = deskId;
    }

    public String getDeskNumber() {
        return deskNumber;
    }

    public void setDeskNumber(String deskNumber) {
        this.deskNumber = deskNumber;
    }

    public DeskType getDeskType() {
        return deskType;
    }

    public void setDeskType(DeskType deskType) {
        this.deskType = deskType;
    }

    public Double getDeskX() {
        return deskX;
    }

    public void setDeskX(Double deskX) {
        this.deskX = deskX;
    }

    public Double getDeskY() {
        return deskY;
    }

    public void setDeskY(Double deskY) {
        this.deskY = deskY;
    }

    public Long getFloorId() {
        return floorId;
    }

    public void setFloorId(Long floorId) {
        this.floorId = floorId;
    }

    public Integer getFloorNumber() {
        return floorNumber;
    }

    public void setFloorNumber(Integer floorNumber) {
        this.floorNumber = floorNumber;
    }

    public String getFloorName() {
        return floorName;
    }

    public void setFloorName(String floorName) {
        this.floorName = floorName;
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

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getCheckedInAt() {
        return checkedInAt;
    }

    public void setCheckedInAt(Instant checkedInAt) {
        this.checkedInAt = checkedInAt;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
