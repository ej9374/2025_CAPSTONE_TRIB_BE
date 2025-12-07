package triB.triB.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import triB.triB.auth.entity.User;
import triB.triB.community.dto.AuthorResponse;
import triB.triB.community.dto.HashtagResponse;
import triB.triB.community.entity.*;
import triB.triB.community.repository.PostHashtagRepository;
import triB.triB.community.repository.PostImageRepository;
import triB.triB.community.repository.PostLikeRepository;
import triB.triB.community.repository.PostRepository;
import triB.triB.schedule.entity.Trip;
import triB.triB.user.dto.PostResponse;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserCommunityService {

    private final PostRepository postRepository;
    private final PostImageRepository postImageRepository;
    private final PostHashtagRepository postHashtagRepository;
    private final PostLikeRepository postLikeRepository;

    public List<PostResponse> getMyPosts(Long userId, PostType postType) {
        log.debug("유저가 작성한 게시글 조회 시작");
        List<Post> posts = postRepository.findByUser_UserIdAndPostTypeOrderByPostIdDesc(userId, postType);
        if (posts.isEmpty()) {
            return List.of();
        }
        return getPosts(posts);
    }

    public List<PostResponse> getLikePosts(Long userId, PostType postType) {
        log.debug("유저가 좋아요 누른 게시글 조회 시작");
        
        List<Post> posts;
        if (postType == PostType.TRIP_SHARE) {
            posts = postLikeRepository.findTripShareLikedPosts(userId);
        } else {
            posts = postLikeRepository.findFreeBoardLikedPosts(userId);
        }
        if (posts.isEmpty()) {
            return List.of();
        }
        return getPosts(posts);
    }

    private List<PostResponse> getPosts(List<Post> posts){
        log.debug("게시글 조회");
        List<Long> postIds = posts.stream().map(Post::getPostId).toList();
        Map<Long, User> userMap = posts.stream()
                .map(Post::getUser)
                .distinct()
                .collect(Collectors.toMap(User::getUserId, u -> u));

        List<PostHashtag> hashtags = postHashtagRepository.findByPost_PostIds(postIds);
        Map<Long, List<PostHashtag>> hashtagMap = hashtags.stream()
                .collect(Collectors.groupingBy(ht -> ht.getPost().getPostId()));

        List<PostImage> images = postImageRepository.findImageUrlByPostIds(postIds);
        Map<Long, List<String>> imageMap = images.stream()
                .collect(Collectors.groupingBy(
                        PostImage::getPostId,
                        Collectors.mapping(PostImage::getImageUrl, Collectors.toList())
                ));

        return posts.stream()
                .map(p -> {
                    Trip t = p.getTrip();
                    List<PostHashtag> ht = hashtagMap.getOrDefault(p.getPostId(), List.of());
                    User u = userMap.get(p.getUserId());
                    PostResponse.PostResponseBuilder builder = PostResponse.builder()
                            .postId(p.getPostId())
                            .postType(p.getPostType())
                            .title(p.getTitle())
                            .author(
                                    AuthorResponse.builder()
                                            .userId(u.getUserId())
                                            .nickname(u.getNickname())
                                            .photoUrl(u.getPhotoUrl())
                                            .build()
                            )
                            .createdAt(p.getCreatedAt())
                            .imageUrl(imageMap.getOrDefault(p.getPostId(), List.of()))
                            .likesCount(p.getLikesCount())
                            .commentsCount(p.getCommentsCount())
                            .hashtags(ht.stream()
                                    .map(h -> new HashtagResponse(h.getId().getHashtagId(), h.getHashtag().getTagName(), h.getHashtag().getTagType()))
                                    .toList());

                    if (t != null) {
                        builder.destination(t.getDestination())
                                .startDate(t.getRoom().getStartDate())
                                .endDate(t.getRoom().getEndDate());
                    }
                    return builder.build();
                })
                .toList();
    }
}
