package com.kostasanagno.his.dto;

import com.kostasanagno.his.entity.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Used for both create (POST) and full update (PUT). */
public record PatientRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotNull @Past LocalDate dateOfBirth,
        @NotNull Gender gender,
        @NotBlank @Size(max = 20) @Pattern(regexp = "^[A-Za-z0-9]*$", message = "must contain only letters and digits")
        String nationalId,
        @Size(max = 30) @Pattern(regexp = "^[+0-9 ()-]*$", message = "is not a valid phone number") String phone,
        @Email @Size(max = 255) String email,
        @Size(max = 255) String address,
        @Size(max = 150) String emergencyContactName,
        @Size(max = 30) @Pattern(regexp = "^[+0-9 ()-]*$", message = "is not a valid phone number")
        String emergencyContactPhone) {
}
