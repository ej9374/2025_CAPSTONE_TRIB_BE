package triB.triB.community.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import triB.triB.auth.entity.User;
import triB.triB.auth.repository.UserRepository;
import triB.triB.chat.entity.Message;
import triB.triB.chat.entity.MessageStatus;
import triB.triB.chat.entity.MessageType;
import triB.triB.chat.repository.MessageRepository;
import triB.triB.community.dto.HashtagResponse;
import triB.triB.community.dto.request.FreeBoardPostCreateRequest;
import triB.triB.community.dto.request.FreeBoardPostFilterRequest;
import triB.triB.community.dto.request.TripSharePostCreateRequest;
import triB.triB.community.dto.request.TripSharePostFilterRequest;
import triB.triB.community.dto.response.HotPostResponse;
import triB.triB.community.dto.response.PostDetailsResponse;
import triB.triB.community.dto.response.PostSummaryResponse;
import triB.triB.community.dto.response.TripSharePreviewResponse;
import triB.triB.community.entity.*;
import triB.triB.community.repository.*;
import triB.triB.global.exception.CustomException;
import triB.triB.global.exception.ErrorCode;
import triB.triB.global.infra.AwsS3Client;
import triB.triB.room.repository.UserRoomRepository;
import triB.triB.schedule.dto.TripScheduleResponse;
import triB.triB.schedule.entity.Trip;
import triB.triB.schedule.repository.TripRepository;
import triB.triB.schedule.service.ScheduleService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final PostImageRepository postImageRepository;
    private final PostHashtagRepository postHashtagRepository;
    private final HashtagRepository hashtagRepository;
    private final PostLikeRepository postLikeRepository;
    private final UserRepository userRepository;
    private final TripRepository tripRepository;
    private final UserRoomRepository userRoomRepository;
    private final AwsS3Client awsS3Client;
    private final MessageRepository messageRepository;
    private final HashtagService hashtagService;
    private final ScheduleService scheduleService;

    /**
     * 일정 공유 게시글 작성 미리보기
     * - AI가 생성한 해시태그와 여행 일정 정보를 반환
     * - DB에 저장하지 않음 (미리보기만 제공)
     */
    @Transactional(readOnly = true)
    public TripSharePreviewResponse getTripSharePreview(Long userId, Long tripId) {
        // 1. 사용자 검증
        userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // 2. Trip 검증 및 조회
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new CustomException(ErrorCode.TRIP_NOT_FOUND));

        // 3. 사용자가 해당 여행의 참여자인지 검증
        validateUserInTrip(userId, trip);

        // 4. AI 해시태그 생성 (DB에 저장됨)
        List<Hashtag> hashtags = hashtagService.generateHashtagsForTripShare(trip);
        List<HashtagResponse> hashtagResponses = hashtags.stream()
                .map(HashtagResponse::from)
                .collect(Collectors.toList());

        // 5. 여행 일정 조회 (첫째 날 일정)
        TripScheduleResponse scheduleInfo = scheduleService.getTripSchedulesPublic(tripId, 1);

        // 6. 미리보기 응답 생성
        return TripSharePreviewResponse.builder()
                .suggestedHashtags(hashtagResponses)
                .scheduleInfo(scheduleInfo)
                .build();
    }

    @Transactional
    public PostDetailsResponse createTripSharePost(Long userId,
                                                   TripSharePostCreateRequest request,
                                                   List<MultipartFile> images) {
        // 1. 사용자 검증
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // 2. Trip 검증 및 조회
        Trip trip = tripRepository.findById(request.getTripId())
                .orElseThrow(() -> new CustomException(ErrorCode.TRIP_NOT_FOUND));

        // 3. 사용자가 해당 여행의 참여자인지 검증
        validateUserInTrip(userId, trip);

        // 4. Trip의 Room에서 room_name 가져오기
        String roomName = trip.getRoom().getRoomName();

        // 5. Post 엔티티 생성 (제목은 room_name, content는 사용자 입력)
        Post post = Post.builder()
                .userId(userId)
                .user(user)
                .postType(PostType.TRIP_SHARE)
                .tripId(request.getTripId())
                .trip(trip)
                .title(roomName)
                .content(request.getContent())
                .matchesPreferences(request.getMatchesPreferences())
                .isPractical(request.getIsPractical())
                .travelReview(request.getTravelReview())
                .likesCount(0)
                .commentsCount(0)
                .build();

        Post savedPost = postRepository.save(post);

        // 6. 이미지 업로드 및 저장
        List<PostImage> postImages = new ArrayList<>();
        if (images != null && !images.isEmpty()) {
            for (int i = 0; i < images.size(); i++) {
                String imageUrl = awsS3Client.uploadFile(images.get(i));
                PostImage postImage = PostImage.builder()
                        .postId(savedPost.getPostId())
                        .post(savedPost)
                        .imageUrl(imageUrl)
                        .displayOrder(i)
                        .build();
                postImages.add(postImageRepository.save(postImage));
            }
        }

        // 7. 선택된 해시태그 처리
        List<Hashtag> hashtags = hashtagRepository.findAllById(request.getSelectedHashtagIds());

        // 7-1. 모든 요청된 해시태그가 DB에 존재하는지 검증
        if (hashtags.size() != request.getSelectedHashtagIds().size()) {
            throw new CustomException(ErrorCode.HASHTAG_NOT_FOUND);
        }

        // 7-2. PostHashtag 관계 생성
        for (Hashtag hashtag : hashtags) {
            PostHashtagId postHashtagId = new PostHashtagId(savedPost.getPostId(), hashtag.getHashtagId());
            PostHashtag postHashtag = PostHashtag.builder()
                    .id(postHashtagId)
                    .post(savedPost)
                    .hashtag(hashtag)
                    .build();
            postHashtagRepository.save(postHashtag);
        }

        // 8. Response 생성
        return PostDetailsResponse.from(savedPost, user, trip, postImages, hashtags, false);
    }

    @Transactional
    public PostDetailsResponse createFreeBoardPost(Long userId,
                                                   FreeBoardPostCreateRequest request,
                                                   List<MultipartFile> images) {
        // 1. 사용자 검증
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // 2. Post 엔티티 생성
        Post post = Post.builder()
                .userId(userId)
                .user(user)
                .postType(PostType.FREE_BOARD)
                .tripId(null)  // 자유게시판은 tripId 없음
                .trip(null)
                .title(request.getTitle())
                .content(request.getContent())
                .likesCount(0)
                .commentsCount(0)
                .build();

        Post savedPost = postRepository.save(post);

        // 3. 이미지 업로드 및 저장
        List<PostImage> postImages = new ArrayList<>();
        if (images != null && !images.isEmpty()) {
            for (int i = 0; i < images.size(); i++) {
                String imageUrl = awsS3Client.uploadFile(images.get(i));
                PostImage postImage = PostImage.builder()
                        .postId(savedPost.getPostId())
                        .post(savedPost)
                        .imageUrl(imageUrl)
                        .displayOrder(i)
                        .build();
                postImages.add(postImageRepository.save(postImage));
            }
        }

        // 4. Predefined 해시태그 연결
        List<Hashtag> hashtags = hashtagRepository.findByTagNameIn(request.getHashtags());

        // 4-1. 모든 요청된 해시태그가 DB에 존재하는지 검증
        if (hashtags.size() != request.getHashtags().size()) {
            throw new CustomException(ErrorCode.INVALID_HASHTAG);
        }

        // 4-2. 모든 해시태그가 PREDEFINED 타입인지 검증
        boolean allPredefined = hashtags.stream()
                .allMatch(h -> h.getTagType() == TagType.PREDEFINED);
        if (!allPredefined) {
            throw new CustomException(ErrorCode.INVALID_HASHTAG);
        }

        for (Hashtag hashtag : hashtags) {
            PostHashtagId postHashtagId = new PostHashtagId(savedPost.getPostId(), hashtag.getHashtagId());
            PostHashtag postHashtag = PostHashtag.builder()
                    .id(postHashtagId)
                    .post(savedPost)
                    .hashtag(hashtag)
                    .build();
            postHashtagRepository.save(postHashtag);
        }

        // 5. Response 생성
        return PostDetailsResponse.from(savedPost, user, null, postImages, hashtags, false);
    }

    public PostDetailsResponse getPostDetails(Long postId, Long currentUserId) {
        // 1. Post 조회
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        // 2. 작성자 조회
        User author = userRepository.findById(post.getUserId())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // 3. Trip 조회 (TRIP_SHARE인 경우만)
        Trip trip = null;
        if (post.getPostType() == PostType.TRIP_SHARE && post.getTripId() != null) {
            trip = tripRepository.findById(post.getTripId()).orElse(null);
        }

        // 4. 이미지 조회
        List<PostImage> images = postImageRepository.findByPostIdOrderByDisplayOrderAsc(postId);

        // 5. 해시태그 조회
        List<PostHashtag> postHashtags = postHashtagRepository.findByIdPostId(postId);
        List<Hashtag> hashtags = postHashtags.stream()
                .map(ph -> hashtagRepository.findById(ph.getId().getHashtagId()).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // 6. 현재 사용자의 좋아요 여부 확인
        boolean isLikedByMe = currentUserId != null &&
                postLikeRepository.existsByIdPostIdAndIdUserId(postId, currentUserId);

        return PostDetailsResponse.from(post, author, trip, images, hashtags, isLikedByMe);
    }

    public List<PostSummaryResponse> getTripSharePosts(TripSharePostFilterRequest filter) {
        // Custom repository를 통한 필터링 쿼리 실행
        List<Post> posts = postRepository.findTripSharePostsWithFilters(filter);

        return posts.stream()
                .map(this::mapToSummary)
                .collect(Collectors.toList());
    }

    public List<PostSummaryResponse> getFreeBoardPosts(FreeBoardPostFilterRequest filter) {
        // Custom repository를 통한 필터링 쿼리 실행
        List<Post> posts = postRepository.findFreeBoardPostsWithFilters(filter);

        return posts.stream()
                .map(this::mapToSummary)
                .collect(Collectors.toList());
    }

    public HotPostResponse calculateHotPost() {
        // 최근 1시간 내 작성된 게시글 중 좋아요 + 댓글 수 최상위 1개
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);

        Post hotPost = postRepository.findTopByPostTypeAndCreatedAtAfterOrderByLikesCountDescCommentsCountDesc(
                PostType.FREE_BOARD, oneHourAgo);

        return hotPost != null ? HotPostResponse.from(hotPost) : null;
    }

    private PostSummaryResponse mapToSummary(Post post) {
        User author = userRepository.findById(post.getUserId()).orElse(null);
        Trip trip = post.getTripId() != null ?
                tripRepository.findById(post.getTripId()).orElse(null) : null;

        // 모든 이미지 조회 (displayOrder 순서대로 정렬)
        List<PostImage> images = postImageRepository.findByPostIdOrderByDisplayOrderAsc(post.getPostId());

        // 첫 번째 이미지 URL 조회
        String coverImageUrl = images.stream()
                .findFirst()
                .map(PostImage::getImageUrl)
                .orElse(null);

        // 모든 이미지 URL 리스트 변환
        List<String> imageUrls = images.stream()
                .map(PostImage::getImageUrl)
                .collect(Collectors.toList());

        // 해시태그 조회
        List<Hashtag> hashtags = postHashtagRepository.findByIdPostId(post.getPostId())
                .stream()
                .map(ph -> hashtagRepository.findById(ph.getId().getHashtagId()).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return PostSummaryResponse.from(post, author, trip, coverImageUrl, imageUrls, hashtags);
    }

    private void validateUserInTrip(Long userId, Trip trip) {
        // UserRoom을 통해 사용자가 해당 여행의 참여자인지 확인
        boolean isParticipant = userRoomRepository.existsByUser_UserIdAndRoom_RoomId(
                userId, trip.getRoomId());

        if (!isParticipant) {
            throw new CustomException(ErrorCode.USER_NOT_IN_TRIP);
        }
    }

    /**
     * 게시글 삭제
     * - 작성자 본인만 삭제 가능
     * - S3 이미지 파일 삭제
     * - Post 삭제 시 cascade로 연관 엔티티 자동 삭제 (PostImage, Comment, PostLike, PostHashtag)
     *
     * @param postId 삭제할 게시글 ID
     * @param userId 현재 로그인한 사용자 ID
     * @throws CustomException POST_NOT_FOUND - 게시글이 존재하지 않음
     * @throws CustomException UNAUTHORIZED_POST_ACCESS - 작성자가 아님
     */
    @Transactional
    public void deletePost(Long postId, Long userId) {
        // 1. Post 조회 및 존재 확인
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        // 2. 작성자 권한 검증
        if (!post.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_POST_ACCESS);
        }

        // 3. S3에 업로드된 이미지 파일 삭제
        List<PostImage> images = postImageRepository.findByPostIdOrderByDisplayOrderAsc(postId);
        for (PostImage image : images) {
            awsS3Client.delete(image.getImageUrl());
        }

        // 4. Post 삭제 (cascade로 연관 엔티티 자동 삭제)
        // - PostImage (cascade ALL, orphanRemoval)
        // - Comment (cascade ALL, orphanRemoval) - 대댓글 포함
        // - PostLike (cascade ALL, orphanRemoval)
        // - PostHashtag (cascade ALL, orphanRemoval)
        postRepository.delete(post);
        changeMessageStatus(postId);
    }

    private void changeMessageStatus(Long postId){
        List<Message> messages = messageRepository.findByMessageTypeAndContent(MessageType.COMMUNITY_SHARE, postId.toString());
        messages.forEach(m -> {
            m.setMessageStatus(MessageStatus.DELETE);
            messageRepository.save(m);
        });
    }
}
