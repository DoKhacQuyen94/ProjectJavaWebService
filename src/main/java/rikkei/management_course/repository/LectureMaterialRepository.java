package rikkei.management_course.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rikkei.management_course.model.entity.LectureMaterial;

public interface LectureMaterialRepository extends JpaRepository<LectureMaterial,Long> {
}
