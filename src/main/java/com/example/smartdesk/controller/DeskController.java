package com.example.smartdesk.controller;

import com.example.smartdesk.dto.response.DeskResponse;
import com.example.smartdesk.entity.Desk;
import com.example.smartdesk.service.DeskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/desks")
@Tag(name = "Desks", description = "Desk queries and availability APIs")
public class DeskController {

    private final DeskService deskService;

    public DeskController(DeskService deskService) {
        this.deskService = deskService;
    }

    @GetMapping("/{deskId}")
    @Operation(summary = "Get desk details by ID")
    public ResponseEntity<DeskResponse> getDeskById(@PathVariable Long deskId) {
        Desk desk = deskService.getDeskById(deskId);
        return ResponseEntity.ok(DeskResponse.fromEntity(desk));
    }

    @GetMapping("/available")
    @Operation(summary = "Get all available desks for a floor, date, and time window")
    public ResponseEntity<List<DeskResponse>> getAvailableDesks(
        @RequestParam Long floorId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime
    ) {
        return ResponseEntity.ok(deskService.getAvailableDesks(floorId, date, startTime, endTime));
    }
}
