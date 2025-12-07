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
@Schema(description = "일정 비용 정보 응답")
public class ScheduleCostResponse {

    @Schema(description = "일정 ID", example = "1")
    private Long scheduleId;

    @Schema(description = "예상 비용 (원)", example = "50000")
    private Integer estimatedCost;

    @Schema(description = "비용 설명", example = "입장료 및 식사 비용 포함")
    private String costExplanation;
}
