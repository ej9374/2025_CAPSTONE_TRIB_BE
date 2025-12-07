package triB.triB.community.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import triB.triB.auth.entity.User;
import triB.triB.community.dto.HashtagResponse;
import triB.triB.community.dto.response.TripSharePreviewResponse;
import triB.triB.community.entity.TagType;
import triB.triB.community.repository.HashtagRepository;
import triB.triB.community.repository.PostRepository;
import triB.triB.community.service.HotPostScheduler;
import triB.triB.community.service.PostLikeService;
import triB.triB.community.service.PostService;
import triB.triB.global.exception.CustomException;
import triB.triB.global.exception.ErrorCode;
import triB.triB.global.security.JwtAuthenticationFilter;
import triB.triB.global.security.JwtProvider;
import triB.triB.global.security.UserPrincipal;
import triB.triB.schedule.dto.ScheduleItemResponse;
import triB.triB.schedule.dto.TripScheduleResponse;
import triB.triB.schedule.service.ScheduleService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PostController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("PostController 통합 테스트")
class PostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PostService postService;

    @MockBean
    private PostLikeService postLikeService;

    @MockBean
    private HashtagRepository hashtagRepository;

    @MockBean
    private HotPostScheduler hotPostScheduler;

    @MockBean
    private PostRepository postRepository;

    @MockBean
    private ScheduleService scheduleService;

    @MockBean
    private JwtProvider jwtProvider;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private UserPrincipal userPrincipal;
    private Long userId;
    private Long tripId;

    @BeforeEach
    void setUp() {
        userId = 1L;
        tripId = 100L;

        // Mock User 객체 생성
        User mockUser = User.builder()
                .userId(userId)
                .email("test@example.com")
                .username("testuser")
                .nickname("TestNickname")
                .build();

        // UserPrincipal 생성
        userPrincipal = new UserPrincipal(mockUser, userId, "test@example.com", "testuser", "TestNickname");

        // SecurityContext 설정
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userPrincipal, null, userPrincipal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    @DisplayName("POST /api/v1/community/posts/trip-share/preview - 일정 공유 미리보기 성공")
    void getTripSharePreview_Success() throws Exception {
        // given
        List<HashtagResponse> hashtags = Arrays.asList(
                HashtagResponse.builder()
                        .hashtagId(1L)
                        .tagName("파리여행")
                        .tagType(TagType.CUSTOM_AI)
                        .build(),
                HashtagResponse.builder()
                        .hashtagId(2L)
                        .tagName("에펠탑")
                        .tagType(TagType.CUSTOM_AI)
                        .build(),
                HashtagResponse.builder()
                        .hashtagId(3L)
                        .tagName("낭만")
                        .tagType(TagType.CUSTOM_AI)
                        .build()
        );

        ScheduleItemResponse scheduleItem = ScheduleItemResponse.builder()
                .scheduleId(1L)
                .displayName("에펠탑")
                .arrival(LocalDateTime.of(2025, 1, 1, 9, 0))
                .departure(LocalDateTime.of(2025, 1, 1, 11, 0))
                .visitOrder(1)
                .isVisit(false)
                .build();

        TripScheduleResponse scheduleInfo = TripScheduleResponse.builder()
                .tripId(tripId)
                .destination("파리")
                .startDate(LocalDate.of(2025, 1, 1))
                .endDate(LocalDate.of(2025, 1, 5))
                .currentDay(1)
                .schedules(Arrays.asList(scheduleItem))
                .budget(2000000)
                .build();

        TripSharePreviewResponse response = TripSharePreviewResponse.builder()
                .suggestedHashtags(hashtags)
                .scheduleInfo(scheduleInfo)
                .build();

        when(postService.getTripSharePreview(eq(userId), eq(tripId)))
                .thenReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/community/posts/trip-share/preview")
                        .param("tripId", tripId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("일정 공유 미리보기 정보를 조회했습니다."))
                .andExpect(jsonPath("$.data.suggestedHashtags").isArray())
                .andExpect(jsonPath("$.data.suggestedHashtags.length()").value(3))
                .andExpect(jsonPath("$.data.suggestedHashtags[0].tagName").value("파리여행"))
                .andExpect(jsonPath("$.data.suggestedHashtags[0].tagType").value("CUSTOM_AI"))
                .andExpect(jsonPath("$.data.scheduleInfo.tripId").value(tripId))
                .andExpect(jsonPath("$.data.scheduleInfo.destination").value("파리"))
                .andExpect(jsonPath("$.data.scheduleInfo.currentDay").value(1))
                .andExpect(jsonPath("$.data.scheduleInfo.schedules").isArray())
                .andExpect(jsonPath("$.data.scheduleInfo.budget").value(2000000));
    }

    @Test
    @DisplayName("POST /api/v1/community/posts/trip-share/preview - 여행을 찾을 수 없음 (404)")
    void getTripSharePreview_TripNotFound() throws Exception {
        // given
        when(postService.getTripSharePreview(eq(userId), eq(tripId)))
                .thenThrow(new CustomException(ErrorCode.TRIP_NOT_FOUND));

        // when & then
        mockMvc.perform(post("/api/v1/community/posts/trip-share/preview")
                        .param("tripId", tripId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/v1/community/posts/trip-share/preview - 사용자가 여행 참여자가 아님 (403)")
    void getTripSharePreview_UserNotParticipant() throws Exception {
        // given
        when(postService.getTripSharePreview(eq(userId), eq(tripId)))
                .thenThrow(new CustomException(ErrorCode.USER_NOT_IN_TRIP));

        // when & then
        mockMvc.perform(post("/api/v1/community/posts/trip-share/preview")
                        .param("tripId", tripId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }
}
