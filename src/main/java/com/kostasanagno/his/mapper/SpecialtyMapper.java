package com.kostasanagno.his.mapper;

import com.kostasanagno.his.dto.SpecialtyResponse;
import com.kostasanagno.his.entity.Specialty;
import org.mapstruct.Mapper;

@Mapper(config = CentralMapperConfig.class)
public interface SpecialtyMapper {

    SpecialtyResponse toResponse(Specialty specialty);
}
