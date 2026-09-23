package com.example.smartdesk;

import com.example.smartdesk.dto.request.BookingRequest;
import com.example.smartdesk.entity.*;
import com.example.smartdesk.entity.enums.DeskType;
import com.example.smartdesk.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class BookingControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private DeskRepository deskRepository;

    @Autowired
    private FloorRepository floorRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TeamQuotaRepository teamQuotaRepository;

    private User testUser;
    private Floor testFloor;
    private Desk testDesk;

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
        teamQuotaRepository.deleteAll();
        deskRepository.deleteAll();
        userRepository.deleteAll();
        floorRepository.deleteAll();
        teamRepository.deleteAll();

        Team team = teamRepository.save(new Team("Integration Team", "INT", "Testing"));
        testFloor = floorRepository.save(new Floor(1, "Main Floor", 20, "Asia/Kolkata"));
        testDesk = deskRepository.save(new Desk("D-101", testFloor, DeskType.HOT, 10.0, 10.0, null));
        testUser = userRepository.save(new User("Integration User", "intuser@test.com", "Asia/Kolkata", team));
    }

    @Test
    @DisplayName("REST API: Create booking -> 201 CREATED with detailed response DTO")
    void testCreateBookingEndpoint() throws Exception {
        LocalDate date = LocalDate.now().plusDays(2);
        BookingRequest request = new BookingRequest(
            testUser.getId(),
            testFloor.getId(),
            date,
            LocalTime.of(9, 0),
            LocalTime.of(17, 0)
        );

        mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.bookingId").isNumber())
            .andExpect(jsonPath("$.userId").value(testUser.getId()))
            .andExpect(jsonPath("$.deskId").value(testDesk.getId()))
            .andExpect(jsonPath("$.deskNumber").value("D-101"))
            .andExpect(jsonPath("$.status").value("CONFIRMED"))
            .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    @DisplayName("REST API: Duplicate booking on same day/time -> 409 CONFLICT")
    void testDuplicateBookingReturns409() throws Exception {
        LocalDate date = LocalDate.now().plusDays(2);
        BookingRequest request = new BookingRequest(
            testUser.getId(),
            testFloor.getId(),
            date,
            LocalTime.of(9, 0),
            LocalTime.of(17, 0)
        );

        // First booking succeeds
        mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());

        // Second duplicate booking fails with 409
        mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.error").value("Booking Conflict"));
    }

    @Test
    @DisplayName("REST API: Get available desks -> 200 OK")
    void testGetAvailableDesksEndpoint() throws Exception {
        mockMvc.perform(get("/api/desks/available")
                .param("floorId", testFloor.getId().toString())
                .param("date", LocalDate.now().plusDays(1).toString())
                .param("startTime", "09:00")
                .param("endTime", "17:00"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].deskNumber").value("D-101"))
            .andExpect(jsonPath("$[0].isAvailable").value(true));
    }

    @Test
    @DisplayName("REST API: Get Floors and Desks on Floor")
    void testGetFloorsAndDesks() throws Exception {
        mockMvc.perform(get("/api/floors"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].floorNumber").value(1));

        mockMvc.perform(get("/api/floors/" + testFloor.getId() + "/desks"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].deskNumber").value("D-101"));
    }

    @Test
    @DisplayName("REST API: Validation errors on invalid booking payload -> 400 BAD REQUEST")
    void testValidationErrorsReturn400() throws Exception {
        // Missing required fields
        BookingRequest emptyRequest = new BookingRequest();

        mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(emptyRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.validationErrors").isMap());
    }
}
