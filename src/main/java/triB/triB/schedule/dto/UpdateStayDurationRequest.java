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
@Schema(description = "체류시간 수정 요청")
public class UpdateStayDurationRequest {

    @Schema(description = "체류 시간 (분 단위)", example = "90")
    @NotNull(message = "체류 시간은 필수입니다.")
    @Positive(message = "체류 시간은 1분 이상이어야 합니다.")
    private Integer stayMinutes;
}
