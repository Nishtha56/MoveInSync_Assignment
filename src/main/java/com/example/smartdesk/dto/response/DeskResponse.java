package com.example.smartdesk.dto.response;

import com.example.smartdesk.entity.Desk;
import com.example.smartdesk.entity.enums.DeskType;

public class DeskResponse {

    private Long id;
    private String deskNumber;
    private Long floorId;
    private Integer floorNumber;
    private DeskType deskType;
    private Double xCoordinate;
    private Double yCoordinate;
    private Long fixedUserId;
    private String fixedUserName;
    private Boolean isActive;
    private Boolean isAvailable;

    public DeskResponse() {
    }

    public static DeskResponse fromEntity(Desk desk, Boolean isAvailable) {
        DeskResponse resp = new DeskResponse();
        resp.setId(desk.getId());
        resp.setDeskNumber(desk.getDeskNumber());
        if (desk.getFloor() != null) {
            resp.setFloorId(desk.getFloor().getId());
            resp.setFloorNumber(desk.getFloor().getFloorNumber());
        }
        resp.setDeskType(desk.getDeskType());
        resp.setXCoordinate(desk.getxCoordinate());
        resp.setYCoordinate(desk.getyCoordinate());
        if (desk.getFixedUser() != null) {
            resp.setFixedUserId(desk.getFixedUser().getId());
            resp.setFixedUserName(desk.getFixedUser().getName());
        }
        resp.setIsActive(desk.getIsActive());
        resp.setIsAvailable(isAvailable);
        return resp;
    }

    public static DeskResponse fromEntity(Desk desk) {
        return fromEntity(desk, null);
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

    public DeskType getDeskType() {
        return deskType;
    }

    public void setDeskType(DeskType deskType) {
        this.deskType = deskType;
    }

    public Double getXCoordinate() {
        return xCoordinate;
    }

    public void setXCoordinate(Double xCoordinate) {
        this.xCoordinate = xCoordinate;
    }

    public Double getYCoordinate() {
        return yCoordinate;
    }

    public void setYCoordinate(Double yCoordinate) {
        this.yCoordinate = yCoordinate;
    }

    public Long getFixedUserId() {
        return fixedUserId;
    }

    public void setFixedUserId(Long fixedUserId) {
        this.fixedUserId = fixedUserId;
    }

    public String getFixedUserName() {
        return fixedUserName;
    }

    public void setFixedUserName(String fixedUserName) {
        this.fixedUserName = fixedUserName;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Boolean getIsAvailable() {
        return isAvailable;
    }

    public void setIsAvailable(Boolean isAvailable) {
        this.isAvailable = isAvailable;
    }
}
