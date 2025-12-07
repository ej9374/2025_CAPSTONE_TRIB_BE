package triB.triB.schedule.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "일정 변경 타입")
public enum ModificationType {
    @Schema(description = "순서 변경")
    REORDER,

    @Schema(description = "체류시간 수정")
    UPDATE_STAY_DURATION,

    @Schema(description = "방문시간 수정")
    UPDATE_VISIT_TIME,

    @Schema(description = "일정 추가")
    ADD,

    @Schema(description = "일정 삭제")
    DELETE,

    @Schema(description = "숙소 변경 (기존 숙소만)")
    UPDATE_ACCOMMODATION,

    @Schema(description = "이동시간 수정")
    UPDATE_TRAVEL_TIME
}
