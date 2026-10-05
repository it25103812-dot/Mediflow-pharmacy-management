package com.mediflow.controller;

import com.mediflow.dto.LoginRequest;
import com.mediflow.dto.LoginResponse;
import com.mediflow.dto.UserDto;
import com.mediflow.service.AuthService;
import com.mediflow.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public UserDto me() {
        return UserDto.from(userService.currentUser());
    }

    @PutMapping("/password")
    public Map<String, String> changePassword(@RequestBody Map<String, String> body) {
        userService.changePassword(body.get("currentPassword"), body.get("newPassword"));
        return Map.of("message", "Password changed successfully");
    }
}
