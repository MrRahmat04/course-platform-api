package com.rahmat.courseplatform.course.mapper;

import com.rahmat.courseplatform.course.Course;
import com.rahmat.courseplatform.course.dto.CourseResponse;
import org.springframework.stereotype.Component;

@Component
public class CourseMapper {

    public CourseResponse toResponse(Course course) {
        return new CourseResponse(
            course.getId(),
            course.getTitle(),
            course.getDescription(),
            course.getInstructor().getId(),
            course.getInstructor().getFullName(),
            course.getCapacity(),
            course.isPublished(),
            course.getCreatedAt()
        );
    }
}