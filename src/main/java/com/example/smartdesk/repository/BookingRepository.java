package com.example.smartdesk.repository;

import com.example.smartdesk.entity.Booking;
import com.example.smartdesk.entity.enums.BookingStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query("""
        SELECT b FROM Booking b 
        WHERE b.desk.id = :deskId 
          AND b.bookingDate = :bookingDate 
          AND b.status IN :statuses 
          AND b.startTime < :endTime 
          AND b.endTime > :startTime
    """)
    List<Booking> findOverlappingBookingsForDesk(
        @Param("deskId") Long deskId,
        @Param("bookingDate") LocalDate bookingDate,
        @Param("startTime") LocalTime startTime,
        @Param("endTime") LocalTime endTime,
        @Param("statuses") Collection<BookingStatus> statuses
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT b FROM Booking b 
        WHERE b.desk.id = :deskId 
          AND b.bookingDate = :bookingDate 
          AND b.status IN :statuses 
          AND b.startTime < :endTime 
          AND b.endTime > :startTime
    """)
    List<Booking> findOverlappingBookingsForDeskWithLock(
        @Param("deskId") Long deskId,
        @Param("bookingDate") LocalDate bookingDate,
        @Param("startTime") LocalTime startTime,
        @Param("endTime") LocalTime endTime,
        @Param("statuses") Collection<BookingStatus> statuses
    );

    @Query("""
        SELECT b FROM Booking b 
        WHERE b.user.id = :userId 
          AND b.bookingDate = :bookingDate 
          AND b.status IN :statuses 
          AND b.startTime < :endTime 
          AND b.endTime > :startTime
    """)
    List<Booking> findOverlappingBookingsForUser(
        @Param("userId") Long userId,
        @Param("bookingDate") LocalDate bookingDate,
        @Param("startTime") LocalTime startTime,
        @Param("endTime") LocalTime endTime,
        @Param("statuses") Collection<BookingStatus> statuses
    );

    @Query("""
        SELECT COUNT(b) FROM Booking b 
        WHERE b.floor.id = :floorId 
          AND b.bookingDate = :bookingDate 
          AND b.status IN :statuses 
          AND b.startTime < :endTime 
          AND b.endTime > :startTime
    """)
    long countActiveBookingsOnFloor(
        @Param("floorId") Long floorId,
        @Param("bookingDate") LocalDate bookingDate,
        @Param("startTime") LocalTime startTime,
        @Param("endTime") LocalTime endTime,
        @Param("statuses") Collection<BookingStatus> statuses
    );

    @Query("""
        SELECT COUNT(b) FROM Booking b 
        WHERE b.user.team.id = :teamId 
          AND b.floor.id = :floorId 
          AND b.bookingDate = :bookingDate 
          AND b.status IN :statuses 
          AND b.startTime < :endTime 
          AND b.endTime > :startTime
    """)
    long countActiveBookingsForTeamOnFloor(
        @Param("teamId") Long teamId,
        @Param("floorId") Long floorId,
        @Param("bookingDate") LocalDate bookingDate,
        @Param("startTime") LocalTime startTime,
        @Param("endTime") LocalTime endTime,
        @Param("statuses") Collection<BookingStatus> statuses
    );

    @Query("""
        SELECT b FROM Booking b 
        WHERE b.user.team.id = :teamId 
          AND b.floor.id = :floorId 
          AND b.bookingDate = :bookingDate 
          AND b.status IN :statuses 
          AND b.startTime < :endTime 
          AND b.endTime > :startTime
    """)
    List<Booking> findActiveTeamBookingsOnFloor(
        @Param("teamId") Long teamId,
        @Param("floorId") Long floorId,
        @Param("bookingDate") LocalDate bookingDate,
        @Param("startTime") LocalTime startTime,
        @Param("endTime") LocalTime endTime,
        @Param("statuses") Collection<BookingStatus> statuses
    );

    @Query("""
        SELECT b.desk.id FROM Booking b 
        WHERE b.floor.id = :floorId 
          AND b.bookingDate = :bookingDate 
          AND b.status IN :statuses 
          AND b.startTime < :endTime 
          AND b.endTime > :startTime
    """)
    List<Long> findBookedDeskIdsOnFloor(
        @Param("floorId") Long floorId,
        @Param("bookingDate") LocalDate bookingDate,
        @Param("startTime") LocalTime startTime,
        @Param("endTime") LocalTime endTime,
        @Param("statuses") Collection<BookingStatus> statuses
    );

    @Query("""
        SELECT b FROM Booking b 
        WHERE b.status = :status 
          AND (
            b.bookingDate < :currentDate 
            OR (b.bookingDate = :currentDate AND b.startTime < :graceThresholdTime)
          )
    """)
    List<Booking> findUnconfirmedBookingsPastGrace(
        @Param("status") BookingStatus status,
        @Param("currentDate") LocalDate currentDate,
        @Param("graceThresholdTime") LocalTime graceThresholdTime
    );

    List<Booking> findByUserIdOrderByBookingDateDescStartTimeDesc(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.id = :id")
    Optional<Booking> findByIdWithLock(@Param("id") Long id);
}
