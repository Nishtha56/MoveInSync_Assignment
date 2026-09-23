package com.example.smartdesk.dto.response;

import com.example.smartdesk.entity.Floor;

public class FloorResponse {

    private Long id;
    private Integer floorNumber;
    private String name;
    private Integer capacity;
    private String timezone;

    public FloorResponse() {
    }

    public static FloorResponse fromEntity(Floor floor) {
        FloorResponse resp = new FloorResponse();
        resp.setId(floor.getId());
        resp.setFloorNumber(floor.getFloorNumber());
        resp.setName(floor.getName());
        resp.setCapacity(floor.getCapacity());
        resp.setTimezone(floor.getTimezone());
        return resp;
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
}
