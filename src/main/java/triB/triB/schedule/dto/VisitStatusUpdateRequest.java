package triB.triB.schedule.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "방문 상태 변경 요청")
public class VisitStatusUpdateRequest {

    @Schema(description = "방문 완료 여부", example = "true")
    @NotNull(message = "방문 상태는 필수입니다.")
    private Boolean isVisit;
}
