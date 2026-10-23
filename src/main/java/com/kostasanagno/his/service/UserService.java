package com.kostasanagno.his.service;

import com.kostasanagno.his.dto.RegisterRequest;
import com.kostasanagno.his.dto.UserResponse;

public interface UserService {

    UserResponse register(RegisterRequest request);
}
