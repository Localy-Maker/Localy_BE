package org.example.localy.dto.place;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.List;

public class TourApiDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Response<T> {
        private ResponseHeader header;
        private ResponseBody<T> body;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ResponseHeader {
        private String resultCode;
        private String resultMsg;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ResponseBody<T> {
        private Integer numOfRows;
        private Integer pageNo;
        private Integer totalCount;
        private Items<T> items;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Items<T> {
        private List<T> item;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LocationBasedItem {
        private String contentid;
        private String contenttypeid;
        private String title;
        private String addr1;
        private String addr2;
        private String mapx;
        private String mapy;
        private String firstimage;
        private String firstimage2;
        private String tel;
        private String dist;
        private String cat1;
        private String cat2;
        private String cat3;
        private String areacode;
        private String sigungucode;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CommonItem {
        private String contentid;
        private String contenttypeid;
        private String title;
        private String addr1;
        private String addr2;
        private String zipcode;
        private String tel;
        private String homepage;
        private String overview;
        private String firstimage;
        private String firstimage2;
        private String mapx;
        private String mapy;
        private String cat1;
        private String cat2;
        private String cat3;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class IntroItem {
        private String contentid;
        private String contenttypeid;

        // 관광지 (12)
        private String infocenter;
        private String restdate;
        private String usetime;
        private String parking;
        private String chkpet;

        // 문화시설 (14)
        private String infocenterculture;
        private String restdateculture;
        private String usetimeculture;
        private String parkingculture;
        private String parkingfee;
        private String chkpetculture;

        // 음식점/카페 (39)
        private String infocenterfood;
        private String restdatefood;
        private String opentimefood;
        private String parkingfood;
        private String firstmenu;
        private String treatmenu;

        // 쇼핑 (38)
        private String infocentershopping;
        private String opentime;
        private String restdateshopping;
        private String parkingshopping;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ImageItem {
        private String contentid;
        private String originimgurl;
        private String smallimageurl;
        private String serialnum;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AreaCodeItem {
        private String code;
        private String name;
        private String rnum;
    }
}
