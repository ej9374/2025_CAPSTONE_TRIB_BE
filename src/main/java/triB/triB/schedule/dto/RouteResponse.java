package triB.triB.schedule.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Google Routes API 경로 계산 응답")
public class RouteResponse {

    @JsonProperty("routes")
    @Schema(description = "경로 목록")
    private List<Route> routes;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "경로 정보")
    public static class Route {
        @JsonProperty("duration")
        @Schema(description = "이동 시간 (ISO 8601 형식)", example = "PT30M")
        private String duration;
    }
}
