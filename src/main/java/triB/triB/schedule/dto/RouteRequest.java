package triB.triB.schedule.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Google Routes API 경로 계산 요청")
public class RouteRequest {

    @JsonProperty("origin")
    @Schema(description = "출발지 정보")
    private LocationWrapper origin;

    @JsonProperty("destination")
    @Schema(description = "목적지 정보")
    private LocationWrapper destination;

    @JsonProperty("travelMode")
    @Schema(description = "이동 수단", example = "DRIVE")
    private String travelMode;

    @JsonProperty("languageCode")
    @Schema(description = "언어 코드", example = "ko")
    @Builder.Default
    private String languageCode = "ko";

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "경유지 래퍼")
    public static class WaypointWrapper {
        @JsonProperty("waypoint")
        @Schema(description = "경유지 정보")
        private LocationWrapper waypoint;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "위치 정보 래퍼")
    public static class LocationWrapper {
        @JsonProperty("location")
        @Schema(description = "위치 상세 정보")
        private Location location;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "위치 상세 정보")
    public static class Location {
        @JsonProperty("latLng")
        @Schema(description = "위도/경도")
        private LatLng latLng;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "위도/경도")
    public static class LatLng {
        @JsonProperty("latitude")
        @Schema(description = "위도", example = "37.5665")
        private Double latitude;

        @JsonProperty("longitude")
        @Schema(description = "경도", example = "126.9780")
        private Double longitude;
    }
}
