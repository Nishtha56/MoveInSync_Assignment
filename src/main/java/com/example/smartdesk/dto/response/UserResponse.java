package com.example.smartdesk.dto.response;

import com.example.smartdesk.entity.User;

public class UserResponse {

    private Long id;
    private String name;
    private String email;
    private String timezone;
    private Long teamId;
    private String teamName;
    private String teamCode;

    public UserResponse() {
    }

    public static UserResponse fromEntity(User user) {
        UserResponse resp = new UserResponse();
        resp.setId(user.getId());
        resp.setName(user.getName());
        resp.setEmail(user.getEmail());
        resp.setTimezone(user.getTimezone());
        if (user.getTeam() != null) {
            resp.setTeamId(user.getTeam().getId());
            resp.setTeamName(user.getTeam().getName());
            resp.setTeamCode(user.getTeam().getCode());
        }
        return resp;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
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

    public String getTeamCode() {
        return teamCode;
    }

    public void setTeamCode(String teamCode) {
        this.teamCode = teamCode;
    }
}
