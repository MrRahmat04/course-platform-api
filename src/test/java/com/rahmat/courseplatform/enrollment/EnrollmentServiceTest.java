
package com.rahmat.courseplatform.enrollment;

import com.rahmat.courseplatform.common.exception.DuplicateResourceException;
import com.rahmat.courseplatform.common.exception.ResourceNotFoundException;
import com.rahmat.courseplatform.course.Course;
import com.rahmat.courseplatform.course.CourseRepository;
import com.rahmat.courseplatform.enrollment.dto.EnrollmentResponse;
import com.rahmat.courseplatform.enrollment.mapper.EnrollmentMapper;
import com.rahmat.courseplatform.user.Role;
import com.rahmat.courseplatform.user.User;
import com.rahmat.courseplatform.user.UserRepository;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EnrollmentMapper enrollmentMapper;

    @InjectMocks
    private EnrollmentService enrollmentService;

    private User createUser(Long id, String email, Role role) {
        User user = new User(email, "password", "Test User", role);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private JwtAuthenticationToken createAuth(String email) {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "HS256")
                .subject(email)
                .build();

        return new JwtAuthenticationToken(jwt);
    }

    private Course createCourse(User instructor, Integer capacity, boolean published) {
        Course course = new Course(
                "Java Backend Course",
                "Test description",
                instructor,
                capacity
        );

        ReflectionTestUtils.setField(course, "id", 100L);
        course.setPublished(published);

        return course;
    }

    @Test
    void studentCanEnrollInPublishedCourse() {
        User student = createUser(1L, "student@test.com", Role.STUDENT);
        User instructor = createUser(2L, "instructor@test.com", Role.INSTRUCTOR);

        Course course = createCourse(instructor, 50, true);
        JwtAuthenticationToken auth = createAuth("student@test.com");

        EnrollmentResponse expected = new EnrollmentResponse(
                10L, 1L, 100L, "Java Backend Course",
                EnrollmentStatus.ENROLLED, null
        );

        when(userRepository.findByEmail("student@test.com"))
                .thenReturn(Optional.of(student));
        when(courseRepository.findByIdForUpdate(100L))
                .thenReturn(Optional.of(course));
        when(enrollmentRepository.existsByStudent_IdAndCourse_Id(1L, 100L))
                .thenReturn(false);
        when(enrollmentRepository.countByCourse_IdAndStatus(
                100L, EnrollmentStatus.ENROLLED))
                .thenReturn(0L);
        when(enrollmentRepository.save(any(Enrollment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(enrollmentMapper.toResponse(any(Enrollment.class)))
                .thenReturn(expected);

        EnrollmentResponse result = enrollmentService.enroll(100L, auth);

        assertEquals(expected, result);

        verify(enrollmentRepository).save(any(Enrollment.class));
    }

    @Test
    void instructorCannotEnrollInCourse() {
        User instructor = createUser(2L, "instructor@test.com", Role.INSTRUCTOR);
        JwtAuthenticationToken auth = createAuth("instructor@test.com");

        when(userRepository.findByEmail("instructor@test.com"))
                .thenReturn(Optional.of(instructor));

        assertThrows(
                AccessDeniedException.class,
                () -> enrollmentService.enroll(100L, auth)
        );

        verifyNoInteractions(courseRepository);
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    void studentCannotEnrollInUnpublishedCourse() {
        User student = createUser(1L, "student@test.com", Role.STUDENT);
        User instructor = createUser(2L, "instructor@test.com", Role.INSTRUCTOR);

        Course course = createCourse(instructor, 50, false);
        JwtAuthenticationToken auth = createAuth("student@test.com");

        when(userRepository.findByEmail("student@test.com"))
                .thenReturn(Optional.of(student));
        when(courseRepository.findByIdForUpdate(100L))
                .thenReturn(Optional.of(course));

        assertThrows(
                IllegalStateException.class,
                () -> enrollmentService.enroll(100L, auth)
        );

        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    void duplicateEnrollmentIsRejected() {
        User student = createUser(1L, "student@test.com", Role.STUDENT);
        User instructor = createUser(2L, "instructor@test.com", Role.INSTRUCTOR);

        Course course = createCourse(instructor, 50, true);
        JwtAuthenticationToken auth = createAuth("student@test.com");

        when(userRepository.findByEmail("student@test.com"))
                .thenReturn(Optional.of(student));
        when(courseRepository.findByIdForUpdate(100L))
                .thenReturn(Optional.of(course));
        when(enrollmentRepository.existsByStudent_IdAndCourse_Id(1L, 100L))
                .thenReturn(true);

        assertThrows(
                DuplicateResourceException.class,
                () -> enrollmentService.enroll(100L, auth)
        );

        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    void enrollmentRejectedWhenCourseIsFull() {
        User student = createUser(1L, "student@test.com", Role.STUDENT);
        User instructor = createUser(2L, "instructor@test.com", Role.INSTRUCTOR);

        Course course = createCourse(instructor, 1, true);
        JwtAuthenticationToken auth = createAuth("student@test.com");

        when(userRepository.findByEmail("student@test.com"))
                .thenReturn(Optional.of(student));
        when(courseRepository.findByIdForUpdate(100L))
                .thenReturn(Optional.of(course));
        when(enrollmentRepository.existsByStudent_IdAndCourse_Id(1L, 100L))
                .thenReturn(false);
        when(enrollmentRepository.countByCourse_IdAndStatus(
                100L, EnrollmentStatus.ENROLLED))
                .thenReturn(1L);

        assertThrows(
                IllegalStateException.class,
                () -> enrollmentService.enroll(100L, auth)
        );

        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    void studentCanDropOwnActiveEnrollment() {
        User student = createUser(1L, "student@test.com", Role.STUDENT);
        User instructor = createUser(2L, "instructor@test.com", Role.INSTRUCTOR);

        Course course = createCourse(instructor, 50, true);
        Enrollment enrollment = new Enrollment(student, course);

        JwtAuthenticationToken auth = createAuth("student@test.com");

        EnrollmentResponse expected = new EnrollmentResponse(
                10L, 1L, 100L, "Java Backend Course",
                EnrollmentStatus.DROPPED, null
        );

        when(userRepository.findByEmail("student@test.com"))
                .thenReturn(Optional.of(student));
        when(enrollmentRepository.findByIdAndStudent_Id(10L, 1L))
                .thenReturn(Optional.of(enrollment));
        when(enrollmentMapper.toResponse(enrollment))
                .thenReturn(expected);

        EnrollmentResponse result = enrollmentService.drop(10L, auth);

        assertEquals(EnrollmentStatus.DROPPED, enrollment.getStatus());
        assertEquals(expected, result);
    }
}
