package com.rahmat.courseplatform.course.dto;

import java.time.Instant;

public record CourseResponse(
    Long id,
    String title,
    String description,
    Long instructorId,
    String instructorName,
    Integer capacity,
    boolean published,
    Instant createdAt
) {}