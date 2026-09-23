package com.example.smartdesk.repository;

import com.example.smartdesk.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {
    Optional<Team> findByCode(String code);
    Optional<Team> findByName(String name);
}
