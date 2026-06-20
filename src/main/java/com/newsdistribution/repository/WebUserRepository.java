package com.newsdistribution.repository;

import com.newsdistribution.entity.WebUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface WebUserRepository extends JpaRepository<WebUser, Integer> {
    Optional<WebUser> findByUsername(String username);
    boolean existsByUsername(String username);

    @org.springframework.data.jpa.repository.Query("SELECT MAX(w.makh) FROM WebUser w WHERE w.makh LIKE 'DL%' AND LEN(w.makh) = 5")
    String findMaxMakh();
}
