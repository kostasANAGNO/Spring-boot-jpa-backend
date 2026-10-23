package com.kostasanagno.his.service;

import com.kostasanagno.his.dto.LoginRequest;
import com.kostasanagno.his.dto.LoginResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);
}
