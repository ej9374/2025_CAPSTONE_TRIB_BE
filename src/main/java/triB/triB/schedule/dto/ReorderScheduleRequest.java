package triB.triB.schedule.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "일정 순서 변경 요청")
public class ReorderScheduleRequest {

    @Schema(description = "새로운 방문 순서", example = "3")
    @NotNull(message = "새로운 방문 순서는 필수입니다.")
    @Positive(message = "방문 순서는 1 이상이어야 합니다.")
    private Integer newVisitOrder;
}
