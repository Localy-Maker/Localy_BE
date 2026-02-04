package org.example.localy.repository;

import org.example.localy.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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

    List<Users> findByLastLoginTimeBefore(LocalDateTime time);

    // lastAccessAt이 NULL인 유저도 포함하고 싶으면
    List<Users> findByLastLoginTimeIsNullOrLastLoginTimeBefore(LocalDateTime time);

}