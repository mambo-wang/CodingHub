package com.iaihub.toolbox.repository;

import com.iaihub.toolbox.model.AccountStatus;
import com.iaihub.toolbox.model.Role;
import com.iaihub.toolbox.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByNickname(String nickname);

    boolean existsByUsername(String username);

    boolean existsByNickname(String nickname);

    List<User> findByStatus(AccountStatus status);

    List<User> findByRole(Role role);

    List<User> findByStatusAndRole(AccountStatus status, Role role);

    @Query("SELECT u FROM User u WHERE " +
           "(:role IS NULL OR u.role = :role) AND " +
           "(:status IS NULL OR u.status = :status) AND " +
           "(:keyword IS NULL OR u.username LIKE %:keyword% OR u.nickname LIKE %:keyword%)")
    Page<User> findAllFiltered(@Param("role") Role role,
                               @Param("status") AccountStatus status,
                               @Param("keyword") String keyword,
                               Pageable pageable);
}
