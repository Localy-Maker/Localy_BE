package org.example.localy.repository;

import org.example.localy.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatBotRepository extends JpaRepository<ChatMessage, Long> {

}
