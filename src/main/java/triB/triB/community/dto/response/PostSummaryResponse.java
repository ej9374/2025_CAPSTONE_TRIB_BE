package triB.triB.community.dto.response;

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
import java.util.stream.Collectors;

@Getter
@Builder
public class PostSummaryResponse {
    private Long postId;
    private PostType postType;
    private String title;
    private AuthorResponse author;
    private LocalDateTime createdAt;

    // TRIP_SHARE 전용 필드
    private String destination;
    private LocalDate startDate;
    private LocalDate endDate;
    private String coverImageUrl;  // 첫 번째 이미지
    private List<String> imageUrls;  // 모든 이미지 URL 리스트

    private Integer likesCount;
    private Integer commentsCount;
    private List<HashtagResponse> hashtags;

    public static PostSummaryResponse from(Post post, User author, Trip trip,
                                          String coverImageUrl, List<String> imageUrls, List<Hashtag> hashtags) {
        return PostSummaryResponse.builder()
            .postId(post.getPostId())
            .postType(post.getPostType())
            .title(post.getTitle())
            .author(AuthorResponse.from(author))
            .createdAt(post.getCreatedAt())
            .destination(trip != null ? trip.getDestination() : null)
            .startDate(trip != null && trip.getRoom() != null ? trip.getRoom().getStartDate() : null)
            .endDate(trip != null && trip.getRoom() != null ? trip.getRoom().getEndDate() : null)
            .coverImageUrl(coverImageUrl)
            .imageUrls(imageUrls)
            .likesCount(post.getLikesCount())
            .commentsCount(post.getCommentsCount())
            .hashtags(hashtags.stream().map(HashtagResponse::from).collect(Collectors.toList()))
            .build();
    }
}
