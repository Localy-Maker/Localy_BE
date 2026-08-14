package org.example.localy.dto.place;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

// /contents/info(상세 조회) 응답 전용 DTO.
// 목록 API(/contents/list)는 data가 배열이지만, 상세 API는 data가 객체 하나로 온다.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class TourApiDetailDto {
    private TourApiDto.Data data;

    @JsonProperty("result_code")
    private Integer resultCode;

    @JsonProperty("result_message")
    private String resultMessage;
}
