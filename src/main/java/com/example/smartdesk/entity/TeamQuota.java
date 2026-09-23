package com.example.smartdesk.entity;

import jakarta.persistence.*;
import java.util.Objects;

@Entity
@Table(
    name = "team_quotas",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_team_floor_quota", columnNames = {"team_id", "floor_id"})
    }
)
public class TeamQuota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "floor_id", nullable = false)
    private Floor floor;

    @Column(name = "max_desks", nullable = false)
    private Integer maxDesks;

    public TeamQuota() {
    }

    public TeamQuota(Long id, Team team, Floor floor, Integer maxDesks) {
        this.id = id;
        this.team = team;
        this.floor = floor;
        this.maxDesks = maxDesks;
    }

    public TeamQuota(Team team, Floor floor, Integer maxDesks) {
        this.team = team;
        this.floor = floor;
        this.maxDesks = maxDesks;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

    public Floor getFloor() {
        return floor;
    }

    public void setFloor(Floor floor) {
        this.floor = floor;
    }

    public Integer getMaxDesks() {
        return maxDesks;
    }

    public void setMaxDesks(Integer maxDesks) {
        this.maxDesks = maxDesks;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TeamQuota teamQuota = (TeamQuota) o;
        return Objects.equals(id, teamQuota.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
