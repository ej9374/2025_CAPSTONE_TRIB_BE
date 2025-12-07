package triB.triB.schedule.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "일정 변경 미리보기 요청")
public class PreviewScheduleRequest {

    @Schema(description = "일차 (조회할 날짜)", example = "1", required = true)
    @NotNull(message = "일차는 필수입니다.")
    @Positive(message = "일차는 1 이상이어야 합니다.")
    private Integer dayNumber;

    @Schema(description = "변경사항 목록", required = true)
    @NotNull(message = "변경사항 목록은 필수입니다.")
    @Valid
    private List<ScheduleModificationItem> modifications;
}
