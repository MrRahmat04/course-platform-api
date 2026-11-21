
package com.rahmat.courseplatform.course;

import com.rahmat.courseplatform.course.dto.CourseRequest;
import com.rahmat.courseplatform.course.dto.CourseResponse;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @PostMapping
    public ResponseEntity<CourseResponse> create(
            @Valid @RequestBody CourseRequest request,
            JwtAuthenticationToken auth) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(courseService.create(request, auth));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CourseResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CourseRequest request,
            JwtAuthenticationToken auth) {

        return ResponseEntity.ok(courseService.update(id, request, auth));
    }

    @PatchMapping("/{id}/publish")
    public ResponseEntity<CourseResponse> publish(
            @PathVariable Long id,
            @RequestParam boolean published,
            JwtAuthenticationToken auth) {

        return ResponseEntity.ok(courseService.publish(id, published, auth));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CourseResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(courseService.getById(id));
    }

    @GetMapping
    public ResponseEntity<Page<CourseResponse>> getPublishedCourses(
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {

        return ResponseEntity.ok(courseService.getPublishedCourses(pageable));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            JwtAuthenticationToken auth) {

        courseService.delete(id, auth);
        return ResponseEntity.noContent().build();
    }
}
