package triB.triB.schedule.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import triB.triB.schedule.dto.RouteRequest;
import triB.triB.schedule.dto.RouteResponse;
import triB.triB.schedule.entity.TravelMode;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoutesApiService {

    @Qualifier("googleRoutesWebClient")
    private final WebClient googleRoutesWebClient;

    /**
     * Google Routes API를 사용하여 두 지점 간의 이동 시간을 계산합니다.
     *
     * @param fromLat 출발지 위도
     * @param fromLng 출발지 경도
     * @param toLat 목적지 위도
     * @param toLng 목적지 경도
     * @param travelMode 이동 수단
     * @return 이동 시간 (분 단위), 실패 시 0
     */
    public Integer calculateTravelTime(Double fromLat, Double fromLng, Double toLat, Double toLng, TravelMode travelMode) {
        // RouteRequest 생성
        RouteRequest request = RouteRequest.builder()
                .origin(RouteRequest.LocationWrapper.builder()
                        .location(RouteRequest.Location.builder()
                                .latLng(RouteRequest.LatLng.builder()
                                        .latitude(fromLat)
                                        .longitude(fromLng)
                                        .build())
                                .build())
                        .build())
                .destination(RouteRequest.LocationWrapper.builder()
                        .location(RouteRequest.Location.builder()
                                .latLng(RouteRequest.LatLng.builder()
                                        .latitude(toLat)
                                        .longitude(toLng)
                                        .build())
                                .build())
                        .build())
                .travelMode(travelMode != null ? travelMode.name() : TravelMode.DRIVE.name())
                .languageCode("ko")
                .build();

        try {
            // Google Routes API 호출
            RouteResponse response = googleRoutesWebClient.post()
                    .uri("/directions/v2:computeRoutes")
                    .header("X-Goog-FieldMask", "routes.duration,routes.distanceMeters")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(RouteResponse.class)
                    .block();

            // 응답에서 duration 추출
            if (response != null && response.getRoutes() != null && !response.getRoutes().isEmpty()) {
                String duration = response.getRoutes().get(0).getDuration();
                Integer minutes = parseDurationToMinutes(duration);
                return minutes;
            }

            return 0;

        } catch (Exception e) {
            log.error("Google Routes API 호출 실패: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * ISO 8601 duration 문자열을 분 단위로 변환합니다.
     * 예: "PT30M" -> 30, "PT1H30M" -> 90, "PT45S" -> 0
     *
     * @param isoDuration ISO 8601 duration 문자열
     * @return 분 단위 시간
     */
    private Integer parseDurationToMinutes(String isoDuration) {
        if (isoDuration == null || isoDuration.isEmpty()) {
            return 0;
        }

        try {
            java.time.Duration duration = java.time.Duration.parse(isoDuration);
            return (int) duration.toMinutes();
        } catch (Exception e) {
            log.error("Failed to parse duration: {}", isoDuration, e);
            return 0;
        }
    }

    /**
     * 분 단위 시간을 한국어 형식으로 변환합니다.
     * 예: 30 -> "30분", 90 -> "1시간 30분", 120 -> "2시간"
     *
     * @param minutes 분 단위 시간
     * @return 한국어 형식 문자열
     */
    public String formatMinutesToReadable(Integer minutes) {
        if (minutes == null || minutes == 0) {
            return "0분";
        }

        int hours = minutes / 60;
        int remainingMinutes = minutes % 60;

        if (hours > 0 && remainingMinutes > 0) {
            return hours + "시간 " + remainingMinutes + "분";
        } else if (hours > 0) {
            return hours + "시간";
        } else {
            return remainingMinutes + "분";
        }
    }
}
