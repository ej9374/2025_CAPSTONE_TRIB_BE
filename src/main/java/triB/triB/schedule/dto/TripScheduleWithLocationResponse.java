package triB.triB.schedule.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import triB.triB.schedule.entity.TravelMode;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "여행 일정 응답 (위경도 포함)")
public class TripScheduleWithLocationResponse {

    @Schema(description = "여행 ID", example = "1")
    private Long tripId;

    @Schema(description = "여행 목적지", example = "파리")
    private String destination;

    @Schema(description = "여행 시작 날짜", example = "2024-03-15")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @Schema(description = "여행 종료 날짜", example = "2024-03-17")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    @Schema(description = "현재 조회 중인 일차", example = "1")
    private Integer currentDay;

    @Schema(description = "해당 날짜의 일정 목록 (위경도 포함)")
    private List<ScheduleItemWithLocationResponse> schedules;

    @Schema(description = "여행 이동 수단", example = "DRIVE")
    private TravelMode travelMode;

    @Schema(description = "AI 추정 예산", example = "500000")
    private Integer budget;
}