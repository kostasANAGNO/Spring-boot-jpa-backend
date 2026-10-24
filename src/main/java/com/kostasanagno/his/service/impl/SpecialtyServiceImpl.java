package com.kostasanagno.his.service.impl;

import com.kostasanagno.his.dto.SpecialtyResponse;
import com.kostasanagno.his.mapper.SpecialtyMapper;
import com.kostasanagno.his.repository.SpecialtyRepository;
import com.kostasanagno.his.service.SpecialtyService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SpecialtyServiceImpl implements SpecialtyService {

    private final SpecialtyRepository specialtyRepository;
    private final SpecialtyMapper specialtyMapper;

    @Override
    public List<SpecialtyResponse> findAll() {
        return specialtyRepository.findAllByOrderByNameAsc().stream().map(specialtyMapper::toResponse).toList();
    }
}
