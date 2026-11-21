package com.rahmat.courseplatform.enrollment.dto;

import com.rahmat.courseplatform.enrollment.EnrollmentStatus;
import java.time.Instant;

public record EnrollmentResponse(
    Long id,
    Long studentId,
    Long courseId,
    String courseTitle,
    EnrollmentStatus status,
    Instant enrolledAt
) {}