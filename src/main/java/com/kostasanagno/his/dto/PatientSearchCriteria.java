package com.kostasanagno.his.dto;

import com.kostasanagno.his.entity.Gender;

/** All filters are optional; text filters are case-insensitive "contains" matches. */
public record PatientSearchCriteria(
        String name,
        String nationalId,
        String phone,
        String email,
        Gender gender) {
}
