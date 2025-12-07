package triB.triB.community.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class TripSharePostCreateRequest {

    @NotNull(message = "여행 ID는 필수입니다.")
    private Long tripId;

    @Size(max = 2000, message = "내용은 2000자를 초과할 수 없습니다.")
    private String content;

    @NotEmpty(message = "최소 1개의 해시태그를 선택해야 합니다.")
    private List<Long> selectedHashtagIds;

    // 여행 평가 필드
    private Boolean matchesPreferences; // 취향에 맞는지

    private Boolean isPractical; // 실용적인지

    @Size(max = 2000, message = "여행평가는 2000자를 초과할 수 없습니다.")
    private String travelReview; // 여행평가 (선택사항)

    // 제목은 해당 여행을 생성한 채팅방의 room_name을 사용
    // 이미지는 MultipartFile로 별도 처리
}
