package com.madeby.repository;

import com.madeby.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByNumber(String number);
    Optional<User> findByEmailHash(String emailHash);

    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdWithoutOrders(@Param("id") Long id);
}
