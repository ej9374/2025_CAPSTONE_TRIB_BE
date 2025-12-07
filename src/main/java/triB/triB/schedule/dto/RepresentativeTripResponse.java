package triB.triB.schedule.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "대표 여행 ID 응답")
public class RepresentativeTripResponse {

    @Schema(description = "대표 여행 ID (여행이 없는 경우 null)", example = "1", nullable = true)
    private Long tripId;
}
