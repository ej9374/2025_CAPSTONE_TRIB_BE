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
@Schema(description = "일정 삭제 응답")
public class DeleteScheduleResponse {

    @Schema(description = "삭제된 일정 ID", example = "1")
    private Long deletedScheduleId;
}
