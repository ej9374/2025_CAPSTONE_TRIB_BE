package triB.triB.schedule.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "방문 시간 수정 요청")
public class UpdateVisitTimeRequest {

    @Schema(description = "새로운 방문 시간 (HH:mm 형식)", example = "14:30")
    @NotNull(message = "방문 시간은 필수입니다.")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime newArrivalTime;
}
