package com.sgitu.userservice.repository;

import com.sgitu.userservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByRolesName(String roleName);

    @Query("SELECT u.id FROM User u JOIN u.roles r WHERE r.name = :roleName")
    List<Long> findIdsByRolesName(@Param("roleName") String roleName);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(value = "UPDATE users SET password = :password WHERE id = :id", nativeQuery = true)
    int updatePassword(@Param("id") Long id, @Param("password") String password);
}
