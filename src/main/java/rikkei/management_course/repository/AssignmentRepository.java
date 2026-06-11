package rikkei.management_course.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rikkei.management_course.model.entity.Assignment;

public interface AssignmentRepository extends JpaRepository<Assignment,Long> {
}
