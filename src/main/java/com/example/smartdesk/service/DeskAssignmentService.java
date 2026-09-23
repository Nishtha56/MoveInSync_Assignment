package com.example.smartdesk.service;

import com.example.smartdesk.entity.Booking;
import com.example.smartdesk.entity.Desk;
import com.example.smartdesk.entity.Floor;
import com.example.smartdesk.entity.User;
import com.example.smartdesk.entity.enums.BookingStatus;
import com.example.smartdesk.entity.enums.DeskType;
import com.example.smartdesk.exception.BookingConflictException;
import com.example.smartdesk.exception.InvalidBookingException;
import com.example.smartdesk.exception.ResourceNotFoundException;
import com.example.smartdesk.repository.BookingRepository;
import com.example.smartdesk.repository.DeskRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@Service
public class DeskAssignmentService {

    private final DeskRepository deskRepository;
    private final BookingRepository bookingRepository;

    public DeskAssignmentService(DeskRepository deskRepository, BookingRepository bookingRepository) {
        this.deskRepository = deskRepository;
        this.bookingRepository = bookingRepository;
    }

    /**
     * Determines the optimal desk for a user on a given floor, date, and time window.
     * 
     * Algorithm Steps (Optimized for 500+ desks):
     * 1. If user has a fixed desk on this floor, allocate their fixed desk if available.
     * 2. If specific deskId requested, validate its eligibility and availability.
     * 3. Fetch active team bookings on this floor for the time window.
     * 4. Fetch all currently booked desk IDs on this floor for the time window.
     * 5. Filter active HOT candidate desks on this floor that are not booked.
     * 6. If teammates are already seated on this floor, calculate the team centroid (x_avg, y_avg)
     *    and find the candidate hot desk with the minimum Euclidean distance to that centroid.
     * 7. If no teammates booked yet, select the best candidate ordered by coordinates.
     * 
     * Time Complexity: O(D_floor + K_team) where D_floor <= 500 and K_team <= 50.
     * Space Complexity: O(D_floor).
     */
    public Desk findOptimalDesk(User user, Floor floor, LocalDate bookingDate, LocalTime startTime, LocalTime endTime, Long requestedDeskId) {
        List<BookingStatus> activeStatuses = List.of(BookingStatus.CONFIRMED, BookingStatus.CHECKED_IN);

        // 1. Check if user has a fixed desk
        Optional<Desk> userFixedDeskOpt = deskRepository.findByFixedUserIdAndIsActiveTrue(user.getId());
        if (userFixedDeskOpt.isPresent()) {
            Desk fixedDesk = userFixedDeskOpt.get();
            if (fixedDesk.getFloor().getId().equals(floor.getId())) {
                // Ensure fixed desk isn't already booked
                List<Booking> overlaps = bookingRepository.findOverlappingBookingsForDesk(
                    fixedDesk.getId(), bookingDate, startTime, endTime, activeStatuses
                );
                if (!overlaps.isEmpty()) {
                    throw new BookingConflictException("Your fixed desk " + fixedDesk.getDeskNumber() + " is already booked for this time window.");
                }
                return fixedDesk;
            }
        }

        // 2. If a specific desk ID was requested
        if (requestedDeskId != null) {
            Desk requestedDesk = deskRepository.findById(requestedDeskId)
                .orElseThrow(() -> new ResourceNotFoundException("Desk with id " + requestedDeskId + " not found"));

            if (!requestedDesk.getFloor().getId().equals(floor.getId())) {
                throw new InvalidBookingException("Requested desk is on floor " + requestedDesk.getFloor().getFloorNumber() + ", but requested floor is " + floor.getFloorNumber());
            }

            if (!Boolean.TRUE.equals(requestedDesk.getIsActive())) {
                throw new InvalidBookingException("Desk " + requestedDesk.getDeskNumber() + " is currently inactive");
            }

            if (requestedDesk.getDeskType() == DeskType.FIXED) {
                if (requestedDesk.getFixedUser() == null || !requestedDesk.getFixedUser().getId().equals(user.getId())) {
                    throw new InvalidBookingException("Desk " + requestedDesk.getDeskNumber() + " is a fixed desk reserved for another employee");
                }
            }

            List<Booking> overlaps = bookingRepository.findOverlappingBookingsForDesk(
                requestedDesk.getId(), bookingDate, startTime, endTime, activeStatuses
            );
            if (!overlaps.isEmpty()) {
                throw new BookingConflictException("Desk " + requestedDesk.getDeskNumber() + " is already booked for the selected date and time window");
            }
            return requestedDesk;
        }

        // 3. Find all currently booked desk IDs on this floor
        List<Long> bookedDeskIds = bookingRepository.findBookedDeskIdsOnFloor(
            floor.getId(), bookingDate, startTime, endTime, activeStatuses
        );
        Set<Long> bookedDeskIdSet = new HashSet<>(bookedDeskIds);

        // 4. Find all candidate HOT desks on this floor
        List<Desk> allHotDesks = deskRepository.findByFloorIdAndDeskTypeAndIsActiveTrue(floor.getId(), DeskType.HOT);
        List<Desk> availableHotDesks = allHotDesks.stream()
            .filter(d -> !bookedDeskIdSet.contains(d.getId()))
            .toList();

        if (availableHotDesks.isEmpty()) {
            throw new BookingConflictException("No hot desks available on floor " + floor.getFloorNumber() + " for the selected time window");
        }

        // 5. Team Neighbourhood Placement: Fetch existing active bookings for user's team on this floor
        List<Booking> teamBookings = Collections.emptyList();
        if (user.getTeam() != null) {
            teamBookings = bookingRepository.findActiveTeamBookingsOnFloor(
                user.getTeam().getId(), floor.getId(), bookingDate, startTime, endTime, activeStatuses
            );
        }

        if (teamBookings.isEmpty()) {
            // No teammate booked yet -> pick the first available desk ordered by coordinates
            return availableHotDesks.stream()
                .min(Comparator.comparing(Desk::getxCoordinate).thenComparing(Desk::getyCoordinate))
                .orElse(availableHotDesks.get(0));
        }

        // Calculate Team Centroid (average X and Y coordinates of current team desks)
        double totalX = 0;
        double totalY = 0;
        for (Booking tb : teamBookings) {
            totalX += tb.getDesk().getxCoordinate();
            totalY += tb.getDesk().getyCoordinate();
        }
        final double centroidX = totalX / teamBookings.size();
        final double centroidY = totalY / teamBookings.size();

        // Select the candidate hot desk with the minimum Euclidean distance to the team centroid
        return availableHotDesks.stream()
            .min(Comparator.comparingDouble((Desk d) -> d.calculateDistanceTo(centroidX, centroidY))
                .thenComparing(Desk::getxCoordinate)
                .thenComparing(Desk::getyCoordinate))
            .orElse(availableHotDesks.get(0));
    }
}
