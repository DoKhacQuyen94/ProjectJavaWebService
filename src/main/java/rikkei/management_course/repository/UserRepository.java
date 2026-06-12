package rikkei.management_course.repository;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import rikkei.management_course.model.entity.User;


import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    Page<User> findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(String username, String email, Pageable pageable);

    Optional<User> findByEmail(@NotBlank(message = "Email không được để trống") @Email(message = "Email không đúng định dạng") String email);

    @Query("SELECT COUNT(c) FROM Course c WHERE c.lecturer.id = :lecturerId")
    long countCoursesByLecturerId(@Param("lecturerId") Long lecturerId);
}
