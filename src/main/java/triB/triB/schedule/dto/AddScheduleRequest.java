package triB.triB.schedule.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import triB.triB.chat.entity.PlaceTag;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "일정 추가 요청")
public class AddScheduleRequest {

    @Schema(description = "일차 (몇 일차에 추가할지)", example = "1")
    @NotNull(message = "일차는 필수입니다.")
    @Positive(message = "일차는 1 이상이어야 합니다.")
    private Integer dayNumber;

    @Schema(description = "장소명", example = "경복궁")
    @NotNull(message = "장소명은 필수입니다.")
    @NotBlank(message = "장소명은 비어있을 수 없습니다.")
    private String placeName;

    @Schema(description = "장소 태그", example = "TOURIST_SPOT")
    @NotNull(message = "장소 태그는 필수입니다.")
    private PlaceTag placeTag;

    @Schema(description = "위도", example = "37.5796")
    @NotNull(message = "위도는 필수입니다.")
    private Double latitude;

    @Schema(description = "경도", example = "126.9770")
    @NotNull(message = "경도는 필수입니다.")
    private Double longitude;

    @Schema(description = "체류 시간 (분 단위)", example = "60")
    @NotNull(message = "체류 시간은 필수입니다.")
    @Positive(message = "체류 시간은 1분 이상이어야 합니다.")
    @Builder.Default
    private Integer stayMinutes = 60;
}
