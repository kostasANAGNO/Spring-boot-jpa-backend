package com.kostasanagno.his.controller;

import com.kostasanagno.his.dto.SpecialtyResponse;
import com.kostasanagno.his.security.AccessRules;
import com.kostasanagno.his.service.SpecialtyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/specialties")
@RequiredArgsConstructor
@Tag(name = "Specialties")
public class SpecialtyController {

    private final SpecialtyService specialtyService;

    @GetMapping
    @PreAuthorize(AccessRules.ANY_STAFF)
    @Operation(summary = "List all specialties")
    public List<SpecialtyResponse> findAll() {
        return specialtyService.findAll();
    }
}
