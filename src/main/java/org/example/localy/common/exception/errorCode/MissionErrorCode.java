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
    POINT_DEDUCTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "MISSION007", "포인트 차감에 실패했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}