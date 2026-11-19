package com.fadikazoura.nextrep.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCase(String username);

    /** Only the token version, without loading the whole user; checked on every authenticated request. */
    @Query("SELECT u.tokenVersion FROM User u WHERE u.id = :id")
    Optional<Integer> findTokenVersionById(@Param("id") Long id);
}
