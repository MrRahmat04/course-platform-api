
package com.rahmat.courseplatform.enrollment;

import com.rahmat.courseplatform.enrollment.dto.EnrollmentRequest;
import com.rahmat.courseplatform.enrollment.dto.EnrollmentResponse;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/enrollments")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    public EnrollmentController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    @PostMapping
    public ResponseEntity<EnrollmentResponse> enroll(
            @Valid @RequestBody EnrollmentRequest request,
            JwtAuthenticationToken auth) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(enrollmentService.enroll(request.courseId(), auth));
    }

    @GetMapping("/my")
    public ResponseEntity<List<EnrollmentResponse>> getMyEnrollments(
            JwtAuthenticationToken auth) {

        return ResponseEntity.ok(enrollmentService.getMyEnrollments(auth));
    }

    @PatchMapping("/{id}/drop")
    public ResponseEntity<EnrollmentResponse> drop(
            @PathVariable Long id,
            JwtAuthenticationToken auth) {

        return ResponseEntity.ok(enrollmentService.drop(id, auth));
    }
}
