package org.example.localy.dto.place;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class TourApiDto {
    private List<Data> data;

    // Paging 정보
    private Paging paging;

    @JsonProperty("result_code")
    private Integer resultCode;

    @JsonProperty("result_message")
    private String resultMessage;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Data {
        private String cid;

        @JsonProperty("lang_code_id")
        private String langCodeId;

        @JsonProperty("com_ctgry_sn")
        private String comCtgrySn;

        @JsonProperty("cate_depth")
        private String cate_depth;

        @JsonProperty("multi_lang_list")
        private String multiLangList;

        @JsonProperty("main_img")
        private String main_img;

        @JsonProperty("relate_img")
        private List<String> relate_img;

        @JsonProperty("post_sj")
        private String post_sj;

        private String sumry;

        @JsonProperty("creat_dt_text")
        private String creatDtText;

        @JsonProperty("updt_dt_text")
        private String updtDtText;

        private List<String> tag;

        @JsonProperty("post_desc")
        private String post_desc;

        private Extra extra;
        private Traffic traffic;
        private Tourist tourist;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Extra {
        @JsonProperty("cmmn_telno")
        private String cmmn_telno;

        @JsonProperty("cmmn_hmpg_url")
        private String cmmn_hmpg_url;

        @JsonProperty("cmmn_use_time")
        private String cmmn_use_time;

        @JsonProperty("trrsrt_use_chrge")
        private String trrsrtUseChrge;

        @JsonProperty("disabled_facility")
        private List<String> disabledFacility;

        @JsonProperty("closed_days")
        private String closedDays;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Traffic {
        private String adres;

        @JsonProperty("new_zip_code")
        private String newZipCode;

        @JsonProperty("new_adres")
        private String new_adres; // 도로명 주소

        // VisitSeoul API: x는 경도(Longitude), y는 위도(Latitude)
        @JsonProperty("map_position_x")
        private String map_position_x;

        @JsonProperty("map_position_y")
        private String map_position_y;

        @JsonProperty("subway_info")
        private String subwayInfo;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Tourist {
        @JsonProperty("guidance_service")
        private String guidanceService;

        @JsonProperty("safe_mng")
        private String safeMng;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Paging {
        @JsonProperty("page_no")
        private Integer pageNo;

        @JsonProperty("page_size")
        private Integer pageSize;

        @JsonProperty("total_count")
        private Integer totalCount;
    }
}