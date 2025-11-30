package org.example.localy.common.exception.errorCode;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.localy.common.exception.model.BaseErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ChatErrorCode implements BaseErrorCode {

    NOT_(HttpStatus.NOT_FOUND, "PLACE001", "존재하지 않는 장소입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}