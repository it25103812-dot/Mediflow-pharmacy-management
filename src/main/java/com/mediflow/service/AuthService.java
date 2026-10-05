package com.mediflow.service;

import com.mediflow.dto.LoginRequest;
import com.mediflow.dto.LoginResponse;
import com.mediflow.dto.UserDto;
import com.mediflow.entity.User;
import com.mediflow.exception.ApiException;
import com.mediflow.repository.UserRepository;
import com.mediflow.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuditService auditService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       AuditService auditService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.auditService = auditService;
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> ApiException.badRequest("Invalid email or password"));

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw ApiException.forbidden("Account is deactivated. Contact your administrator.");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            auditService.log("LOGIN_FAILED", "USER", user.getEmail(), "Wrong password attempt");
            throw ApiException.badRequest("Invalid email or password");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole().getName());
        auditService.log("LOGIN", "USER", user.getEmail(), "User logged in");

        return new LoginResponse(token, "Bearer", UserDto.from(user));
    }
}
