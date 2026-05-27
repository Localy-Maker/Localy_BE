package org.example.localy.common.exception.errorCode;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.localy.common.exception.model.BaseErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ShopErrorCode implements BaseErrorCode {

    ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "SHOP001", "존재하지 않는 아이템입니다."),
    ITEM_ALREADY_OWNED(HttpStatus.CONFLICT, "SHOP002", "이미 보유한 아이템입니다."),
    INSUFFICIENT_POINTS(HttpStatus.BAD_REQUEST, "SHOP003", "포인트가 부족합니다."),
    CHARACTER_NOT_FOUND(HttpStatus.NOT_FOUND, "SHOP004", "존재하지 않는 캐릭터입니다."),
    ITEM_NOT_OWNED(HttpStatus.FORBIDDEN, "SHOP005", "보유하지 않은 아이템은 착용할 수 없습니다."),
    INVALID_CATEGORY(HttpStatus.BAD_REQUEST, "SHOP006", "유효하지 않은 카테고리입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
