package com.example.smartdesk.service;

import com.example.smartdesk.dto.response.FloorResponse;
import com.example.smartdesk.entity.Floor;
import com.example.smartdesk.exception.ResourceNotFoundException;
import com.example.smartdesk.repository.FloorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FloorService {

    private final FloorRepository floorRepository;

    public FloorService(FloorRepository floorRepository) {
        this.floorRepository = floorRepository;
    }

    @Transactional(readOnly = true)
    public List<FloorResponse> getAllFloors() {
        return floorRepository.findAll().stream()
            .map(FloorResponse::fromEntity)
            .toList();
    }

    @Transactional(readOnly = true)
    public Floor getFloorById(Long id) {
        return floorRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Floor with id " + id + " not found"));
    }
}
