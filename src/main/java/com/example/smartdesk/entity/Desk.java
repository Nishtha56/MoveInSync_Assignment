package com.example.smartdesk.entity;

import com.example.smartdesk.entity.enums.DeskType;
import jakarta.persistence.*;
import java.util.Objects;

@Entity
@Table(
    name = "desks",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_desk_floor_number", columnNames = {"floor_id", "desk_number"})
    }
)
public class Desk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "desk_number", nullable = false, length = 50)
    private String deskNumber;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "floor_id", nullable = false)
    private Floor floor;

    @Enumerated(EnumType.STRING)
    @Column(name = "desk_type", nullable = false)
    private DeskType deskType = DeskType.HOT;

    @Column(name = "x_coordinate", nullable = false)
    private Double xCoordinate;

    @Column(name = "y_coordinate", nullable = false)
    private Double yCoordinate;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "fixed_user_id")
    private User fixedUser;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    public Desk() {
    }

    public Desk(Long id, String deskNumber, Floor floor, DeskType deskType, Double xCoordinate, Double yCoordinate, User fixedUser, Boolean isActive) {
        this.id = id;
        this.deskNumber = deskNumber;
        this.floor = floor;
        this.deskType = deskType;
        this.xCoordinate = xCoordinate;
        this.yCoordinate = yCoordinate;
        this.fixedUser = fixedUser;
        this.isActive = isActive != null ? isActive : true;
    }

    public Desk(String deskNumber, Floor floor, DeskType deskType, Double xCoordinate, Double yCoordinate, User fixedUser) {
        this.deskNumber = deskNumber;
        this.floor = floor;
        this.deskType = deskType;
        this.xCoordinate = xCoordinate;
        this.yCoordinate = yCoordinate;
        this.fixedUser = fixedUser;
        this.isActive = true;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDeskNumber() {
        return deskNumber;
    }

    public void setDeskNumber(String deskNumber) {
        this.deskNumber = deskNumber;
    }

    public Floor getFloor() {
        return floor;
    }

    public void setFloor(Floor floor) {
        this.floor = floor;
    }

    public DeskType getDeskType() {
        return deskType;
    }

    public void setDeskType(DeskType deskType) {
        this.deskType = deskType;
    }

    public Double getxCoordinate() {
        return xCoordinate;
    }

    public void setxCoordinate(Double xCoordinate) {
        this.xCoordinate = xCoordinate;
    }

    public Double getyCoordinate() {
        return yCoordinate;
    }

    public void setyCoordinate(Double yCoordinate) {
        this.yCoordinate = yCoordinate;
    }

    public User getFixedUser() {
        return fixedUser;
    }

    public void setFixedUser(User fixedUser) {
        this.fixedUser = fixedUser;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public double calculateDistanceTo(Desk other) {
        if (other == null) return Double.MAX_VALUE;
        double dx = this.xCoordinate - other.xCoordinate;
        double dy = this.yCoordinate - other.yCoordinate;
        return Math.sqrt(dx * dx + dy * dy);
    }

    public double calculateDistanceTo(double x, double y) {
        double dx = this.xCoordinate - x;
        double dy = this.yCoordinate - y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Desk desk = (Desk) o;
        return Objects.equals(id, desk.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
