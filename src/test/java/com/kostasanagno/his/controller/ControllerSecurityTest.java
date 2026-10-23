package com.kostasanagno.his.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kostasanagno.his.config.SecurityConfig;
import com.kostasanagno.his.dto.PatientResponse;
import com.kostasanagno.his.dto.UserResponse;
import com.kostasanagno.his.entity.Gender;
import com.kostasanagno.his.exception.GlobalExceptionHandler;
import com.kostasanagno.his.security.JwtService;
import com.kostasanagno.his.security.RestAccessDeniedHandler;
import com.kostasanagno.his.security.RestAuthenticationEntryPoint;
import com.kostasanagno.his.service.AuthService;
import com.kostasanagno.his.service.PatientService;
import com.kostasanagno.his.service.UserService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** 401 / 403 / 2xx per role for the Patient and Auth endpoints (service layer mocked). */
@WebMvcTest({PatientController.class, AuthController.class})
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        GlobalExceptionHandler.class})
class ControllerSecurityTest {

    private static final String PATIENT_JSON = """
            {"firstName":"Maria","lastName":"Papadopoulou","dateOfBirth":"1990-05-17","gender":"FEMALE",
             "nationalId":"17059012345"}""";
    private static final String REGISTER_JSON = """
            {"username":"dr.house","email":"house@his.local","password":"Sup3rSecret!","fullName":"Gregory House",
             "roles":["DOCTOR"]}""";

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private PatientService patientService;
    @MockitoBean
    private AuthService authService;
    @MockitoBean
    private UserService userService;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UserDetailsService userDetailsService;

    private static PatientResponse patient() {
        return new PatientResponse(1L, "Maria", "Papadopoulou", LocalDate.of(1990, 5, 17), Gender.FEMALE,
                "17059012345", null, null, null, null, null, true, Instant.now(), Instant.now());
    }

    @Test
    void anonymousGets401Json() throws Exception {
        mockMvc.perform(get("/api/patients/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value("/api/patients/1"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void invalidBearerTokenGets401() throws Exception {
        mockMvc.perform(get("/api/patients/1").header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "DOCTOR", "NURSE", "RECEPTIONIST"})
    void everyStaffRoleCanReadAndSearch(String role) throws Exception {
        when(patientService.getById(1L)).thenReturn(patient());
        when(patientService.search(any(), any())).thenReturn(new PageImpl<>(List.of(patient()), PageRequest.of(0, 20), 1));
        var auth = org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                .user("u").roles(role);

        mockMvc.perform(get("/api/patients/1").with(auth)).andExpect(status().isOk());
        mockMvc.perform(get("/api/patients").with(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "DOCTOR", "NURSE", "RECEPTIONIST"})
    void everyStaffRoleCanCreateAndUpdate(String role) throws Exception {
        when(patientService.create(any())).thenReturn(patient());
        when(patientService.update(any(), any())).thenReturn(patient());
        var auth = org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                .user("u").roles(role);

        mockMvc.perform(post("/api/patients").with(auth).contentType(MediaType.APPLICATION_JSON).content(PATIENT_JSON))
                .andExpect(status().isCreated());
        mockMvc.perform(put("/api/patients/1").with(auth).contentType(MediaType.APPLICATION_JSON).content(PATIENT_JSON))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"DOCTOR", "NURSE", "RECEPTIONIST"})
    void nonAdminCannotDelete(String role) throws Exception {
        var auth = org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                .user("u").roles(role);

        mockMvc.perform(delete("/api/patients/1").with(auth))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanDelete() throws Exception {
        mockMvc.perform(delete("/api/patients/1")).andExpect(status().isNoContent());
    }

    @Test
    void anonymousCannotDelete() throws Exception {
        mockMvc.perform(delete("/api/patients/1")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "RECEPTIONIST")
    void invalidPatientBodyReturnsValidationErrors() throws Exception {
        mockMvc.perform(post("/api/patients").contentType(MediaType.APPLICATION_JSON).content("{\"firstName\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.validationErrors").isArray())
                .andExpect(jsonPath("$.validationErrors[?(@.field=='lastName')]").exists());
    }

    @Test
    void loginIsPublic() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"x\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void anonymousCannotRegister() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(REGISTER_JSON))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {"DOCTOR", "NURSE", "RECEPTIONIST"})
    void nonAdminCannotRegister(String role) throws Exception {
        var auth = org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                .user("u").roles(role);
        mockMvc.perform(post("/api/auth/register").with(auth).contentType(MediaType.APPLICATION_JSON).content(REGISTER_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanRegister() throws Exception {
        when(userService.register(any())).thenReturn(
                new UserResponse(2L, "dr.house", "house@his.local", "Gregory House", true,
                        Set.of(com.kostasanagno.his.entity.Role.DOCTOR), Instant.now()));
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(REGISTER_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("dr.house"));
    }
}
