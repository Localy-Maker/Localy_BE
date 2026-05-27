package org.example.localy.common.exception.errorCode;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.localy.common.exception.model.BaseErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum PremiumErrorCode implements BaseErrorCode {

    PLAN_NOT_FOUND(HttpStatus.BAD_REQUEST, "PREMIUM001", "존재하지 않는 플랜입니다."),
    INSUFFICIENT_POINTS(HttpStatus.BAD_REQUEST, "PREMIUM002", "포인트가 부족합니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
