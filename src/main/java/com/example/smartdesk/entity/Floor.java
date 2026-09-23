package com.example.smartdesk.entity;

import jakarta.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "floors")
public class Floor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Integer floorNumber;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer capacity = 60;

    @Column(nullable = false)
    private String timezone = "Asia/Kolkata";

    public Floor() {
    }

    public Floor(Long id, Integer floorNumber, String name, Integer capacity, String timezone) {
        this.id = id;
        this.floorNumber = floorNumber;
        this.name = name;
        this.capacity = capacity;
        this.timezone = timezone != null ? timezone : "Asia/Kolkata";
    }

    public Floor(Integer floorNumber, String name, Integer capacity, String timezone) {
        this.floorNumber = floorNumber;
        this.name = name;
        this.capacity = capacity;
        this.timezone = timezone != null ? timezone : "Asia/Kolkata";
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getFloorNumber() {
        return floorNumber;
    }

    public void setFloorNumber(Integer floorNumber) {
        this.floorNumber = floorNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Floor floor = (Floor) o;
        return Objects.equals(id, floor.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
