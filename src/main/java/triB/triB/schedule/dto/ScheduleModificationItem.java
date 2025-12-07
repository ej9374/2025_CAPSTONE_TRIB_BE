package triB.triB.schedule.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import triB.triB.chat.entity.PlaceTag;

import java.time.LocalTime;

/**
 * 일정 변경 항목
 *
 * <h3>각 변경 타입별 필요한 필드</h3>
 * <ul>
 *   <li><strong>REORDER:</strong> scheduleId, newVisitOrder</li>
 *   <li><strong>UPDATE_STAY_DURATION:</strong> scheduleId, stayMinutes</li>
 *   <li><strong>UPDATE_VISIT_TIME:</strong> scheduleId, newArrivalTime</li>
 *   <li><strong>UPDATE_TRAVEL_TIME:</strong> scheduleId, travelTime</li>
 *   <li><strong>ADD:</strong> dayNumber, placeName, placeTag, latitude, longitude, stayMinutes</li>
 *   <li><strong>DELETE:</strong> scheduleId</li>
 *   <li><strong>UPDATE_ACCOMMODATION:</strong> scheduleId, placeName, latitude, longitude (기존 숙소만 변경 가능)</li>
 * </ul>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "일정 변경사항")
public class ScheduleModificationItem {

    @Schema(description = "변경 타입", example = "REORDER", required = true)
    @NotNull(message = "변경 타입은 필수입니다.")
    private ModificationType modificationType;

    // ===== REORDER, UPDATE_STAY_DURATION, UPDATE_VISIT_TIME, UPDATE_TRAVEL_TIME, DELETE, UPDATE_ACCOMMODATION에 사용 =====
    @Schema(description = "일정 ID (REORDER, UPDATE_STAY_DURATION, UPDATE_VISIT_TIME, UPDATE_TRAVEL_TIME, DELETE, UPDATE_ACCOMMODATION용)", example = "1")
    private Long scheduleId;

    // ===== REORDER에 사용 =====
    @Schema(description = "새로운 방문 순서 (REORDER용)", example = "3")
    @Positive(message = "방문 순서는 1 이상이어야 합니다.")
    private Integer newVisitOrder;

    // ===== UPDATE_STAY_DURATION, ADD에 사용 =====
    @Schema(description = "체류 시간 분 단위 (UPDATE_STAY_DURATION, ADD용)", example = "90")
    @Positive(message = "체류 시간은 1분 이상이어야 합니다.")
    private Integer stayMinutes;

    // ===== UPDATE_VISIT_TIME에 사용 =====
    @Schema(description = "새로운 도착 시간 (UPDATE_VISIT_TIME용) - HH:mm 형식", example = "10:30")
    private LocalTime newArrivalTime;

    // ===== ADD에 사용 =====
    @Schema(description = "일차 (ADD용)", example = "1")
    @Positive(message = "일차는 1 이상이어야 합니다.")
    private Integer dayNumber;

    // ===== ADD, UPDATE_ACCOMMODATION에 사용 =====
    @Schema(description = "장소명 (ADD, UPDATE_ACCOMMODATION용)", example = "남산타워")
    private String placeName;

    @Schema(description = "장소 태그 (ADD용)", example = "TOURIST_SPOT")
    private PlaceTag placeTag;

    @Schema(description = "위도 (ADD, UPDATE_ACCOMMODATION용)", example = "37.5512")
    private Double latitude;

    @Schema(description = "경도 (ADD, UPDATE_ACCOMMODATION용)", example = "126.9882")
    private Double longitude;

    // ===== UPDATE_TRAVEL_TIME에 사용 =====
    @Schema(description = "이동시간 분 단위 (UPDATE_TRAVEL_TIME용)", example = "30")
    @Positive(message = "이동시간은 1분 이상이어야 합니다.")
    private Integer travelTime;
}