package triB.triB.community.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import triB.triB.community.dto.response.HotPostResponse;

import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class HotPostScheduler {

    private final PostService postService;
    private final AtomicReference<HotPostResponse> cachedHotPost = new AtomicReference<>();

    /**
     * 핫 게시글을 계산하여 캐시에 저장
     * 기본값: 3600000ms (1시간), application.properties에서 hot-post.scheduler.interval로 설정 가능
     */
    @Scheduled(fixedRateString = "${hot-post.scheduler.interval:3600000}")
    public void updateHotPost() {
        log.info("핫 게시글 갱신 시작");
        try {
            HotPostResponse hotPost = postService.calculateHotPost();
            if (hotPost != null) {
                cachedHotPost.set(hotPost);
                log.info("핫 게시글 갱신 완료: postId={}, title={}", hotPost.getPostId(), hotPost.getTitle());
            } else {
                log.info("새로운 핫 게시글이 없어 기존 핫 게시글을 유지합니다.");
            }
        } catch (Exception e) {
            log.error("핫 게시글 갱신 중 오류 발생", e);
        }
    }

    /**
     * 캐시된 핫 게시글 반환
     */
    public HotPostResponse getHotPost() {
        return cachedHotPost.get();
    }
}
