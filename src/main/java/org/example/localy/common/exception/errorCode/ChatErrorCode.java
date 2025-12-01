package org.example.localy.common.exception.errorCode;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.localy.common.exception.model.BaseErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ChatErrorCode implements BaseErrorCode {

    NOT_EMOTION_RESULTS(HttpStatus.NOT_FOUND, "E001", "최근 감정 결과가 존재하지 않습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}