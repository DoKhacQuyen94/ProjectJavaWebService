package rikkei.management_course.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rikkei.management_course.model.entity.Assignment;
import rikkei.management_course.model.entity.Submission;
import rikkei.management_course.model.entity.User;

import java.util.Optional;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    Optional<Submission> findByStudentAndAssignment(User student, Assignment assignment);
}
