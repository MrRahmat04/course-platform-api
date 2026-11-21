package com.rahmat.courseplatform.enrollment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record EnrollmentRequest(

    @NotNull
    @Positive
    Long courseId

) {}