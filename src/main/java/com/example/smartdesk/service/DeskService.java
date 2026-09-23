package com.example.smartdesk.service;

import com.example.smartdesk.dto.response.DeskResponse;
import com.example.smartdesk.entity.Desk;
import com.example.smartdesk.entity.enums.BookingStatus;
import com.example.smartdesk.exception.ResourceNotFoundException;
import com.example.smartdesk.repository.BookingRepository;
import com.example.smartdesk.repository.DeskRepository;
import com.example.smartdesk.repository.FloorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class DeskService {

    private final DeskRepository deskRepository;
    private final FloorRepository floorRepository;
    private final BookingRepository bookingRepository;

    public DeskService(DeskRepository deskRepository, FloorRepository floorRepository, BookingRepository bookingRepository) {
        this.deskRepository = deskRepository;
        this.floorRepository = floorRepository;
        this.bookingRepository = bookingRepository;
    }

    @Transactional(readOnly = true)
    public Desk getDeskById(Long id) {
        return deskRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Desk with id " + id + " not found"));
    }

    @Transactional(readOnly = true)
    public List<DeskResponse> getDesksByFloor(Long floorId, LocalDate date, LocalTime startTime, LocalTime endTime) {
        if (!floorRepository.existsById(floorId)) {
            throw new ResourceNotFoundException("Floor with id " + floorId + " not found");
        }

        List<Desk> desks = deskRepository.findAllActiveByFloorIdOrdered(floorId);

        if (date != null && startTime != null && endTime != null) {
            List<BookingStatus> activeStatuses = List.of(BookingStatus.CONFIRMED, BookingStatus.CHECKED_IN);
            List<Long> bookedIds = bookingRepository.findBookedDeskIdsOnFloor(floorId, date, startTime, endTime, activeStatuses);
            Set<Long> bookedSet = new HashSet<>(bookedIds);

            return desks.stream()
                .map(d -> DeskResponse.fromEntity(d, !bookedSet.contains(d.getId())))
                .toList();
        }

        return desks.stream()
            .map(d -> DeskResponse.fromEntity(d, null))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<DeskResponse> getAvailableDesks(Long floorId, LocalDate date, LocalTime startTime, LocalTime endTime) {
        if (!floorRepository.existsById(floorId)) {
            throw new ResourceNotFoundException("Floor with id " + floorId + " not found");
        }

        List<BookingStatus> activeStatuses = List.of(BookingStatus.CONFIRMED, BookingStatus.CHECKED_IN);
        List<Long> bookedIds = bookingRepository.findBookedDeskIdsOnFloor(floorId, date, startTime, endTime, activeStatuses);
        Set<Long> bookedSet = new HashSet<>(bookedIds);

        List<Desk> allDesks = deskRepository.findAllActiveByFloorIdOrdered(floorId);
        return allDesks.stream()
            .filter(d -> !bookedSet.contains(d.getId()))
            .map(d -> DeskResponse.fromEntity(d, true))
            .toList();
    }
}
