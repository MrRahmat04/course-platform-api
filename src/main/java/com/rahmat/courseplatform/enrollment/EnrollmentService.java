
package com.rahmat.courseplatform.enrollment;

import com.rahmat.courseplatform.course.Course;
import com.rahmat.courseplatform.course.CourseRepository;
import com.rahmat.courseplatform.common.exception.ResourceNotFoundException;
import com.rahmat.courseplatform.common.exception.DuplicateResourceException;
import com.rahmat.courseplatform.enrollment.dto.EnrollmentResponse;
import com.rahmat.courseplatform.enrollment.mapper.EnrollmentMapper;
import com.rahmat.courseplatform.user.User;
import com.rahmat.courseplatform.user.UserRepository;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final EnrollmentMapper enrollmentMapper;

    public EnrollmentService(
            EnrollmentRepository enrollmentRepository,
            CourseRepository courseRepository,
            UserRepository userRepository,
            EnrollmentMapper enrollmentMapper) {
        this.enrollmentRepository = enrollmentRepository;
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.enrollmentMapper = enrollmentMapper;
    }

    public EnrollmentResponse enroll(Long courseId, JwtAuthenticationToken auth) {

        User student = getCurrentUser(auth);

        if (student.getRole() != com.rahmat.courseplatform.user.Role.STUDENT) {
            throw new AccessDeniedException("Only students can enroll in courses");
        }

        Course course = courseRepository.findByIdForUpdate(courseId)
        .orElseThrow(() -> new ResourceNotFoundException("Course not found"));

        if (!course.isPublished()) {
            throw new IllegalStateException("Course is not published");
        }

        if (enrollmentRepository.existsByStudent_IdAndCourse_Id(
                student.getId(), courseId)) {
            throw new DuplicateResourceException("Already enrolled in this course");
        }

        long enrolledCount = enrollmentRepository.countByCourse_IdAndStatus(
                courseId, EnrollmentStatus.ENROLLED);

        if (enrolledCount >= course.getCapacity()) {
            throw new IllegalStateException("Course is full");
        }

        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setCourse(course);
        enrollment.setStatus(EnrollmentStatus.ENROLLED);

        return enrollmentMapper.toResponse(enrollmentRepository.save(enrollment));
    }

    @Transactional(readOnly = true)
    public List<EnrollmentResponse> getMyEnrollments(JwtAuthenticationToken auth) {

        User student = getCurrentUser(auth);

        return enrollmentRepository.findByStudent_Id(student.getId())
                .stream()
                .map(enrollmentMapper::toResponse)
                .toList();
    }

    public EnrollmentResponse drop(Long enrollmentId, JwtAuthenticationToken auth) {

        User student = getCurrentUser(auth);

        Enrollment enrollment = enrollmentRepository
                .findByIdAndStudent_Id(enrollmentId, student.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));

        if (enrollment.getStatus() != EnrollmentStatus.ENROLLED) {
            throw new IllegalStateException("Enrollment is not active");
        }

        enrollment.setStatus(EnrollmentStatus.DROPPED);

        return enrollmentMapper.toResponse(enrollment);
    }

    private User getCurrentUser(JwtAuthenticationToken auth) {

        return userRepository.findByEmail(auth.getToken().getSubject())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
