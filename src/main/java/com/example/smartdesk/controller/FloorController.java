package com.example.smartdesk.controller;

import com.example.smartdesk.dto.response.DeskResponse;
import com.example.smartdesk.dto.response.FloorResponse;
import com.example.smartdesk.entity.Floor;
import com.example.smartdesk.service.DeskService;
import com.example.smartdesk.service.FloorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/floors")
@Tag(name = "Floors", description = "Floor management and floor layout APIs")
public class FloorController {

    private final FloorService floorService;
    private final DeskService deskService;

    public FloorController(FloorService floorService, DeskService deskService) {
        this.floorService = floorService;
        this.deskService = deskService;
    }

    @GetMapping
    @Operation(summary = "Get all floors")
    public ResponseEntity<List<FloorResponse>> getAllFloors() {
        return ResponseEntity.ok(floorService.getAllFloors());
    }

    @GetMapping("/{floorId}")
    @Operation(summary = "Get floor details by ID")
    public ResponseEntity<FloorResponse> getFloorById(@PathVariable Long floorId) {
        Floor floor = floorService.getFloorById(floorId);
        return ResponseEntity.ok(FloorResponse.fromEntity(floor));
    }

    @GetMapping("/{floorId}/desks")
    @Operation(summary = "Get all desks on a floor (optionally with availability for a time window)")
    public ResponseEntity<List<DeskResponse>> getDesksOnFloor(
        @PathVariable Long floorId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime
    ) {
        return ResponseEntity.ok(deskService.getDesksByFloor(floorId, date, startTime, endTime));
    }
}
