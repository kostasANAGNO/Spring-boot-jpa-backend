package com.kostasanagno.his.mapper;

import com.kostasanagno.his.dto.UserResponse;
import com.kostasanagno.his.entity.User;
import org.mapstruct.Mapper;

@Mapper(config = CentralMapperConfig.class)
public interface UserMapper {

    UserResponse toResponse(User user);
}
