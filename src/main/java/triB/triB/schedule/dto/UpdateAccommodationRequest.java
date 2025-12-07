package triB.triB.schedule.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
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
@Schema(description = "숙소 변경 요청")
public class UpdateAccommodationRequest {

    @Schema(description = "숙소가 위치한 날짜 (일차)", example = "1")
    @NotNull(message = "날짜는 필수입니다.")
    @Positive(message = "날짜는 1 이상이어야 합니다.")
    private Integer dayNumber;

    @Schema(description = "숙소 이름", example = "호텔 신라")
    @NotBlank(message = "숙소 이름은 필수입니다.")
    private String placeName;

    @Schema(description = "위도", example = "37.5665")
    @NotNull(message = "위도는 필수입니다.")
    private Double latitude;

    @Schema(description = "경도", example = "126.9780")
    @NotNull(message = "경도는 필수입니다.")
    private Double longitude;
}
