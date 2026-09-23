package com.example.smartdesk.service;

import com.example.smartdesk.dto.response.TeamResponse;
import com.example.smartdesk.entity.Team;
import com.example.smartdesk.exception.ResourceNotFoundException;
import com.example.smartdesk.repository.TeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TeamService {

    private final TeamRepository teamRepository;

    public TeamService(TeamRepository teamRepository) {
        this.teamRepository = teamRepository;
    }

    @Transactional(readOnly = true)
    public List<TeamResponse> getAllTeams() {
        return teamRepository.findAll().stream()
            .map(TeamResponse::fromEntity)
            .toList();
    }

    @Transactional(readOnly = true)
    public Team getTeamById(Long id) {
        return teamRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Team with id " + id + " not found"));
    }
}
