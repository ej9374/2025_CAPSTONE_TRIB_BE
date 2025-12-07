package triB.triB.user.dto;

import lombok.Builder;
import lombok.Getter;
import triB.triB.auth.entity.User;
import triB.triB.community.dto.AuthorResponse;
import triB.triB.community.dto.HashtagResponse;
import triB.triB.community.entity.Hashtag;
import triB.triB.community.entity.Post;
import triB.triB.community.entity.PostType;
import triB.triB.schedule.entity.Trip;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class PostResponse {
    private Long postId;
    private PostType postType;
    private String title;
    private AuthorResponse author;
    private LocalDateTime createdAt;

    // TRIP_SHARE 전용 필드
    private String destination;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<String> imageUrl;  // 첫 번째 이미지

    private Integer likesCount;
    private Integer commentsCount;
    private List<HashtagResponse> hashtags;

}