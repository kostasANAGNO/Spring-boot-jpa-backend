package com.kostasanagno.his.service.impl;

import com.kostasanagno.his.dto.RegisterRequest;
import com.kostasanagno.his.dto.UserResponse;
import com.kostasanagno.his.entity.User;
import com.kostasanagno.his.exception.DuplicateResourceException;
import com.kostasanagno.his.mapper.UserMapper;
import com.kostasanagno.his.repository.UserRepository;
import com.kostasanagno.his.service.UserService;
import java.util.HashSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserResponse register(RegisterRequest request) {
        String username = request.username().trim();
        String email = request.email().trim();
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw DuplicateResourceException.of("User", "username", username);
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw DuplicateResourceException.of("User", "email", email);
        }
        User user = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .fullName(request.fullName().trim())
                .roles(new HashSet<>(request.roles()))
                .build();
        User saved = userRepository.save(user);
        log.info("Registered user '{}' with roles {}", saved.getUsername(), saved.getRoles());
        return userMapper.toResponse(saved);
    }
}
