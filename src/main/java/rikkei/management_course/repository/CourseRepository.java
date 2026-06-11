package rikkei.management_course.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import rikkei.management_course.model.entity.Course;

public interface CourseRepository extends JpaRepository<Course, Long> {
    Page<Course> findByCourseNameContainingIgnoreCase(String courseName, Pageable pageable);
}
