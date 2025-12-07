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
@Schema(description = "숙박 비용 정보 응답")
public class AccommodationCostResponse {

    @Schema(description = "숙박 비용 정보", example = "1박당 15만원 예상")
    private String accommodationCostInfo;
}
