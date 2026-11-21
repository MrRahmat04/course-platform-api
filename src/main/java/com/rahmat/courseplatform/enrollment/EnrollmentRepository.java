
package com.rahmat.courseplatform.enrollment;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    boolean existsByStudent_IdAndCourse_Id(Long studentId, Long courseId);

    List<Enrollment> findByStudent_Id(Long studentId);

    List<Enrollment> findByCourse_Id(Long courseId);

    long countByCourse_IdAndStatus(Long courseId, EnrollmentStatus status);

    java.util.Optional<Enrollment> findByIdAndStudent_Id(
            Long enrollmentId, Long studentId);
}
