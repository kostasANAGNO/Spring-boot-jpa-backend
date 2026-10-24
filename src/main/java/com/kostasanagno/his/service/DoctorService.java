package com.kostasanagno.his.service;

import com.kostasanagno.his.dto.DoctorRequest;
import com.kostasanagno.his.dto.DoctorResponse;
import com.kostasanagno.his.dto.DoctorSearchCriteria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DoctorService {

    DoctorResponse create(DoctorRequest request);

    DoctorResponse getById(Long id);

    /** The doctor profile linked to the login account with this username. */
    DoctorResponse getByUsername(String username);

    Page<DoctorResponse> search(DoctorSearchCriteria criteria, Pageable pageable);

    DoctorResponse update(Long id, DoctorRequest request);

    /** Soft delete: the doctor is marked inactive and hidden from normal queries. */
    void delete(Long id);
}
