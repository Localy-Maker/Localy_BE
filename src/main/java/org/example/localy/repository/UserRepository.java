package org.example.localy.repository;

import jakarta.persistence.LockModeType;
import org.example.localy.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<Users, Long> {

    Optional<Users> findByEmail(String email);

    Optional<Users> findByNickname(String nickname);

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);

    Optional<Users> findByAuthProviderAndProviderId(Users.AuthProvider authProvider, String providerId);

    @Query("SELECT u.id FROM Users u")
    List<Long> findAllUserIds();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM Users u WHERE u.id = :id")
    Optional<Users> findByIdWithLock(Long id);
}