package org.example.localy.util;

import java.util.HashMap;
import java.util.Map;

public class CategoryMapper {

    private static final Map<String, String> CONTENT_TYPE_MAP = new HashMap<>();
    private static final Map<String, String> CAT3_MAP = new HashMap<>();

    static {
        // ContentTypeId 매핑
        CONTENT_TYPE_MAP.put("12", "관광지");
        CONTENT_TYPE_MAP.put("14", "문화시설");
        CONTENT_TYPE_MAP.put("15", "축제");
        CONTENT_TYPE_MAP.put("25", "여행코스");
        CONTENT_TYPE_MAP.put("28", "레포츠");
        CONTENT_TYPE_MAP.put("32", "숙박");
        CONTENT_TYPE_MAP.put("38", "쇼핑");
        CONTENT_TYPE_MAP.put("39", "음식점");

        // Cat3 소분류 매핑 (일부 예시)
        CAT3_MAP.put("A05020900", "카페");
        CAT3_MAP.put("A05020100", "한식");
        CAT3_MAP.put("A05020200", "중식");
        CAT3_MAP.put("A05020300", "일식");
        CAT3_MAP.put("A05020400", "서양식");
        CAT3_MAP.put("A02060100", "박물관");
        CAT3_MAP.put("A02060200", "미술관");
        CAT3_MAP.put("A02060300", "전시관");
        CAT3_MAP.put("A02060400", "공연장");
    }

    public static String getCategoryName(String contentTypeId, String cat3) {
        if (cat3 != null && CAT3_MAP.containsKey(cat3)) {
            return CAT3_MAP.get(cat3);
        }
        return CONTENT_TYPE_MAP.getOrDefault(contentTypeId, "기타");
    }

    public static String getContentTypeName(String contentTypeId) {
        return CONTENT_TYPE_MAP.getOrDefault(contentTypeId, "기타");
    }
}
