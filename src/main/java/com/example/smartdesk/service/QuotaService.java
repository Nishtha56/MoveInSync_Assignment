package com.example.smartdesk.service;

import com.example.smartdesk.entity.Floor;
import com.example.smartdesk.entity.TeamQuota;
import com.example.smartdesk.entity.User;
import com.example.smartdesk.entity.enums.BookingStatus;
import com.example.smartdesk.exception.QuotaExceededException;
import com.example.smartdesk.repository.BookingRepository;
import com.example.smartdesk.repository.TeamQuotaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
public class QuotaService {

    private final BookingRepository bookingRepository;
    private final TeamQuotaRepository teamQuotaRepository;

    public QuotaService(BookingRepository bookingRepository, TeamQuotaRepository teamQuotaRepository) {
        this.bookingRepository = bookingRepository;
        this.teamQuotaRepository = teamQuotaRepository;
    }

    /**
     * Validates both floor capacity and team-specific quotas before booking confirmation.
     */
    public void validateQuotas(User user, Floor floor, LocalDate bookingDate, LocalTime startTime, LocalTime endTime) {
        List<BookingStatus> activeStatuses = List.of(BookingStatus.CONFIRMED, BookingStatus.CHECKED_IN);

        // 1. Check Floor Capacity
        long activeFloorBookings = bookingRepository.countActiveBookingsOnFloor(
            floor.getId(), bookingDate, startTime, endTime, activeStatuses
        );

        if (activeFloorBookings >= floor.getCapacity()) {
            throw new QuotaExceededException(
                String.format("Floor %d capacity reached (Max: %d, Current: %d). No more bookings allowed.",
                    floor.getFloorNumber(), floor.getCapacity(), activeFloorBookings)
            );
        }

        // 2. Check Team Quota on this Floor (if team assigned and quota defined)
        if (user.getTeam() != null) {
            Optional<TeamQuota> quotaOpt = teamQuotaRepository.findByTeamIdAndFloorId(user.getTeam().getId(), floor.getId());
            if (quotaOpt.isPresent()) {
                TeamQuota teamQuota = quotaOpt.get();
                long activeTeamBookings = bookingRepository.countActiveBookingsForTeamOnFloor(
                    user.getTeam().getId(), floor.getId(), bookingDate, startTime, endTime, activeStatuses
                );

                if (activeTeamBookings >= teamQuota.getMaxDesks()) {
                    throw new QuotaExceededException(
                        String.format("Team '%s' has reached its maximum quota of %d desks on Floor %d (Current: %d).",
                            user.getTeam().getName(), teamQuota.getMaxDesks(), floor.getFloorNumber(), activeTeamBookings)
                    );
                }
            }
        }
    }
}
