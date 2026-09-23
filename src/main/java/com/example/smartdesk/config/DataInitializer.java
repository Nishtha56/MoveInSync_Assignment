package com.example.smartdesk.config;

import com.example.smartdesk.entity.*;
import com.example.smartdesk.entity.enums.DeskType;
import com.example.smartdesk.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
@org.springframework.context.annotation.Profile("!test")
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final FloorRepository floorRepository;
    private final DeskRepository deskRepository;
    private final TeamQuotaRepository teamQuotaRepository;

    public DataInitializer(
        TeamRepository teamRepository,
        UserRepository userRepository,
        FloorRepository floorRepository,
        DeskRepository deskRepository,
        TeamQuotaRepository teamQuotaRepository
    ) {
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
        this.floorRepository = floorRepository;
        this.deskRepository = deskRepository;
        this.teamQuotaRepository = teamQuotaRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (teamRepository.count() > 0) {
            log.info("Database already initialized. Skipping sample data seed.");
            return;
        }

        log.info("Initializing Smart Desk sample data...");

        // 1. Teams
        Team teamAlpha = teamRepository.save(new Team("Team Alpha", "ALPHA", "Core Engineering Team"));
        Team teamBeta = teamRepository.save(new Team("Team Beta", "BETA", "Product & Design Team"));
        Team teamGamma = teamRepository.save(new Team("Team Gamma", "GAMMA", "Platform Infrastructure Team"));

        // 2. Users (with timezones)
        User alice = userRepository.save(new User("Alice Smith", "alice@company.com", "Asia/Kolkata", teamAlpha));
        User bob = userRepository.save(new User("Bob Jones", "bob@company.com", "Asia/Kolkata", teamAlpha));
        User charlie = userRepository.save(new User("Charlie Brown", "charlie@company.com", "Asia/Kolkata", teamAlpha));
        User diana = userRepository.save(new User("Diana Prince", "diana@company.com", "America/New_York", teamBeta));
        User ethan = userRepository.save(new User("Ethan Hunt", "ethan@company.com", "Europe/London", teamBeta));
        User fiona = userRepository.save(new User("Fiona Gallagher", "fiona@company.com", "Asia/Tokyo", teamGamma));
        User george = userRepository.save(new User("George Clark", "george@company.com", "Asia/Kolkata", teamGamma));

        // 3. Floors
        Floor floor1 = floorRepository.save(new Floor(1, "Ground Floor Innovation Hub", 50, "Asia/Kolkata"));
        Floor floor2 = floorRepository.save(new Floor(2, "Level 2 Collaboration Zone", 80, "Asia/Kolkata"));
        Floor floor3 = floorRepository.save(new Floor(3, "Level 3 Smart Team Floor", 60, "Asia/Kolkata"));
        Floor floor4 = floorRepository.save(new Floor(4, "Level 4 Mega Scale Floor", 500, "Asia/Kolkata"));

        // 4. Team Quotas
        teamQuotaRepository.save(new TeamQuota(teamAlpha, floor3, 8));
        teamQuotaRepository.save(new TeamQuota(teamBeta, floor3, 4));

        // 5. Desks for Floor 3 (Hot & Fixed desks with coordinates)
        List<Desk> floor3Desks = new ArrayList<>();

        // Fixed Desk for Alice
        floor3Desks.add(new Desk("D-300", floor3, DeskType.FIXED, 5.0, 5.0, alice));

        // Clustered Hot Desks (Zone A: (10..14, 10), Zone B: (10..14, 15))
        for (int i = 1; i <= 5; i++) {
            floor3Desks.add(new Desk(String.format("D-30%d", i), floor3, DeskType.HOT, 10.0 + (i - 1), 10.0, null));
        }
        for (int i = 6; i <= 10; i++) {
            floor3Desks.add(new Desk(String.format("D-3%02d", i), floor3, DeskType.HOT, 10.0 + (i - 6), 15.0, null));
        }
        // Distant Hot Desks (Zone C: (50, 50), (51, 50))
        floor3Desks.add(new Desk("D-350", floor3, DeskType.HOT, 50.0, 50.0, null));
        floor3Desks.add(new Desk("D-351", floor3, DeskType.HOT, 51.0, 50.0, null));

        deskRepository.saveAll(floor3Desks);

        // 6. 500 Desks for Floor 4 (Mega Scale Floor Grid)
        List<Desk> floor4Desks = new ArrayList<>();
        int deskCounter = 1;
        for (int row = 1; row <= 20; row++) {
            for (int col = 1; col <= 25; col++) {
                floor4Desks.add(new Desk(
                    String.format("D-4%03d", deskCounter),
                    floor4,
                    DeskType.HOT,
                    (double) col * 2,
                    (double) row * 2,
                    null
                ));
                deskCounter++;
            }
        }
        deskRepository.saveAll(floor4Desks);

        log.info("Sample data initialization complete: 3 teams, 7 users, 4 floors, {} desks created.",
            floor3Desks.size() + floor4Desks.size());
    }
}
