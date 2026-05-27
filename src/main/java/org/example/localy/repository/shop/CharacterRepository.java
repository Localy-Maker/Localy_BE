package org.example.localy.repository.shop;

import org.example.localy.entity.shop.Character;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CharacterRepository extends JpaRepository<Character, Long> {

    boolean existsByCode(String code);

    Optional<Character> findByCode(String code);
}
