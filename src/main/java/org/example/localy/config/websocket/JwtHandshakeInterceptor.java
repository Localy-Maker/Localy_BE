package org.example.localy.config.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.localy.util.JwtUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attrs) {

        String path = request.getURI().getPath();
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        log.info("authHeader:{}", authHeader);

        log.info("[Handshake] Path={}, Authorization Header={}", path, authHeader); // 🔹 추가

        // SockJS fallback은 인증 없이 통과
        if (path.endsWith("/info") || path.contains("/iframe.html")) {
            return true;
        }

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            try {
                jwtUtil.validateToken(token);
                Long userId = jwtUtil.getUserIdFromToken(token);
                String email = jwtUtil.getEmailFromToken(token);

                attrs.put("userId", userId);
                attrs.put("email", email);
                attrs.put("jwt", token);

                log.info("[Handshake] JWT 검증 성공 userId={}, email={}", userId, email);
                log.info("[Handshake] attrs now: {}", attrs); // 🔹 추가
            } catch (Exception e) {
                log.warn("[Handshake] JWT 검증 실패: {}", e.getMessage());
                return false;
            }
        } else {
            log.warn("[Handshake] JWT 없음 → 연결은 허용, CONNECT 단계에서 처리 필요");
            log.info("[Handshake] attrs at JWT 없음: {}", attrs); // 🔹 추가
        }

        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        log.info("[Handshake] afterHandshake 호출, exception={}", exception); // 🔹 추가
    }
}
