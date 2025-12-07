package triB.triB.schedule.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import triB.triB.chat.entity.PlaceTag;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "일정 항목 응답")
public class ScheduleItemResponse {

    @Schema(description = "일정 ID", example = "1")
    private Long scheduleId;

    @Schema(description = "장소명", example = "에펠탑")
    private String displayName;

    @Schema(description = "도착 시간", example = "2024-03-15T09:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime arrival;

    @Schema(description = "출발 시간", example = "2024-03-15T11:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime departure;

    @Schema(description = "장소 태그", example = "TOURIST_SPOT")
    private PlaceTag placeTag;

    @Schema(description = "다음 장소까지의 이동 시간 (분 단위)", example = "30")
    private Integer travelTime;

    @Schema(description = "방문 순서", example = "1")
    private Integer visitOrder;

    @Schema(description = "방문 완료 여부", example = "false")
    private Boolean isVisit;
}