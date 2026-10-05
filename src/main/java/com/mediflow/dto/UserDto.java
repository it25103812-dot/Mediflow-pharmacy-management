package com.mediflow.dto;

import java.time.LocalDateTime;

public record UserDto(
        Long id,
        String firstName,
        String lastName,
        String fullName,
        String email,
        String roleName,
        Boolean active,
        LocalDateTime createdAt) {

    public static UserDto from(com.mediflow.entity.User u) {
        if (u == null) return null;
        return new UserDto(u.getId(), u.getFirstName(), u.getLastName(), u.getFullName(),
                u.getEmail(), u.getRole() != null ? u.getRole().getName() : null,
                u.getActive(), u.getCreatedAt());
    }
}
