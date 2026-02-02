package org.example.localy.common.exception.errorCode;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.localy.common.exception.model.BaseErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum MissionErrorCode implements BaseErrorCode {

    MISSION_NOT_FOUND(HttpStatus.NOT_FOUND, "MISSION001", "존재하지 않는 미션입니다."),
    MISSION_ALREADY_COMPLETED(HttpStatus.BAD_REQUEST, "MISSION002", "이미 완료된 미션입니다."),
    MISSION_EXPIRED(HttpStatus.BAD_REQUEST, "MISSION003", "만료된 미션입니다."),
    MISSION_NOT_OWNER(HttpStatus.FORBIDDEN, "MISSION004", "본인의 미션만 인증할 수 있습니다."),
    LOCATION_TOO_FAR(HttpStatus.BAD_REQUEST, "MISSION005", "장소로부터 너무 멀리 떨어져 있습니다. (50m 이내 필요)"),
    LOCATION_UNAVAILABLE(HttpStatus.BAD_REQUEST, "MISSION006", "현재 위치를 확인할 수 없습니다."),
    POINT_DEDUCTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "MISSION007", "포인트 차감에 실패했습니다."),
    DATE_MISMATCH(HttpStatus.BAD_REQUEST, "MISSION008", "사진의 촬영 날짜가 선택한 날짜와 일치하지 않습니다."),
    DATE_OUT_OF_RANGE(HttpStatus.BAD_REQUEST, "MISSION009", "사진 저장 날짜가 미션 수행 기간 범위를 벗어났습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}