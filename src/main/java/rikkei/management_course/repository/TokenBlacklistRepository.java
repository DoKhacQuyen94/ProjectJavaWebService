package rikkei.management_course.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rikkei.management_course.model.entity.TokenBlacklist;

public interface TokenBlacklistRepository extends JpaRepository<TokenBlacklist, Long> {
    boolean existsByToken(String jwt);
}
