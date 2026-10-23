package com.kostasanagno.his.service;

import com.kostasanagno.his.dto.PatientRequest;
import com.kostasanagno.his.dto.PatientResponse;
import com.kostasanagno.his.dto.PatientSearchCriteria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PatientService {

    PatientResponse create(PatientRequest request);

    PatientResponse getById(Long id);

    Page<PatientResponse> search(PatientSearchCriteria criteria, Pageable pageable);

    PatientResponse update(Long id, PatientRequest request);

    /** Soft delete: the patient is marked inactive and hidden from every query. */
    void delete(Long id);
}
