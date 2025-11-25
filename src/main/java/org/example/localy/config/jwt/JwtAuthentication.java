package org.example.localy.config.jwt;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class JwtAuthentication {
    private Long userId;
    private String email;
}
