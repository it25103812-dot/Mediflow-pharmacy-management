package com.mediflow.controller;

import com.mediflow.dto.PageResponse;
import com.mediflow.dto.UserDto;
import com.mediflow.dto.UserRequest;
import com.mediflow.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMINISTRATOR')")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public PageResponse<UserDto> list(@RequestParam(defaultValue = "") String search,
                                      @RequestParam(required = false) String role,
                                      @RequestParam(required = false) Boolean active,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "10") int size) {
        return userService.search(search, role, active, page, size);
    }

    @GetMapping("/{id}")
    public UserDto get(@PathVariable Long id) {
        return userService.getById(id);
    }

    @PostMapping
    public UserDto create(@Valid @RequestBody UserRequest request) {
        return userService.create(request);
    }

    @PutMapping("/{id}")
    public UserDto update(@PathVariable Long id, @Valid @RequestBody UserRequest request) {
        return userService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    public UserDto setActive(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        return userService.setActive(id, Boolean.TRUE.equals(body.get("active")));
    }
}
