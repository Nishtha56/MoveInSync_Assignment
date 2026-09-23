package com.example.smartdesk.dto.response;

import com.example.smartdesk.entity.Team;

public class TeamResponse {

    private Long id;
    private String name;
    private String code;
    private String description;

    public TeamResponse() {
    }

    public static TeamResponse fromEntity(Team team) {
        TeamResponse resp = new TeamResponse();
        resp.setId(team.getId());
        resp.setName(team.getName());
        resp.setCode(team.getCode());
        resp.setDescription(team.getDescription());
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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
