package triB.triB.schedule.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "여행 목록 응답")
public class TripListResponse {

    @Schema(description = "여행 ID", example = "1")
    private Long tripId;

    @Schema(description = "여행 목적지", example = "서울")
    private String destination;

    @Schema(description = "채팅방 이름", example = "부산 여행방")
    private String roomName;

    @Schema(description = "여행 시작 날짜", example = "2025-01-01")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @Schema(description = "여행 종료 날짜", example = "2025-01-03")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    @Schema(description = "여행 참여자 목록")
    private List<TripParticipantResponse> participants;

    @Schema(description = "AI 추정 예산", example = "500000")
    private Integer budget;

    @Schema(description = "사용자 설정 예산", example = "450000.00")
    private BigDecimal userBudget;
}
