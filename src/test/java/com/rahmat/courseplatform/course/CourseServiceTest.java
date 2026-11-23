
package com.rahmat.courseplatform.course;

import com.rahmat.courseplatform.common.exception.ResourceNotFoundException;
import com.rahmat.courseplatform.course.dto.CourseResponse;
import com.rahmat.courseplatform.course.mapper.CourseMapper;
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
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CourseMapper courseMapper;

    @InjectMocks
    private CourseService courseService;

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

    private Course createCourse(User instructor, boolean published) {
        Course course = new Course(
                "Java Backend Course",
                "Test description",
                instructor,
                50
        );

        ReflectionTestUtils.setField(course, "id", 100L);
        course.setPublished(published);

        return course;
    }

    @Test
    void instructorCanViewOwnUnpublishedCourse() {
        User instructor = createUser(1L, "instructor@test.com", Role.INSTRUCTOR);
        Course course = createCourse(instructor, false);

        JwtAuthenticationToken auth = createAuth("instructor@test.com");

        CourseResponse expected = new CourseResponse(
                100L, "Java Backend Course", "Test description",
                1L, "Test User", 50, false, null
        );

        when(courseRepository.findById(100L)).thenReturn(Optional.of(course));
        when(userRepository.findByEmail("instructor@test.com"))
                .thenReturn(Optional.of(instructor));
        when(courseMapper.toResponse(course)).thenReturn(expected);

        CourseResponse result = courseService.getById(100L, auth);

        assertEquals(expected, result);
    }

    @Test
    void studentCannotViewUnpublishedCourse() {
        User instructor = createUser(1L, "instructor@test.com", Role.INSTRUCTOR);
        User student = createUser(2L, "student@test.com", Role.STUDENT);

        Course course = createCourse(instructor, false);
        JwtAuthenticationToken auth = createAuth("student@test.com");

        when(courseRepository.findById(100L)).thenReturn(Optional.of(course));
        when(userRepository.findByEmail("student@test.com"))
                .thenReturn(Optional.of(student));

        assertThrows(
                ResourceNotFoundException.class,
                () -> courseService.getById(100L, auth)
        );

        verify(courseMapper, never()).toResponse(any());
    }

    @Test
    void studentCanViewPublishedCourse() {
        User instructor = createUser(1L, "instructor@test.com", Role.INSTRUCTOR);
        User student = createUser(2L, "student@test.com", Role.STUDENT);

        Course course = createCourse(instructor, true);
        JwtAuthenticationToken auth = createAuth("student@test.com");

        CourseResponse expected = new CourseResponse(
                100L, "Java Backend Course", "Test description",
                1L, "Test User", 50, true, null
        );

        when(courseRepository.findById(100L)).thenReturn(Optional.of(course));
        when(courseMapper.toResponse(course)).thenReturn(expected);

        CourseResponse result = courseService.getById(100L, auth);

        assertEquals(expected, result);
    }

    @Test
    void visitorCanViewPublishedCourse() {
        User instructor = createUser(1L, "instructor@test.com", Role.INSTRUCTOR);
        Course course = createCourse(instructor, true);

        CourseResponse expected = new CourseResponse(
                100L, "Java Backend Course", "Test description",
                1L, "Test User", 50, true, null
        );

        when(courseRepository.findById(100L)).thenReturn(Optional.of(course));
        when(courseMapper.toResponse(course)).thenReturn(expected);

        CourseResponse result = courseService.getById(100L, null);

        assertEquals(expected, result);
        verifyNoInteractions(userRepository);
    }

    @Test
    void visitorCannotViewUnpublishedCourse() {
        User instructor = createUser(1L, "instructor@test.com", Role.INSTRUCTOR);
        Course course = createCourse(instructor, false);

        when(courseRepository.findById(100L)).thenReturn(Optional.of(course));

        assertThrows(
                ResourceNotFoundException.class,
                () -> courseService.getById(100L, null)
        );

        verifyNoInteractions(userRepository);
        verify(courseMapper, never()).toResponse(any());
    }

    @Test
    void adminCanViewUnpublishedCourse() {
        User instructor = createUser(1L, "instructor@test.com", Role.INSTRUCTOR);
        User admin = createUser(3L, "admin@test.com", Role.ADMIN);

        Course course = createCourse(instructor, false);
        JwtAuthenticationToken auth = createAuth("admin@test.com");

        CourseResponse expected = new CourseResponse(
                100L, "Java Backend Course", "Test description",
                1L, "Test User", 50, false, null
        );

        when(courseRepository.findById(100L)).thenReturn(Optional.of(course));
        when(userRepository.findByEmail("admin@test.com"))
                .thenReturn(Optional.of(admin));
        when(courseMapper.toResponse(course)).thenReturn(expected);

        CourseResponse result = courseService.getById(100L, auth);

        assertEquals(expected, result);
    }

    @Test
    void differentInstructorCannotViewUnpublishedCourse() {
        User owner = createUser(1L, "owner@test.com", Role.INSTRUCTOR);
        User otherInstructor = createUser(2L, "other@test.com", Role.INSTRUCTOR);

        Course course = createCourse(owner, false);
        JwtAuthenticationToken auth = createAuth("other@test.com");

        when(courseRepository.findById(100L)).thenReturn(Optional.of(course));
        when(userRepository.findByEmail("other@test.com"))
                .thenReturn(Optional.of(otherInstructor));

        assertThrows(
                ResourceNotFoundException.class,
                () -> courseService.getById(100L, auth)
        );
    }

    @Test
    void missingCourseThrowsResourceNotFoundException() {
        JwtAuthenticationToken auth = createAuth("student@test.com");

        when(courseRepository.findById(100L)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> courseService.getById(100L, auth)
        );

        verifyNoInteractions(userRepository);
    }
}
