package com.busantrip.service;

import com.busantrip.dto.response.PlaceResponse;
import com.busantrip.dto.response.TourismResponse;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * 외부 관광 API 키를 준비하기 전에도 전체 사용자 흐름을 시연할 수 있도록 제공하는
 * 부산 대표 관광지 카탈로그다. 외부 API가 준비되면 TourismService가 실시간 결과를 우선한다.
 */
@Component
public class DemoTourismCatalog {

    private static final List<PlaceResponse> PLACES = List.of(
            place("해운대해수욕장", "해운대구", "자연관광", "바다", 35.1587, 129.1604, 1),
            place("광안리해수욕장", "수영구", "자연관광", "야경", 35.1532, 129.1186, 2),
            place("감천문화마을", "사하구", "문화관광", "문화", 35.0975, 129.0106, 3),
            place("자갈치시장", "중구", "시장", "먹거리", 35.0968, 129.0302, 4),
            place("흰여울문화마을", "영도구", "문화관광", "산책", 35.0788, 129.0410, 5),
            place("송도해상케이블카", "서구", "체험", "바다", 35.0778, 129.0168, 6),
            place("태종대", "영도구", "자연관광", "전망", 35.0516, 129.0872, 7),
            place("용두산공원", "중구", "공원", "야경", 35.1007, 129.0322, 8),
            place("F1963", "수영구", "문화관광", "카페", 35.1712, 129.1254, 9),
            place("오륙도 스카이워크", "남구", "체험", "전망", 35.0995, 129.1220, 10),
            place("부산시민공원", "부산진구", "공원", "산책", 35.1682, 129.0556, 11),
            place("다대포해수욕장", "사하구", "자연관광", "노을", 35.0483, 128.9657, 12)
    );

    private static final Map<String, Set<String>> THEME_TERMS = Map.of(
            "해수욕장", Set.of("바다", "해수욕장", "해운대", "광안리", "다대포"),
            "시장", Set.of("시장", "먹거리", "자갈치"),
            "카페", Set.of("카페", "f1963", "조용한"),
            "문화", Set.of("문화", "감천", "흰여울", "전시"),
            "체험", Set.of("체험", "케이블카", "스카이워크"),
            "공원", Set.of("공원", "산책", "걷기"),
            "야경", Set.of("야경", "노을", "밤", "전망")
    );

    public TourismResponse search(String keyword, int pageNo, int numOfRows) {
        String normalized = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        List<PlaceResponse> matched = PLACES.stream()
                .filter(place -> matches(place, normalized))
                .toList();

        List<PlaceResponse> selected = matched.isEmpty() ? PLACES : matched;
        int safePage = Math.max(pageNo, 1);
        int safeRows = Math.max(1, Math.min(numOfRows, 20));
        int fromIndex = Math.min((safePage - 1) * safeRows, selected.size());
        int toIndex = Math.min(fromIndex + safeRows, selected.size());
        return new TourismResponse(selected.size(), safePage, safeRows, selected.subList(fromIndex, toIndex), "demo");
    }

    /**
     * 일정 생성기는 테마를 조합해 여러 날짜의 코스를 만들기 때문에 전체 좌표 카탈로그가
     * 필요하다. 반환값은 불변 리스트라 호출자가 데모 원본을 변경할 수 없다.
     */
    public List<PlaceResponse> all() {
        return PLACES;
    }

    private boolean matches(PlaceResponse place, String keyword) {
        if (keyword.isBlank() || keyword.equals("부산") || keyword.equals("추천")) {
            return true;
        }

        String searchable = String.join(" ",
                value(place.name()), value(place.district()), value(place.category()), value(place.subCategory()))
                .toLowerCase(Locale.ROOT);
        if (searchable.contains(keyword)) {
            return true;
        }

        return THEME_TERMS.getOrDefault(keyword, Set.of()).stream().anyMatch(searchable::contains);
    }

    private static PlaceResponse place(
            String name,
            String district,
            String category,
            String subCategory,
            double latitude,
            double longitude,
            int rank
    ) {
        return new PlaceResponse(name, district, category, subCategory, latitude, longitude, rank);
    }

    private String value(String input) {
        return input == null ? "" : input;
    }
}
