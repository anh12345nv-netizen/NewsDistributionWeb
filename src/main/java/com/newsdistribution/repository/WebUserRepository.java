package com.newsdistribution.repository;

import com.newsdistribution.entity.WebUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface WebUserRepository extends JpaRepository<WebUser, Integer> {
    Optional<WebUser> findByUsername(String username);
    boolean existsByUsername(String username);
}
