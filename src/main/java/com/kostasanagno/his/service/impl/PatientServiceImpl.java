package com.kostasanagno.his.service.impl;

import com.kostasanagno.his.dto.PatientRequest;
import com.kostasanagno.his.dto.PatientResponse;
import com.kostasanagno.his.dto.PatientSearchCriteria;
import com.kostasanagno.his.entity.Patient;
import com.kostasanagno.his.exception.DuplicateResourceException;
import com.kostasanagno.his.exception.ResourceNotFoundException;
import com.kostasanagno.his.mapper.PatientMapper;
import com.kostasanagno.his.repository.PatientRepository;
import com.kostasanagno.his.repository.PatientSpecifications;
import com.kostasanagno.his.service.PatientService;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PatientServiceImpl implements PatientService {

    private static final String RESOURCE = "Patient";

    private final PatientRepository patientRepository;
    private final PatientMapper patientMapper;

    @Override
    public PatientResponse create(PatientRequest request) {
        String nationalId = request.nationalId().trim();
        if (patientRepository.existsByNationalId(nationalId)) {
            throw DuplicateResourceException.of(RESOURCE, "national ID", nationalId);
        }
        Patient patient = patientMapper.toEntity(request);
        patient.setNationalId(nationalId);
        Patient saved = patientRepository.save(patient);
        log.info("Created patient {}", saved.getId());
        return patientMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PatientResponse getById(Long id) {
        return patientMapper.toResponse(findActive(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PatientResponse> search(PatientSearchCriteria criteria, Pageable pageable) {
        return patientRepository.findAll(PatientSpecifications.from(criteria), pageable)
                .map(patientMapper::toResponse);
    }

    @Override
    public PatientResponse update(Long id, PatientRequest request) {
        Patient patient = findActive(id);
        String nationalId = request.nationalId().trim();
        if (patientRepository.existsByNationalIdAndIdNot(nationalId, id)) {
            throw DuplicateResourceException.of(RESOURCE, "national ID", nationalId);
        }
        patientMapper.updateEntity(request, patient);
        patient.setNationalId(nationalId);
        log.info("Updated patient {}", id);
        return patientMapper.toResponse(patientRepository.saveAndFlush(patient));
    }

    @Override
    public void delete(Long id) {
        Patient patient = findActive(id);
        patient.setActive(false);
        patient.setDeletedAt(Instant.now());
        log.info("Soft-deleted patient {}", id);
    }

    private Patient findActive(Long id) {
        return patientRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> ResourceNotFoundException.of(RESOURCE, id));
    }
}
