package triB.triB.community.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import triB.triB.community.dto.PostSortType;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FreeBoardPostFilterRequest {
    // 제목 검색
    private String keyword;

    // 정렬 조건
    @Builder.Default
    private PostSortType sortType = PostSortType.LATEST;

    // 해시태그 필터 (여러 개 선택 가능)
    private List<String> hashtags;

    // 페이징
    @Builder.Default
    private Integer page = 0;

    @Builder.Default
    private Integer size = 20;

    // 현재 사용자 ID (차단 필터링용)
    private Long currentUserId;
}
