package com.example.smartdesk.repository;

import com.example.smartdesk.entity.TeamQuota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamQuotaRepository extends JpaRepository<TeamQuota, Long> {
    Optional<TeamQuota> findByTeamIdAndFloorId(Long teamId, Long floorId);
    List<TeamQuota> findByFloorId(Long floorId);
    List<TeamQuota> findByTeamId(Long teamId);
}
