package com.kostasanagno.his;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Boots the whole application against a real PostgreSQL. Proves that the Flyway migrations apply and
 * that Hibernate's {@code ddl-auto=validate} accepts the resulting schema, then walks the main flow:
 * seeded admin login -> patient CRUD -> soft delete.
 */
@Testcontainers
@SpringBootTest(properties = "app.jwt.secret=integration-test-secret-integration-test-secret")
@AutoConfigureMockMvc
class HisApplicationIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void flywayMigrationsAreApplied() {
        List<String> versions = jdbc.queryForList(
                "SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank", String.class);
        assertThat(versions).containsExactly("1", "2");
    }

    @Test
    void seededAdminCanRunPatientCrudFlow() throws Exception {
        String token = login("admin", "Admin123!");

        // create
        String body = """
                {"firstName":"Maria","lastName":"Papadopoulou","dateOfBirth":"1990-05-17","gender":"FEMALE",
                 "nationalId":"17059012345","phone":"+30 210 1234567","email":"maria@example.com"}""";
        String created = mockMvc.perform(post("/api/patients").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.active").value(true))
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(created).get("id").asLong();

        // duplicate national ID -> 409
        mockMvc.perform(post("/api/patients").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        // read + search + sort
        mockMvc.perform(get("/api/patients/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastName").value("Papadopoulou"));
        mockMvc.perform(get("/api/patients").param("name", "maria papad").param("sort", "lastName,desc")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(get("/api/patients").param("sort", "nope").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());

        // update
        mockMvc.perform(put("/api/patients/" + id).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(body.replace("Maria", "Marianna")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Marianna"))
                .andExpect(jsonPath("$.updatedAt").exists());

        // soft delete -> hidden from reads and searches, but the row stays
        mockMvc.perform(delete("/api/patients/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/patients/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/patients").param("nationalId", "17059012345")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.totalElements").value(0));
        Boolean active = jdbc.queryForObject("SELECT active FROM patients WHERE id = ?", Boolean.class, id);
        assertThat(active).isFalse();
    }

    @Test
    void registeredReceptionistCanWorkWithPatientsButNotDelete() throws Exception {
        String adminToken = login("admin", "Admin123!");
        mockMvc.perform(post("/api/auth/register").header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"reception1","email":"r1@his.local","password":"Sup3rSecret!",
                                 "fullName":"Rita Reception","roles":["RECEPTIONIST"]}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.password").doesNotExist());

        String token = login("reception1", "Sup3rSecret!");
        mockMvc.perform(get("/api/patients").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        mockMvc.perform(delete("/api/patients/1").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/auth/register").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void wrongPasswordIsRejectedAndAnonymousAccessBlocked() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/patients")).andExpect(status().isUnauthorized());
    }

    private String login(String username, String password) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andReturn().getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(response);
        return json.get("accessToken").asText();
    }
}
