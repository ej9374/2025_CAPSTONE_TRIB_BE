package triB.triB.schedule.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import triB.triB.chat.entity.PlaceTag;
import triB.triB.global.security.JwtAuthenticationFilter;
import triB.triB.global.security.JwtProvider;
import triB.triB.global.security.UserPrincipal;
import triB.triB.schedule.dto.*;

import java.time.LocalTime;
import triB.triB.schedule.service.ScheduleService;
import triB.triB.schedule.service.TripService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ScheduleController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("ScheduleController 통합 테스트")
class ScheduleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ScheduleService scheduleService;

    @MockBean
    private TripService tripService;

    @MockBean
    private JwtProvider jwtProvider;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private UserPrincipal userPrincipal;
    private Long tripId;
    private Long scheduleId;
    private Long userId;
    private TripScheduleResponse tripScheduleResponse;
    private ScheduleItemResponse scheduleItemResponse;

    @BeforeEach
    void setUp() {
        tripId = 100L;
        scheduleId = 1L;
        userId = 1L;

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

        // ScheduleItemResponse 생성
        scheduleItemResponse = ScheduleItemResponse.builder()
                .scheduleId(scheduleId)
                .displayName("경복궁")
                .arrival(LocalDateTime.of(2025, 1, 1, 9, 0))
                .departure(LocalDateTime.of(2025, 1, 1, 10, 0))
                .placeTag(PlaceTag.TOURIST_SPOT)
                .travelTime(30)
                .visitOrder(1)
                .isVisit(false)
                .build();

        // TripScheduleResponse 생성
        tripScheduleResponse = TripScheduleResponse.builder()
                .tripId(tripId)
                .destination("Seoul")
                .startDate(LocalDate.of(2025, 1, 1))
                .endDate(LocalDate.of(2025, 1, 3))
                .currentDay(1)
                .schedules(Arrays.asList(scheduleItemResponse))
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/trips/{tripId}/schedules - 일정 조회 성공")
    void getTripSchedules_Success() throws Exception {
        // given
        when(scheduleService.getTripSchedules(eq(tripId), eq(1), eq(userId)))
                .thenReturn(tripScheduleResponse);

        // when & then
        mockMvc.perform(get("/api/v1/trips/{tripId}/schedules", tripId)
                        .param("dayNumber", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("일정을 조회했습니다."))
                .andExpect(jsonPath("$.data.tripId").value(tripId))
                .andExpect(jsonPath("$.data.destination").value("Seoul"))
                .andExpect(jsonPath("$.data.currentDay").value(1))
                .andExpect(jsonPath("$.data.schedules").isArray())
                .andExpect(jsonPath("$.data.schedules[0].scheduleId").value(scheduleId));
    }

    @Test
    @DisplayName("GET /api/v1/trips/{tripId}/schedules - dayNumber 파라미터 없이 조회")
    void getTripSchedules_WithoutDayNumber() throws Exception {
        // given
        when(scheduleService.getTripSchedules(eq(tripId), eq(1), eq(userId)))
                .thenReturn(tripScheduleResponse);

        // when & then
        mockMvc.perform(get("/api/v1/trips/{tripId}/schedules", tripId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.currentDay").value(1));
    }

    @Test
    @DisplayName("PATCH /api/v1/trips/{tripId}/schedules/{scheduleId}/visit-status - 방문 상태 변경 성공")
    void updateVisitStatus_Success() throws Exception {
        // given
        VisitStatusUpdateRequest request = new VisitStatusUpdateRequest(true);
        VisitStatusUpdateResponse response = VisitStatusUpdateResponse.builder()
                .scheduleId(scheduleId)
                .isVisit(true)
                .build();

        when(scheduleService.updateVisitStatus(eq(tripId), eq(scheduleId), any(), eq(userId)))
                .thenReturn(response);

        // when & then
        mockMvc.perform(patch("/api/v1/trips/{tripId}/schedules/{scheduleId}/visit-status", tripId, scheduleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("방문 상태를 변경했습니다."))
                .andExpect(jsonPath("$.data.scheduleId").value(scheduleId))
                .andExpect(jsonPath("$.data.isVisit").value(true));
    }

    @Test
    @DisplayName("PATCH /api/v1/trips/{tripId}/schedules/{scheduleId}/reorder - 일정 순서 변경 성공")
    void reorderSchedule_Success() throws Exception {
        // given
        ReorderScheduleRequest request = new ReorderScheduleRequest(2);

        when(scheduleService.reorderSchedule(eq(tripId), eq(scheduleId), any(), eq(userId)))
                .thenReturn(tripScheduleResponse);

        // when & then
        mockMvc.perform(patch("/api/v1/trips/{tripId}/schedules/{scheduleId}/reorder", tripId, scheduleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("일정 순서를 변경했습니다."))
                .andExpect(jsonPath("$.data.tripId").value(tripId));
    }

    @Test
    @DisplayName("PATCH /api/v1/trips/{tripId}/schedules/{scheduleId}/stay-duration - 체류시간 수정 성공")
    void updateStayDuration_Success() throws Exception {
        // given
        UpdateStayDurationRequest request = new UpdateStayDurationRequest(90);

        when(scheduleService.updateStayDuration(eq(tripId), eq(scheduleId), any(), eq(userId)))
                .thenReturn(scheduleItemResponse);

        // when & then
        mockMvc.perform(patch("/api/v1/trips/{tripId}/schedules/{scheduleId}/stay-duration", tripId, scheduleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("체류시간을 수정했습니다."))
                .andExpect(jsonPath("$.data.scheduleId").value(scheduleId));
    }

    @Test
    @DisplayName("POST /api/v1/trips/{tripId}/schedules - 일정 추가 성공 (201)")
    void addSchedule_Success() throws Exception {
        // given
        AddScheduleRequest request = AddScheduleRequest.builder()
                .dayNumber(1)
                .placeName("남산타워")
                .placeTag(PlaceTag.TOURIST_SPOT)
                .latitude(37.5512)
                .longitude(126.9882)
                .stayMinutes(60)
                .build();

        when(scheduleService.addScheduleToDay(eq(tripId), any(), eq(userId)))
                .thenReturn(scheduleItemResponse);

        // when & then
        mockMvc.perform(post("/api/v1/trips/{tripId}/schedules", tripId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.message").value("일정을 추가했습니다."))
                .andExpect(jsonPath("$.data.scheduleId").value(scheduleId));
    }

    @Test
    @DisplayName("DELETE /api/v1/trips/{tripId}/schedules/{scheduleId} - 일정 삭제 성공")
    void deleteSchedule_Success() throws Exception {
        // given
        DeleteScheduleResponse response = DeleteScheduleResponse.builder()
                .deletedScheduleId(scheduleId)
                .build();

        when(scheduleService.deleteSchedule(eq(tripId), eq(scheduleId), eq(userId)))
                .thenReturn(response);

        // when & then
        mockMvc.perform(delete("/api/v1/trips/{tripId}/schedules/{scheduleId}", tripId, scheduleId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("일정을 삭제했습니다."))
                .andExpect(jsonPath("$.data.deletedScheduleId").value(scheduleId));
    }

    @Test
    @DisplayName("POST /api/v1/trips/{tripId}/schedules - Validation 실패 (400)")
    void addSchedule_ValidationFailed() throws Exception {
        // given - placeName이 없는 잘못된 요청
        String invalidRequest = """
                {
                    "dayNumber": 1,
                    "placeTag": "TOURIST_SPOT",
                    "latitude": 37.5512,
                    "longitude": 126.9882,
                    "stayMinutes": 60
                }
                """;

        // when & then
        mockMvc.perform(post("/api/v1/trips/{tripId}/schedules", tripId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/trips/{tripId}/schedules/preview - 일정 변경 미리보기 성공")
    void previewScheduleChanges_Success() throws Exception {
        // given
        ScheduleModificationItem modification1 = ScheduleModificationItem.builder()
                .modificationType(ModificationType.REORDER)
                .scheduleId(scheduleId)
                .newVisitOrder(2)
                .build();

        ScheduleModificationItem modification2 = ScheduleModificationItem.builder()
                .modificationType(ModificationType.UPDATE_STAY_DURATION)
                .scheduleId(scheduleId)
                .stayMinutes(90)
                .build();

        PreviewScheduleRequest request = PreviewScheduleRequest.builder()
                .dayNumber(1)
                .modifications(Arrays.asList(modification1, modification2))
                .build();

        when(scheduleService.previewScheduleChanges(eq(tripId), any(), eq(userId)))
                .thenReturn(tripScheduleResponse);

        // when & then
        mockMvc.perform(post("/api/v1/trips/{tripId}/schedules/preview", tripId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("일정 변경사항을 미리보기합니다."))
                .andExpect(jsonPath("$.data.tripId").value(tripId));
    }

    @Test
    @DisplayName("POST /api/v1/trips/{tripId}/schedules/batch-update - 일정 일괄 수정 성공")
    void batchUpdateSchedule_Success() throws Exception {
        // given
        ScheduleModificationItem modification = ScheduleModificationItem.builder()
                .modificationType(ModificationType.ADD)
                .dayNumber(1)
                .placeName("남산타워")
                .placeTag(PlaceTag.TOURIST_SPOT)
                .latitude(37.5512)
                .longitude(126.9882)
                .stayMinutes(60)
                .build();

        BatchUpdateScheduleRequest request = BatchUpdateScheduleRequest.builder()
                .dayNumber(1)
                .modifications(Arrays.asList(modification))
                .build();

        when(scheduleService.batchUpdateSchedule(eq(tripId), any(), eq(userId)))
                .thenReturn(tripScheduleResponse);

        // when & then
        mockMvc.perform(post("/api/v1/trips/{tripId}/schedules/batch-update", tripId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("일정 변경사항을 저장했습니다."))
                .andExpect(jsonPath("$.data.tripId").value(tripId));
    }

    @Test
    @DisplayName("POST /api/v1/trips/{tripId}/schedules/preview - 복합 변경사항 미리보기")
    void previewScheduleChanges_MultipleModifications() throws Exception {
        // given - 삭제, 추가, 순서변경을 모두 포함
        ScheduleModificationItem delete = ScheduleModificationItem.builder()
                .modificationType(ModificationType.DELETE)
                .scheduleId(2L)
                .build();

        ScheduleModificationItem add = ScheduleModificationItem.builder()
                .modificationType(ModificationType.ADD)
                .dayNumber(1)
                .placeName("북촌한옥마을")
                .placeTag(PlaceTag.TOURIST_SPOT)
                .latitude(37.5834)
                .longitude(126.9830)
                .stayMinutes(90)
                .build();

        ScheduleModificationItem reorder = ScheduleModificationItem.builder()
                .modificationType(ModificationType.REORDER)
                .scheduleId(scheduleId)
                .newVisitOrder(1)
                .build();

        ScheduleModificationItem updateTime = ScheduleModificationItem.builder()
                .modificationType(ModificationType.UPDATE_VISIT_TIME)
                .scheduleId(scheduleId)
                .newArrivalTime(LocalTime.of(10, 30))
                .build();

        PreviewScheduleRequest request = PreviewScheduleRequest.builder()
                .dayNumber(1)
                .modifications(Arrays.asList(delete, add, reorder, updateTime))
                .build();

        when(scheduleService.previewScheduleChanges(eq(tripId), any(), eq(userId)))
                .thenReturn(tripScheduleResponse);

        // when & then
        mockMvc.perform(post("/api/v1/trips/{tripId}/schedules/preview", tripId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    @DisplayName("POST /api/v1/trips/{tripId}/schedules/batch-update - 숙소 변경 포함")
    void batchUpdateSchedule_WithAccommodation() throws Exception {
        // given
        ScheduleModificationItem accommodationUpdate = ScheduleModificationItem.builder()
                .modificationType(ModificationType.UPDATE_ACCOMMODATION)
                .scheduleId(100L)
                .placeName("롯데호텔")
                .latitude(37.5665)
                .longitude(126.9780)
                .build();

        BatchUpdateScheduleRequest request = BatchUpdateScheduleRequest.builder()
                .dayNumber(1)
                .modifications(Arrays.asList(accommodationUpdate))
                .build();

        // Mock response에 변경된 숙소 정보 포함
        ScheduleItemResponse accommodationResponse = ScheduleItemResponse.builder()
                .scheduleId(100L)
                .displayName("롯데호텔")
                .placeTag(PlaceTag.HOME)
                .arrival(LocalDateTime.of(2025, 1, 1, 18, 0))
                .departure(LocalDateTime.of(2025, 1, 2, 9, 0))
                .travelTime(60)
                .visitOrder(2)
                .isVisit(false)
                .build();

        TripScheduleResponse mockResponse = TripScheduleResponse.builder()
                .tripId(tripId)
                .destination("Seoul")
                .startDate(LocalDate.of(2025, 1, 1))
                .endDate(LocalDate.of(2025, 1, 3))
                .currentDay(1)
                .schedules(Arrays.asList(scheduleItemResponse, accommodationResponse))
                .build();

        when(scheduleService.batchUpdateSchedule(eq(tripId), any(), eq(userId)))
                .thenReturn(mockResponse);

        // when & then
        mockMvc.perform(post("/api/v1/trips/{tripId}/schedules/batch-update", tripId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("일정 변경사항을 저장했습니다."))
                .andExpect(jsonPath("$.data.tripId").value(tripId))
                .andExpect(jsonPath("$.data.schedules[?(@.scheduleId == 100)].displayName").value("롯데호텔"));

        verify(scheduleService).batchUpdateSchedule(eq(tripId), any(BatchUpdateScheduleRequest.class), eq(userId));
    }

    @Test
    @DisplayName("POST /api/v1/trips/{tripId}/schedules/preview - 숙소 변경 미리보기")
    void previewScheduleChanges_WithAccommodation() throws Exception {
        // given
        ScheduleModificationItem accommodationUpdate = ScheduleModificationItem.builder()
                .modificationType(ModificationType.UPDATE_ACCOMMODATION)
                .scheduleId(100L)
                .placeName("신라호텔")
                .latitude(37.5555)
                .longitude(126.9999)
                .build();

        PreviewScheduleRequest request = PreviewScheduleRequest.builder()
                .dayNumber(1)
                .modifications(Arrays.asList(accommodationUpdate))
                .build();

        // Mock response에 변경된 숙소 정보 포함
        ScheduleItemResponse accommodationResponse = ScheduleItemResponse.builder()
                .scheduleId(100L)
                .displayName("신라호텔")
                .placeTag(PlaceTag.HOME)
                .arrival(LocalDateTime.of(2025, 1, 1, 18, 0))
                .departure(LocalDateTime.of(2025, 1, 2, 9, 0))
                .travelTime(75)
                .visitOrder(2)
                .isVisit(false)
                .build();

        TripScheduleResponse mockResponse = TripScheduleResponse.builder()
                .tripId(tripId)
                .destination("Seoul")
                .startDate(LocalDate.of(2025, 1, 1))
                .endDate(LocalDate.of(2025, 1, 3))
                .currentDay(1)
                .schedules(Arrays.asList(scheduleItemResponse, accommodationResponse))
                .build();

        when(scheduleService.previewScheduleChanges(eq(tripId), any(), eq(userId)))
                .thenReturn(mockResponse);

        // when & then
        mockMvc.perform(post("/api/v1/trips/{tripId}/schedules/preview", tripId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("일정 변경사항을 미리보기합니다."))
                .andExpect(jsonPath("$.data.tripId").value(tripId))
                .andExpect(jsonPath("$.data.schedules[?(@.scheduleId == 100)].displayName").value("신라호텔"));

        verify(scheduleService).previewScheduleChanges(eq(tripId), any(PreviewScheduleRequest.class), eq(userId));
    }

    @Test
    @DisplayName("POST /api/v1/trips/{tripId}/schedules/batch-update - HOME 아닌 일정 변경 시 실패")
    void batchUpdateSchedule_AccommodationNotHome_BadRequest() throws Exception {
        // given
        ScheduleModificationItem accommodationUpdate = ScheduleModificationItem.builder()
                .modificationType(ModificationType.UPDATE_ACCOMMODATION)
                .scheduleId(scheduleId) // TOURIST_SPOT 일정
                .placeName("롯데호텔")
                .latitude(37.5665)
                .longitude(126.9780)
                .build();

        BatchUpdateScheduleRequest request = BatchUpdateScheduleRequest.builder()
                .dayNumber(1)
                .modifications(Arrays.asList(accommodationUpdate))
                .build();

        when(scheduleService.batchUpdateSchedule(eq(tripId), any(), eq(userId)))
                .thenThrow(new IllegalArgumentException("숙소(PlaceTag.HOME)만 변경할 수 있습니다."));

        // when & then
        mockMvc.perform(post("/api/v1/trips/{tripId}/schedules/batch-update", tripId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("숙소(PlaceTag.HOME)만 변경할 수 있습니다."));
    }

    @Test
    @DisplayName("GET /api/v1/trips/{tripId}/accommodation-cost - 숙박 비용 정보 조회 성공")
    void getAccommodationCost_Success() throws Exception {
        // given
        AccommodationCostResponse mockResponse = AccommodationCostResponse.builder()
                .accommodationCostInfo("1박당 15만원 예상")
                .build();

        when(scheduleService.getAccommodationCost(eq(tripId), eq(userId)))
                .thenReturn(mockResponse);

        // when & then
        mockMvc.perform(get("/api/v1/trips/{tripId}/accommodation-cost", tripId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("숙박 비용 정보를 조회했습니다."))
                .andExpect(jsonPath("$.data.accommodationCostInfo").value("1박당 15만원 예상"));
    }

    @Test
    @DisplayName("GET /api/v1/trips/{tripId}/accommodation-cost - 숙박 비용 정보가 없는 경우")
    void getAccommodationCost_Success_NullInfo() throws Exception {
        // given
        AccommodationCostResponse mockResponse = AccommodationCostResponse.builder()
                .accommodationCostInfo(null)
                .build();

        when(scheduleService.getAccommodationCost(eq(tripId), eq(userId)))
                .thenReturn(mockResponse);

        // when & then
        mockMvc.perform(get("/api/v1/trips/{tripId}/accommodation-cost", tripId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accommodationCostInfo").isEmpty());
    }
}
