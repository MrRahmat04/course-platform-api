package com.rahmat.courseplatform.user.mapper;

import com.rahmat.courseplatform.user.User;
import com.rahmat.courseplatform.user.dto.UserResponse;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        return new UserResponse(
            user.getId(),
            user.getEmail(),
            user.getFullName(),
            user.getRole(),
            user.isEnabled(),
            user.getCreatedAt()
        );
    }
}