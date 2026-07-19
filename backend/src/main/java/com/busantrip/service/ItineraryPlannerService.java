package com.busantrip.service;

import com.busantrip.client.GemmaClient;
import com.busantrip.config.ApiKeyProperties;
import com.busantrip.controller.RouteController.Coordinate;
import com.busantrip.controller.RouteController.Place;
import com.busantrip.controller.RouteController.RouteRequest;
import com.busantrip.dto.itinerary.ItineraryAdjustmentRequest;
import com.busantrip.dto.itinerary.ItineraryDay;
import com.busantrip.dto.itinerary.ItineraryRequest;
import com.busantrip.dto.itinerary.ItineraryResponse;
import com.busantrip.dto.itinerary.ItineraryStop;
import com.busantrip.dto.itinerary.TravelContext;
import com.busantrip.dto.response.PlaceResponse;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.IntStream;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * 일정 순서는 검증 가능한 규칙과 경로 최적화로 결정하고, Gemma는 사용자의 조건을
 * 설명 가능한 여행 문장으로 해석한다. 따라서 모델 응답 실패 시에도 일정 자체는 항상 생성된다.
 */
@Service
public class ItineraryPlannerService {

    private static final String GEMMA_INSTRUCTION = """
            당신은 부산 여행 서비스 ROAMATE의 일정 설계 메이트입니다.
            주어진 여행 조건, 방문 순서, 날씨와 교통 상태만 근거로 2~3문장의 한국어 일정 요약을 작성하세요.
            제공되지 않은 영업시간, 실제 날씨, 교통 상황을 사실처럼 만들지 마세요.
            마크다운 없이 바로 사용자에게 보여 줄 문장만 답하세요.
            """;
    private static final Map<String, PlaceProfile> PROFILES = profiles();

    private final TourismService tourismService;
    private final DemoTourismCatalog demoTourismCatalog;
    private final RouteOptimizationService routeOptimizationService;
    private final TravelContextService travelContextService;
    private final GemmaClient gemmaClient;
    private final ApiKeyProperties apiKeyProperties;

    public ItineraryPlannerService(
            TourismService tourismService,
            DemoTourismCatalog demoTourismCatalog,
            RouteOptimizationService routeOptimizationService,
            TravelContextService travelContextService,
            GemmaClient gemmaClient,
            ApiKeyProperties apiKeyProperties
    ) {
        this.tourismService = tourismService;
        this.demoTourismCatalog = demoTourismCatalog;
        this.routeOptimizationService = routeOptimizationService;
        this.travelContextService = travelContextService;
        this.gemmaClient = gemmaClient;
        this.apiKeyProperties = apiKeyProperties;
    }

    public Mono<ItineraryResponse> plan(ItineraryRequest request) {
        validate(request);
        return build(request, "");
    }

    public Mono<ItineraryResponse> adjust(ItineraryAdjustmentRequest request) {
        validate(request.request());
        return build(request.request(), request.adjustment().trim());
    }

    private Mono<ItineraryResponse> build(ItineraryRequest request, String adjustment) {
        return Mono.zip(loadCandidates(), travelContextService.load(request))
                .flatMap(values -> {
                    List<ItineraryStop> selected = selectStops(request, adjustment, values.getT1());
                    TravelContext context = values.getT2();
                    RouteRequest routeRequest = new RouteRequest(
                            new Coordinate(request.start().latitude(), request.start().longitude()),
                            selected.stream().map(this::toRoutePlace).toList(),
                            "trafast");
                    return routeOptimizationService.optimize(routeRequest)
                            .flatMap(route -> {
                                Map<String, ItineraryStop> stopsById = selected.stream()
                                        .collect(java.util.stream.Collectors.toMap(
                                                ItineraryStop::id,
                                                stop -> stop,
                                                (left, right) -> left,
                                                LinkedHashMap::new));
                                List<ItineraryStop> ordered = route.orderedPlaces().stream()
                                        .map(place -> stopsById.get(place.id()))
                                        .filter(Objects::nonNull)
                                        .toList();
                                List<ItineraryDay> days = splitDays(request, ordered);
                                return narrative(request, adjustment, days, context)
                                        .map(narrative -> new ItineraryResponse(
                                                title(request, adjustment),
                                                narrative.text(),
                                                narrative.source(),
                                                context,
                                                days,
                                                ordered,
                                                route,
                                                tips(request, adjustment, context)
                                        ));
                            });
                });
    }

    private Mono<List<PlaceResponse>> loadCandidates() {
        String baseYm = YearMonth.now().toString().replace("-", "");
        return tourismService.getLocalTourism(baseYm, "26350", 1, 20)
                .map(response -> response.places().isEmpty() ? demoTourismCatalog.all() : response.places());
    }

    private List<ItineraryStop> selectStops(
            ItineraryRequest request,
            String adjustment,
            List<PlaceResponse> candidates
    ) {
        int days = travelDays(request);
        int limit = stopLimit(request, adjustment, days);
        List<ScoredPlace> scored = candidates.stream()
                .filter(place -> place.latitude() != null && place.longitude() != null)
                .map(place -> new ScoredPlace(place, score(place, request, adjustment)))
                .sorted(Comparator.comparingInt(ScoredPlace::score).reversed()
                        .thenComparing(place -> value(place.place().name())))
                .toList();

        if (containsAny(adjustment.toLowerCase(Locale.ROOT), "비", "실내", "우산")) {
            List<PlaceResponse> indoorPlaces = scored.stream()
                    .map(ScoredPlace::place)
                    .filter(place -> profile(place.name()).indoor())
                    .limit(Math.max(1, days * 2L))
                    .toList();
            if (!indoorPlaces.isEmpty()) {
                return IntStream.range(0, indoorPlaces.size())
                        .mapToObj(index -> toStop(indoorPlaces.get(index), index))
                        .toList();
            }
        }

        List<PlaceResponse> picked = new ArrayList<>();
        Set<String> categories = new java.util.HashSet<>();
        for (ScoredPlace candidate : scored) {
            String category = value(candidate.place().subCategory());
            if (picked.size() < limit && (categories.add(category) || picked.size() >= Math.max(2, limit - 2))) {
                picked.add(candidate.place());
            }
        }
        if (picked.size() < Math.min(limit, scored.size())) {
            scored.stream().map(ScoredPlace::place).filter(place -> !picked.contains(place))
                    .limit(limit - picked.size()).forEach(picked::add);
        }
        if (picked.isEmpty()) {
            picked.addAll(demoTourismCatalog.all().stream().limit(Math.min(limit, 4)).toList());
        }

        return IntStream.range(0, picked.size())
                .mapToObj(index -> toStop(picked.get(index), index))
                .toList();
    }

    private int score(PlaceResponse place, ItineraryRequest request, String adjustment) {
        String text = (value(place.name()) + " " + value(place.district()) + " "
                + value(place.category()) + " " + value(place.subCategory())).toLowerCase(Locale.ROOT);
        PlaceProfile profile = profile(place.name());
        int score = 10;
        for (String theme : request.themes()) {
            if (themeMatches(text, theme)) {
                score += 15;
            }
        }
        String companion = request.companion().toLowerCase(Locale.ROOT);
        if (companion.contains("연인") && (profile.night() || profile.cafe())) score += 7;
        if (companion.contains("아이") && (profile.indoor() || text.contains("공원") || text.contains("체험"))) score += 7;
        if (companion.contains("혼자") && (profile.cafe() || text.contains("문화") || text.contains("산책"))) score += 5;

        String normalizedAdjustment = adjustment.toLowerCase(Locale.ROOT);
        if (containsAny(normalizedAdjustment, "비", "실내", "우산") && profile.indoor()) score += 30;
        if (containsAny(normalizedAdjustment, "비", "실내", "우산") && !profile.indoor()) score -= 40;
        if (containsAny(normalizedAdjustment, "카페", "휴식") && profile.cafe()) score += 25;
        if (containsAny(normalizedAdjustment, "야경", "밤", "노을") && profile.night()) score += 25;
        if (containsAny(normalizedAdjustment, "걷", "피곤", "택시", "줄") && profile.stayMinutes() > 100) score -= 5;
        return score;
    }

    private boolean themeMatches(String text, String theme) {
        return switch (theme.trim().toLowerCase(Locale.ROOT)) {
            case "바다" -> containsAny(text, "바다", "해수욕장", "해운대", "광안리", "송도", "다대포");
            case "야경", "노을" -> containsAny(text, "야경", "전망", "노을", "광안리", "용두산", "다대포");
            case "카페" -> containsAny(text, "카페", "f1963");
            case "문화" -> containsAny(text, "문화", "감천", "흰여울", "f1963");
            case "시장", "먹거리" -> containsAny(text, "시장", "먹거리", "자갈치");
            case "체험" -> containsAny(text, "체험", "케이블카", "스카이워크");
            case "산책", "공원" -> containsAny(text, "산책", "공원", "흰여울");
            default -> text.contains(theme.trim().toLowerCase(Locale.ROOT));
        };
    }

    private ItineraryStop toStop(PlaceResponse place, int index) {
        PlaceProfile profile = profile(place.name());
        String category = value(place.subCategory()).isBlank() ? value(place.category()) : value(place.subCategory());
        return new ItineraryStop(
                "stop-" + (index + 1) + "-" + slug(place.name()),
                value(place.name()),
                valueOr(place.district(), "부산"),
                valueOr(category, "부산 여행"),
                profile.description(),
                profile.image(),
                place.latitude(),
                place.longitude(),
                "",
                profile.stayMinutes(),
                profile.indoor(),
                profile.reason()
        );
    }

    private Place toRoutePlace(ItineraryStop stop) {
        return new Place(stop.id(), stop.name(), stop.description(), stop.district(), stop.image(),
                stop.category(), "", stop.latitude(), stop.longitude());
    }

    private List<ItineraryDay> splitDays(ItineraryRequest request, List<ItineraryStop> ordered) {
        int dayCount = travelDays(request);
        int perDay = Math.max(1, (int) Math.ceil((double) ordered.size() / dayCount));
        List<ItineraryDay> days = new ArrayList<>();
        for (int day = 0; day < dayCount; day++) {
            int from = Math.min(day * perDay, ordered.size());
            int to = Math.min(from + perDay, ordered.size());
            List<ItineraryStop> scheduled = new ArrayList<>();
            int minutes = 10 * 60;
            for (ItineraryStop stop : ordered.subList(from, to)) {
                scheduled.add(new ItineraryStop(
                        stop.id(), stop.name(), stop.district(), stop.category(), stop.description(), stop.image(),
                        stop.latitude(), stop.longitude(), formatTime(minutes), stop.stayMinutes(), stop.indoor(), stop.reason()));
                minutes += stop.stayMinutes() + 25;
            }
            LocalDate date = request.startDate().plusDays(day);
            String headline = scheduled.isEmpty() ? "휴식과 자유 일정" : scheduled.getFirst().category() + " 중심 코스";
            days.add(new ItineraryDay(day + 1, date, headline, List.copyOf(scheduled)));
        }
        return List.copyOf(days);
    }

    private Mono<Narrative> narrative(
            ItineraryRequest request,
            String adjustment,
            List<ItineraryDay> days,
            TravelContext context
    ) {
        if (!configured(apiKeyProperties.getGemma().getApiKey())) {
            return Mono.just(new Narrative(fallbackNarrative(request, adjustment, days, context), "guided-demo"));
        }
        String prompt = "여행 조건: " + request
                + "\n변경 요청: " + (adjustment.isBlank() ? "없음" : adjustment)
                + "\n일정: " + days
                + "\n여행 데이터: " + context;
        return Mono.defer(() -> gemmaClient.generate(GEMMA_INSTRUCTION, prompt))
                .map(text -> new Narrative(trimTo(text, 420), "gemma"))
                .onErrorReturn(new Narrative(fallbackNarrative(request, adjustment, days, context), "guided-demo"));
    }

    private String fallbackNarrative(
            ItineraryRequest request,
            String adjustment,
            List<ItineraryDay> days,
            TravelContext context
    ) {
        int count = days.stream().mapToInt(day -> day.stops().size()).sum();
        String base = request.themes().stream().limit(2).reduce((left, right) -> left + " · " + right).orElse("부산");
        String changed = adjustment.isBlank() ? "" : " 요청하신 ‘" + adjustment + "’ 조건을 반영해";
        return base + " 취향으로 " + count + "곳을 연결했습니다." + changed
                + " " + context.weather().label() + "과 " + context.traffic().label()
                + "은 출발 전 한 번 더 확인하면 더 편안하게 여행할 수 있어요.";
    }

    private List<String> tips(ItineraryRequest request, String adjustment, TravelContext context) {
        List<String> tips = new ArrayList<>();
        tips.add("장소 카드를 누르면 지도에서 해당 지점을 바로 확인할 수 있어요.");
        if (!adjustment.isBlank()) {
            tips.add("현재 변경 요청을 반영한 새 순서입니다. 경로는 다시 계산됐어요.");
        }
        if (!context.weather().live()) {
            tips.add("발표 환경에서는 날씨 API 없이 동작합니다. 실제 여행 때는 기상청 키를 연결하세요.");
        }
        if (request.transport().contains("대중")) {
            tips.add("대중교통 이용 시 막차와 환승 정보는 출발 전에 확인하세요.");
        }
        return List.copyOf(tips);
    }

    private void validate(ItineraryRequest request) {
        if (request.endDate().isBefore(request.startDate())) {
            throw new IllegalArgumentException("여행 종료일은 시작일보다 빠를 수 없습니다.");
        }
        if (travelDays(request) > 3) {
            throw new IllegalArgumentException("발표용 일정은 최대 3일까지 생성할 수 있습니다.");
        }
    }

    private int travelDays(ItineraryRequest request) {
        return (int) (request.endDate().toEpochDay() - request.startDate().toEpochDay()) + 1;
    }

    private int stopLimit(ItineraryRequest request, String adjustment, int days) {
        int perDay = request.pace().contains("알차") ? 4 : 3;
        if (containsAny(adjustment.toLowerCase(Locale.ROOT), "걷", "피곤", "택시", "줄")) perDay = 2;
        return Math.min(days * perDay, 8);
    }

    private String title(ItineraryRequest request, String adjustment) {
        String themes = request.themes().stream().limit(2).reduce((left, right) -> left + " · " + right).orElse("부산");
        return adjustment.isBlank() ? themes + " 부산 여행" : "변경된 " + themes + " 부산 여행";
    }

    private PlaceProfile profile(String name) {
        return PROFILES.getOrDefault(value(name), PlaceProfile.defaultProfile());
    }

    private static Map<String, PlaceProfile> profiles() {
        Map<String, PlaceProfile> profiles = new HashMap<>();
        profiles.put("해운대해수욕장", new PlaceProfile("바다를 보며 여유롭게 시작하기 좋은 대표 해변이에요.", image(0), false, false, false, 90, "바다 테마와 가까운 위치를 반영했어요."));
        profiles.put("광안리해수욕장", new PlaceProfile("광안대교 전망을 함께 즐길 수 있는 해변이에요.", image(1), false, true, false, 100, "야경과 바다 테마에 잘 어울려요."));
        profiles.put("감천문화마을", new PlaceProfile("골목 풍경과 색감 있는 문화 산책을 즐길 수 있어요.", image(2), false, false, false, 100, "문화 테마의 대표 방문지예요."));
        profiles.put("자갈치시장", new PlaceProfile("시장 분위기와 부산의 먹거리를 함께 느낄 수 있어요.", image(3), true, false, false, 80, "날씨 변수 때도 비교적 머무르기 좋은 선택이에요."));
        profiles.put("흰여울문화마을", new PlaceProfile("바다를 따라 걷는 골목 풍경이 인상적인 곳이에요.", image(0), false, false, false, 90, "문화와 산책 취향을 함께 반영했어요."));
        profiles.put("송도해상케이블카", new PlaceProfile("송도 바다를 위에서 바라보는 체험형 장소예요.", image(1), false, false, false, 80, "체험과 바다 테마를 반영했어요."));
        profiles.put("태종대", new PlaceProfile("절벽과 바다 풍경을 즐기는 자연 관광지예요.", image(2), false, false, false, 110, "전망과 자연 취향을 반영했어요."));
        profiles.put("용두산공원", new PlaceProfile("도심 가까이에서 부산 전경을 볼 수 있는 공원이에요.", image(3), false, true, false, 70, "야경과 짧은 산책에 적합해요."));
        profiles.put("F1963", new PlaceProfile("책·전시·카페를 한곳에서 쉬어 갈 수 있는 문화 공간이에요.", image(0), true, false, true, 90, "실내 휴식과 카페 요청을 반영했어요."));
        profiles.put("오륙도 스카이워크", new PlaceProfile("바다 위 전망을 가까이에서 즐기는 체험 장소예요.", image(1), false, false, false, 60, "체험과 전망 테마에 잘 맞아요."));
        profiles.put("부산시민공원", new PlaceProfile("도심에서 쉬어 가기 좋은 넓은 공원이에요.", image(2), false, false, false, 70, "가벼운 산책과 휴식에 적합해요."));
        profiles.put("다대포해수욕장", new PlaceProfile("넓은 해변과 노을 풍경을 즐길 수 있어요.", image(3), false, true, false, 100, "노을과 야경 테마의 마무리 장소예요."));
        return Map.copyOf(profiles);
    }

    private static String image(int index) {
        return List.of(
                "https://images.unsplash.com/photo-1517154421773-0529f29ea451?auto=format&fit=crop&w=1000&q=80",
                "https://images.unsplash.com/photo-1588416936097-41850ab3d86d?auto=format&fit=crop&w=1000&q=80",
                "https://images.unsplash.com/photo-1534274988757-a28bf1a57c17?auto=format&fit=crop&w=1000&q=80",
                "https://images.unsplash.com/photo-1470252649378-9c29740c9fa8?auto=format&fit=crop&w=1000&q=80"
        ).get(index);
    }

    private static boolean containsAny(String value, String... terms) {
        for (String term : terms) {
            if (value.contains(term)) return true;
        }
        return false;
    }

    private static String slug(String value) {
        return value == null ? "place" : value.replaceAll("[^A-Za-z0-9가-힣]", "").toLowerCase(Locale.ROOT);
    }

    private static String formatTime(int minutes) {
        return "%02d:%02d".formatted(minutes / 60, minutes % 60);
    }

    private static String trimTo(String value, int limit) {
        String normalized = value == null ? "" : value.trim();
        return normalized.length() <= limit ? normalized : normalized.substring(0, limit) + "…";
    }

    private static boolean configured(String value) {
        return value != null && !value.isBlank();
    }

    private static String value(String value) {
        return value == null ? "" : value;
    }

    private static String valueOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private record ScoredPlace(PlaceResponse place, int score) {
    }

    private record Narrative(String text, String source) {
    }

    private record PlaceProfile(
            String description,
            String image,
            boolean indoor,
            boolean night,
            boolean cafe,
            int stayMinutes,
            String reason
    ) {
        private static PlaceProfile defaultProfile() {
            return new PlaceProfile(
                    "부산 여행 동선에 맞춰 추천한 장소예요.",
                    ItineraryPlannerService.image(0),
                    false,
                    false,
                    false,
                    80,
                    "선택한 테마와 동선을 반영했어요."
            );
        }
    }
}
