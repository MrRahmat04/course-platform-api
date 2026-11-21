package com.rahmat.courseplatform.auth.dto;

import com.rahmat.courseplatform.user.dto.UserResponse;

public record AuthResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    long expiresIn,
    UserResponse user
) {}