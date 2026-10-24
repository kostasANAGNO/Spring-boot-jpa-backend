package com.kostasanagno.his.controller;

import com.kostasanagno.his.dto.DoctorRequest;
import com.kostasanagno.his.dto.DoctorResponse;
import com.kostasanagno.his.dto.DoctorSearchCriteria;
import com.kostasanagno.his.dto.PageResponse;
import com.kostasanagno.his.security.AccessRules;
import com.kostasanagno.his.service.DoctorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
@Tag(name = "Doctors")
public class DoctorController {

    private final DoctorService doctorService;

    @PostMapping
    @PreAuthorize(AccessRules.ADMIN_ONLY)
    @Operation(summary = "Create a doctor (ADMIN only)")
    public ResponseEntity<DoctorResponse> create(@Valid @RequestBody DoctorRequest request) {
        DoctorResponse created = doctorService.create(request);
        var location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/me")
    @PreAuthorize(AccessRules.DOCTOR_ONLY)
    @Operation(summary = "The doctor profile linked to the authenticated user (DOCTOR only)")
    public DoctorResponse me(Principal principal) {
        return doctorService.getByUsername(principal.getName());
    }

    @GetMapping("/{id}")
    @PreAuthorize(AccessRules.ANY_STAFF)
    @Operation(summary = "Get a doctor by id")
    public DoctorResponse getById(@PathVariable Long id) {
        return doctorService.getById(id);
    }

    @GetMapping
    @PreAuthorize(AccessRules.ANY_STAFF)
    @Operation(summary = "Search doctors (paged, sortable, filterable)")
    public PageResponse<DoctorResponse> search(
            @ModelAttribute DoctorSearchCriteria criteria,
            @PageableDefault(size = 20, sort = "lastName", direction = Sort.Direction.ASC) Pageable pageable) {
        return PageResponse.from(doctorService.search(criteria, pageable));
    }

    @PutMapping("/{id}")
    @PreAuthorize(AccessRules.ADMIN_ONLY)
    @Operation(summary = "Update a doctor (ADMIN only)")
    public DoctorResponse update(@PathVariable Long id, @Valid @RequestBody DoctorRequest request) {
        return doctorService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(AccessRules.ADMIN_ONLY)
    @Operation(summary = "Soft-delete a doctor (ADMIN only)")
    public void delete(@PathVariable Long id) {
        doctorService.delete(id);
    }
}
