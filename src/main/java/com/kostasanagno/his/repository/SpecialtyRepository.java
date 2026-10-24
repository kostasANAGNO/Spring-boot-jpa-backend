package com.kostasanagno.his.repository;

import com.kostasanagno.his.entity.Specialty;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpecialtyRepository extends JpaRepository<Specialty, Long> {

    List<Specialty> findAllByOrderByNameAsc();
}
