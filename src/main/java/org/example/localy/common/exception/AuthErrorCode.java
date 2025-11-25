package org.example.localy.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.localy.common.exception.model.BaseErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AuthErrorCode implements BaseErrorCode {

    // 회원가입
    EMAIL_ALREADY_EXISTS("AUTH001", "이미 사용중인 이메일입니다.", HttpStatus.CONFLICT),
    NICKNAME_ALREADY_EXISTS("AUTH002", "이미 사용중인 닉네임입니다.", HttpStatus.CONFLICT),
    INVALID_PASSWORD_FORMAT("AUTH003", "비밀번호 형식이 올바르지 않습니다. (영문 소문자 8~16자, 특수문자 !#$%&*@^ 포함)", HttpStatus.BAD_REQUEST),
    PASSWORD_MISMATCH("AUTH004", "비밀번호가 일치하지 않습니다.", HttpStatus.BAD_REQUEST),

    // 이메일 인증
    EMAIL_VERIFICATION_CODE_EXPIRED("AUTH005", "이메일 인증번호가 만료되었습니다. 다시 요청해주세요.", HttpStatus.BAD_REQUEST),
    EMAIL_VERIFICATION_CODE_MISMATCH("AUTH006", "이메일 인증번호가 일치하지 않습니다.", HttpStatus.BAD_REQUEST),
    EMAIL_VERIFICATION_CODE_NOT_FOUND("AUTH007", "이메일 인증번호를 찾을 수 없습니다. 먼저 인증번호를 요청해주세요.", HttpStatus.BAD_REQUEST),
    EMAIL_SEND_FAILED("AUTH008", "이메일 전송에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),

    // 로그인
    USER_NOT_FOUND("AUTH009", "존재하지 않는 사용자입니다.", HttpStatus.NOT_FOUND),
    INVALID_CREDENTIALS("AUTH010", "이메일 또는 비밀번호가 올바르지 않습니다.", HttpStatus.UNAUTHORIZED),
    OAUTH_USER_EMAIL_MISMATCH("AUTH011", "해당 이메일은 소셜 로그인으로 가입된 계정입니다.", HttpStatus.BAD_REQUEST),
    LOGOUT_FAILED("AUTH013", "로그아웃 처리 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),

    // OAuth
    OAUTH_PROCESSING_FAILED("AUTH012", "소셜 로그인 처리 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_OAUTH_PROVIDER("AUTH013", "지원하지 않는 소셜 로그인 제공자입니다.", HttpStatus.BAD_REQUEST),

    // 온보딩
    ONBOARDING_ALREADY_COMPLETED("AUTH015", "이미 온보딩을 완료한 사용자입니다.",HttpStatus.BAD_REQUEST),
    ONBOARDING_SAVE_FAILED( "AUTH016", "온보딩 정보 저장 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    ONBOARDING_LOAD_FAILED( "AUTH017", "온보딩 정보 조회 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String message;
    private final HttpStatus status;
}
