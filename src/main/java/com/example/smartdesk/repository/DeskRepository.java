package com.example.smartdesk.repository;

import com.example.smartdesk.entity.Desk;
import com.example.smartdesk.entity.enums.DeskType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeskRepository extends JpaRepository<Desk, Long> {

    List<Desk> findByFloorIdAndIsActiveTrue(Long floorId);

    List<Desk> findByFloorIdAndDeskTypeAndIsActiveTrue(Long floorId, DeskType deskType);

    Optional<Desk> findByFloorIdAndDeskNumber(Long floorId, String deskNumber);

    Optional<Desk> findByFixedUserIdAndIsActiveTrue(Long fixedUserId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM Desk d WHERE d.id = :id")
    Optional<Desk> findByIdWithLock(@Param("id") Long id);

    @Query("SELECT d FROM Desk d WHERE d.floor.id = :floorId AND d.isActive = true ORDER BY d.xCoordinate ASC, d.yCoordinate ASC")
    List<Desk> findAllActiveByFloorIdOrdered(@Param("floorId") Long floorId);
}
