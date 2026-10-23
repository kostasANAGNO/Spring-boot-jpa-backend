package com.kostasanagno.his.repository;

import com.kostasanagno.his.entity.Patient;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PatientRepository extends JpaRepository<Patient, Long>, JpaSpecificationExecutor<Patient> {

    /** Soft-deleted patients are never returned by this lookup. */
    Optional<Patient> findByIdAndActiveTrue(Long id);

    /** Checks all rows, soft-deleted included, because the national ID stays unique in the table. */
    boolean existsByNationalId(String nationalId);

    boolean existsByNationalIdAndIdNot(String nationalId, Long id);
}
