package triB.triB.community.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import triB.triB.community.dto.PostSortType;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripSharePostFilterRequest {
    // 나라별 필터
    private String country;  // Trip의 destination과 매칭

    // 정렬 조건
    @Builder.Default
    private PostSortType sortType = PostSortType.LATEST;

    // 검색어 (제목 + 해시태그 동시 검색)
    private String keyword;

    // 페이징 (선택)
    @Builder.Default
    private Integer page = 0;

    @Builder.Default
    private Integer size = 20;

    // 현재 사용자 ID (차단 필터링용)
    private Long currentUserId;
}
