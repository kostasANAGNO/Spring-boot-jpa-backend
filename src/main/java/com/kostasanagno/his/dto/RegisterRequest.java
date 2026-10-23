package com.kostasanagno.his.dto;

import com.kostasanagno.his.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record RegisterRequest(
        @NotBlank @Size(min = 3, max = 50) @Pattern(regexp = "^[A-Za-z0-9._-]+$",
                message = "may contain only letters, digits, '.', '_' and '-'") String username,
        @NotBlank @Email @Size(max = 255) String email,
        // BCrypt only uses the first 72 bytes
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank @Size(max = 150) String fullName,
        @NotEmpty Set<Role> roles) {
}
