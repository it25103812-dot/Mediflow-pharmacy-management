package com.mediflow.repository;

import com.mediflow.entity.Role;
import com.mediflow.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    long countByActiveTrue();

    @Query("""
           SELECT u FROM User u
           WHERE (:search IS NULL OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
                               OR LOWER(u.lastName)  LIKE LOWER(CONCAT('%', :search, '%'))
                               OR LOWER(u.email)     LIKE LOWER(CONCAT('%', :search, '%')))
             AND (:roleName IS NULL OR u.role.name = :roleName)
             AND (:active IS NULL OR u.active = :active)
           """)
    Page<User> search(@Param("search") String search,
                      @Param("roleName") String roleName,
                      @Param("active") Boolean active,
                      Pageable pageable);
}
