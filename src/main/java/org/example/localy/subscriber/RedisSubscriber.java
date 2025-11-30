package org.example.localy.subscriber;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class RedisSubscriber implements MessageListener {

    private final RedisMessageListenerContainer listenerContainer;
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public RedisSubscriber(RedisMessageListenerContainer listenerContainer,
                           SimpMessagingTemplate messagingTemplate) {
        this.listenerContainer = listenerContainer;
        this.messagingTemplate = messagingTemplate;
    }

    public void subscribe(String channel) {
        listenerContainer.addMessageListener(this, new ChannelTopic(channel));
    }

    public void unsubscribe(String channel) {
        listenerContainer.removeMessageListener(this, new ChannelTopic(channel));
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel());   // localy:chat:bot:{userId}
        String text = new String(message.getBody());

        String userId = channel.replace("localy:chat:bot:", "");
        String destination = "/topic/chat/" + userId;

        // JSON 형태로 변환
        Map<String, String> payload = Map.of(
                "sender", "BOT",
                "text", text
        );

        System.out.println("Sending to " + destination + ": " + text); // 🔹 서버 로그 확인용

        messagingTemplate.convertAndSend(destination, payload);
    }
}
