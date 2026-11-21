
package com.rahmat.courseplatform.course;

import com.rahmat.courseplatform.course.dto.CourseRequest;
import com.rahmat.courseplatform.course.dto.CourseResponse;
import com.rahmat.courseplatform.course.mapper.CourseMapper;
import com.rahmat.courseplatform.user.User;
import com.rahmat.courseplatform.user.UserRepository;
import com.rahmat.courseplatform.common.exception.ResourceNotFoundException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CourseService {

    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final CourseMapper courseMapper;

    public CourseService(
            CourseRepository courseRepository,
            UserRepository userRepository,
            CourseMapper courseMapper) {
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.courseMapper = courseMapper;
    }

    public CourseResponse create(CourseRequest request, JwtAuthenticationToken auth) {
        User instructor = getCurrentUser(auth);

        if (!instructor.getRole().name().equals("INSTRUCTOR")
                && !instructor.getRole().name().equals("ADMIN")) {
            throw new AccessDeniedException("Only instructors can create courses");
        }

        Course course = new Course();
        course.setTitle(request.title());
        course.setDescription(request.description());
        course.setCapacity(request.capacity());
        course.setInstructor(instructor);

        return courseMapper.toResponse(courseRepository.save(course));
    }

    public CourseResponse update(Long id, CourseRequest request, JwtAuthenticationToken auth) {
        Course course = getCourse(id);
        checkOwnership(course, auth);

        course.setTitle(request.title());
        course.setDescription(request.description());
        course.setCapacity(request.capacity());

        return courseMapper.toResponse(course);
    }

    public CourseResponse publish(Long id, boolean published, JwtAuthenticationToken auth) {
        Course course = getCourse(id);
        checkOwnership(course, auth);

        course.setPublished(published);

        return courseMapper.toResponse(course);
    }

    @Transactional(readOnly = true)
    public CourseResponse getById(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
        return courseMapper.toResponse(course);
    }

    @Transactional(readOnly = true)
    public Page<CourseResponse> getPublishedCourses(Pageable pageable) {
        return courseRepository.findByPublishedTrue(pageable)
                .map(courseMapper::toResponse);
    }

    public void delete(Long id, JwtAuthenticationToken auth) {
        Course course = getCourse(id);
        checkOwnership(course, auth);
        courseRepository.delete(course);
    }

    private Course getCourse(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
    }

    private User getCurrentUser(JwtAuthenticationToken auth) {
        String email = auth.getToken().getSubject();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private void checkOwnership(Course course, JwtAuthenticationToken auth) {
        User currentUser = getCurrentUser(auth);

        boolean isAdmin = currentUser.getRole().name().equals("ADMIN");
        boolean isOwner = course.getInstructor().getId().equals(currentUser.getId());

        if (!isAdmin && !isOwner) {
            throw new AccessDeniedException("You cannot modify this course");
        }
    }
}
