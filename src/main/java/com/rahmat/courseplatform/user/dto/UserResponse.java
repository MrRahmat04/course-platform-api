package com.rahmat.courseplatform.user.dto;

import com.rahmat.courseplatform.user.Role;
import java.time.Instant;

public record UserResponse(
    Long id,
    String email,
    String fullName,
    Role role,
    boolean enabled,
    Instant createdAt
) {}