package org.example.localy.repository.shop;

import org.example.localy.entity.Users;
import org.example.localy.entity.shop.UserCharacterState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserCharacterStateRepository extends JpaRepository<UserCharacterState, Long> {

    Optional<UserCharacterState> findByUserAndCharacterCode(Users user, String characterCode);
}
