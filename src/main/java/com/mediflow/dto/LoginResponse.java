package com.mediflow.dto;

public record LoginResponse(String token, String tokenType, UserDto user) {
}
