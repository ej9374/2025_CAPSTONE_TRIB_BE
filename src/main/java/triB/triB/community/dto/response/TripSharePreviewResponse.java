package triB.triB.community.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import triB.triB.community.dto.HashtagResponse;
import triB.triB.schedule.dto.TripScheduleResponse;

import java.util.List;

@Getter
@Builder
@Schema(description = "일정 공유 게시글 미리보기 응답")
public class TripSharePreviewResponse {

    @Schema(description = "AI가 생성한 추천 해시태그 목록")
    private List<HashtagResponse> suggestedHashtags;

    @Schema(description = "여행 일정 정보")
    private TripScheduleResponse scheduleInfo;
}
