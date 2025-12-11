package org.example.localy.common.exception.errorCode;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.localy.common.exception.model.BaseErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum PlaceErrorCode implements BaseErrorCode {

    PLACE_NOT_FOUND(HttpStatus.NOT_FOUND, "PLACE001", "존재하지 않는 장소입니다."),
    LOCATION_REQUIRED(HttpStatus.BAD_REQUEST, "PLACE002", "위치 정보가 필요합니다."),
    BOOKMARK_NOT_FOUND(HttpStatus.NOT_FOUND, "PLACE003", "북마크를 찾을 수 없습니다."),
    TOUR_API_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "PLACE004", "관광정보 API 호출에 실패했습니다."),
    INVALID_SORT_TYPE(HttpStatus.BAD_REQUEST, "PLACE005", "잘못된 정렬 방식입니다."),
    EMOTION_DATA_NOT_FOUND(HttpStatus.NOT_FOUND, "PLACE006", "감정 데이터를 찾을 수 없습니다."),
    AI_RECOMMENDATION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "PLACE007", "AI 추천 요청에 실패했습니다."),
    PLACE_SAVE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "PLACE_001", "장소 저장에 실패했습니다");

    private final HttpStatus status;
    private final String code;
    private final String message;
}