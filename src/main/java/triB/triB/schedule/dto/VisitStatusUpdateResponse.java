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
@Schema(description = "방문 상태 변경 응답")
public class VisitStatusUpdateResponse {

    @Schema(description = "일정 ID", example = "1")
    private Long scheduleId;

    @Schema(description = "방문 완료 여부", example = "true")
    private Boolean isVisit;
}
