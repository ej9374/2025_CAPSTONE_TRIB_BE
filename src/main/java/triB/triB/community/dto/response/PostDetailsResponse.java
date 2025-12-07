package triB.triB.community.dto.response;

import lombok.Builder;
import lombok.Getter;
import triB.triB.auth.entity.User;
import triB.triB.community.dto.AuthorResponse;
import triB.triB.community.dto.HashtagResponse;
import triB.triB.community.dto.PostImageResponse;
import triB.triB.community.entity.Hashtag;
import triB.triB.community.entity.Post;
import triB.triB.community.entity.PostImage;
import triB.triB.community.entity.PostType;
import triB.triB.schedule.entity.Trip;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class PostDetailsResponse {
    private Long postId;
    private PostType postType;
    private String title;
    private String content;
    private AuthorResponse author;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // TRIP_SHARE 전용 필드
    private Long tripId;
    private String destination;
    private LocalDate startDate;
    private LocalDate endDate;

    // TRIP_SHARE 전용 평가 필드
    private Boolean matchesPreferences; // 취향에 맞는지
    private Boolean isPractical; // 실용적인지
    private String travelReview; // 여행평가

    private Integer likesCount;
    private Integer commentsCount;
    private Boolean isLikedByMe;  // 현재 사용자의 좋아요 여부

    private List<PostImageResponse> images;
    private List<HashtagResponse> hashtags;

    public static PostDetailsResponse from(Post post, User author, Trip trip,
                                           List<PostImage> images,
                                           List<Hashtag> hashtags,
                                           boolean isLikedByMe) {
        return PostDetailsResponse.builder()
                .postId(post.getPostId())
                .postType(post.getPostType())
                .title(post.getTitle())
                .content(post.getContent())
                .author(AuthorResponse.from(author))
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .tripId(trip != null ? trip.getTripId() : null)
                .destination(trip != null ? trip.getDestination() : null)
                .startDate(trip != null && trip.getRoom() != null ? trip.getRoom().getStartDate() : null)
                .endDate(trip != null && trip.getRoom() != null ? trip.getRoom().getEndDate() : null)
                .matchesPreferences(post.getMatchesPreferences())
                .isPractical(post.getIsPractical())
                .travelReview(post.getTravelReview())
                .likesCount(post.getLikesCount())
                .commentsCount(post.getCommentsCount())
                .isLikedByMe(isLikedByMe)
                .images(images.stream().map(PostImageResponse::from).collect(Collectors.toList()))
                .hashtags(hashtags.stream().map(HashtagResponse::from).collect(Collectors.toList()))
                .build();
    }
}
