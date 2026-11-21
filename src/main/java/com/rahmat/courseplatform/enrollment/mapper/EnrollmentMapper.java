package com.rahmat.courseplatform.enrollment.mapper;

import com.rahmat.courseplatform.enrollment.Enrollment;
import com.rahmat.courseplatform.enrollment.dto.EnrollmentResponse;
import org.springframework.stereotype.Component;

@Component
public class EnrollmentMapper {

    public EnrollmentResponse toResponse(Enrollment enrollment) {
        return new EnrollmentResponse(
            enrollment.getId(),
            enrollment.getStudent().getId(),
            enrollment.getCourse().getId(),
            enrollment.getCourse().getTitle(),
            enrollment.getStatus(),
            enrollment.getEnrolledAt()
        );
    }
}