package triB.triB.schedule.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import triB.triB.chat.entity.PlaceTag;
import triB.triB.room.entity.Room;
import triB.triB.room.entity.UserRoomId;
import triB.triB.room.repository.RoomRepository;
import triB.triB.room.repository.UserRoomRepository;
import triB.triB.schedule.dto.*;
import triB.triB.schedule.entity.Schedule;
import triB.triB.schedule.entity.TravelMode;
import triB.triB.schedule.entity.Trip;
import triB.triB.schedule.repository.ScheduleRepository;
import triB.triB.schedule.repository.TripRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScheduleService 단위 테스트")
class ScheduleServiceTest {

    @Mock
    private TripRepository tripRepository;

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private UserRoomRepository userRoomRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoutesApiService routesApiService;

    @InjectMocks
    private ScheduleService scheduleService;

    private Trip testTrip;
    private Room testRoom;
    private Schedule testSchedule1;
    private Schedule testSchedule2;
    private Long userId;
    private Long tripId;
    private Long roomId;

    @BeforeEach
    void setUp() {
        userId = 1L;
        tripId = 100L;
        roomId = 10L;

        // Test Room 생성
        testRoom = Room.builder()
                .roomId(roomId)
                .roomName("Test Room")
                .destination("Seoul")
                .startDate(LocalDate.of(2025, 1, 1))
                .endDate(LocalDate.of(2025, 1, 3))
                .build();

        // Test Trip 생성
        testTrip = Trip.builder()
                .tripId(tripId)
                .roomId(roomId)
                .destination("Seoul")
                .travelMode(TravelMode.DRIVE)
                .build();

        // Test Schedule 생성
        testSchedule1 = Schedule.builder()
                .scheduleId(1L)
                .tripId(tripId)
                .dayNumber(1)
                .date(LocalDate.of(2025, 1, 1))
                .visitOrder(1)
                .placeName("장소1")
                .placeTag(PlaceTag.TOURIST_SPOT)
                .latitude(37.5665)
                .longitude(126.9780)
                .isVisit(false)
                .arrival(LocalDateTime.of(2025, 1, 1, 9, 0))
                .departure(LocalDateTime.of(2025, 1, 1, 10, 0))
                .travelTime("30분")
                .build();

        testSchedule2 = Schedule.builder()
                .scheduleId(2L)
                .tripId(tripId)
                .dayNumber(1)
                .date(LocalDate.of(2025, 1, 1))
                .visitOrder(2)
                .placeName("장소2")
                .placeTag(PlaceTag.RESTAURANT)
                .latitude(37.5700)
                .longitude(126.9800)
                .isVisit(false)
                .arrival(LocalDateTime.of(2025, 1, 1, 10, 30))
                .departure(LocalDateTime.of(2025, 1, 1, 11, 30))
                .travelTime(null)
                .build();
    }

    @Test
    @DisplayName("일정 조회 성공")
    void getTripSchedules_Success() {
        // given
        Integer dayNumber = 1;
        List<Schedule> schedules = Arrays.asList(testSchedule1, testSchedule2);

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(testTrip));
        when(userRoomRepository.existsById(any(UserRoomId.class))).thenReturn(true);
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(testRoom));
        when(scheduleRepository.findByTripIdAndDayNumber(tripId, dayNumber)).thenReturn(schedules);

        // when
        TripScheduleResponse response = scheduleService.getTripSchedules(tripId, dayNumber, userId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getTripId()).isEqualTo(tripId);
        assertThat(response.getDestination()).isEqualTo("Seoul");
        assertThat(response.getCurrentDay()).isEqualTo(dayNumber);
        assertThat(response.getSchedules()).hasSize(2);
        assertThat(response.getStartDate()).isEqualTo(LocalDate.of(2025, 1, 1));
        assertThat(response.getEndDate()).isEqualTo(LocalDate.of(2025, 1, 3));

        verify(tripRepository, times(2)).findById(tripId);
        verify(scheduleRepository).findByTripIdAndDayNumber(tripId, dayNumber);
    }

    @Test
    @DisplayName("일정 조회 - dayNumber가 null일 때 기본값 1 사용")
    void getTripSchedules_WithDefaultDay() {
        // given
        Integer dayNumber = null;
        List<Schedule> schedules = Arrays.asList(testSchedule1);

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(testTrip));
        when(userRoomRepository.existsById(any(UserRoomId.class))).thenReturn(true);
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(testRoom));
        when(scheduleRepository.findByTripIdAndDayNumber(tripId, 1)).thenReturn(schedules);

        // when
        TripScheduleResponse response = scheduleService.getTripSchedules(tripId, dayNumber, userId);

        // then
        assertThat(response.getCurrentDay()).isEqualTo(1);
        verify(scheduleRepository).findByTripIdAndDayNumber(tripId, 1);
    }

    @Test
    @DisplayName("일정 조회 실패 - Trip이 존재하지 않음")
    void getTripSchedules_TripNotFound() {
        // given
        when(tripRepository.findById(tripId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> scheduleService.getTripSchedules(tripId, 1, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("여행을 찾을 수 없습니다");

        verify(tripRepository).findById(tripId);
        verify(scheduleRepository, never()).findByTripIdAndDayNumber(anyLong(), anyInt());
    }

    @Test
    @DisplayName("일정 조회 실패 - 사용자가 해당 여행에 권한이 없음")
    void getTripSchedules_UnauthorizedUser() {
        // given
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(testTrip));
        when(userRoomRepository.existsById(any(UserRoomId.class))).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> scheduleService.getTripSchedules(tripId, 1, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("해당 여행에 접근 권한이 없습니다");

        verify(userRoomRepository).existsById(any(UserRoomId.class));
    }

    @Test
    @DisplayName("방문 상태 변경 성공")
    void updateVisitStatus_Success() {
        // given
        Long scheduleId = 1L;
        VisitStatusUpdateRequest request = new VisitStatusUpdateRequest(true);

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(testTrip));
        when(userRoomRepository.existsById(any(UserRoomId.class))).thenReturn(true);
        when(scheduleRepository.findByScheduleIdAndTripId(scheduleId, tripId))
                .thenReturn(Optional.of(testSchedule1));

        // when
        VisitStatusUpdateResponse response = scheduleService.updateVisitStatus(tripId, scheduleId, request, userId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getScheduleId()).isEqualTo(scheduleId);
        assertThat(response.getIsVisit()).isTrue();
        assertThat(testSchedule1.getIsVisit()).isTrue();

        verify(scheduleRepository).findByScheduleIdAndTripId(scheduleId, tripId);
    }

    @Test
    @DisplayName("방문 상태 변경 실패 - Schedule이 존재하지 않음")
    void updateVisitStatus_ScheduleNotFound() {
        // given
        Long scheduleId = 999L;
        VisitStatusUpdateRequest request = new VisitStatusUpdateRequest(true);

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(testTrip));
        when(userRoomRepository.existsById(any(UserRoomId.class))).thenReturn(true);
        when(scheduleRepository.findByScheduleIdAndTripId(scheduleId, tripId))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> scheduleService.updateVisitStatus(tripId, scheduleId, request, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("일정을 찾을 수 없습니다");

        verify(scheduleRepository).findByScheduleIdAndTripId(scheduleId, tripId);
    }

    @Test
    @DisplayName("일정 순서 변경 성공")
    void reorderSchedule_Success() {
        // given
        Long scheduleId = 1L;
        ReorderScheduleRequest request = new ReorderScheduleRequest(2);

        List<Schedule> daySchedules = Arrays.asList(testSchedule1, testSchedule2);

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(testTrip));
        when(userRoomRepository.existsById(any(UserRoomId.class))).thenReturn(true);
        when(scheduleRepository.findByScheduleIdAndTripId(scheduleId, tripId))
                .thenReturn(Optional.of(testSchedule1));
        when(scheduleRepository.findByTripIdAndDayNumber(tripId, 1))
                .thenReturn(daySchedules);
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(testRoom));
        when(routesApiService.calculateTravelTime(anyDouble(), anyDouble(), anyDouble(), anyDouble(), any()))
                .thenReturn(30);
        when(routesApiService.formatMinutesToReadable(anyInt())).thenReturn("30분");

        // when
        TripScheduleResponse response = scheduleService.reorderSchedule(tripId, scheduleId, request, userId);

        // then
        assertThat(response).isNotNull();
        assertThat(testSchedule1.getVisitOrder()).isEqualTo(2);
        assertThat(testSchedule2.getVisitOrder()).isEqualTo(1);

        verify(scheduleRepository, atLeastOnce()).findByTripIdAndDayNumber(tripId, 1);
        verify(routesApiService, atLeastOnce()).calculateTravelTime(anyDouble(), anyDouble(), anyDouble(), anyDouble(), any());
    }

    @Test
    @DisplayName("일정 순서 변경 - 이동시간 재계산 확인")
    void reorderSchedule_RecalculatesTravelTimes() {
        // given
        Long scheduleId = 2L;
        ReorderScheduleRequest request = new ReorderScheduleRequest(1);

        List<Schedule> daySchedules = Arrays.asList(testSchedule1, testSchedule2);

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(testTrip));
        when(userRoomRepository.existsById(any(UserRoomId.class))).thenReturn(true);
        when(scheduleRepository.findByScheduleIdAndTripId(scheduleId, tripId))
                .thenReturn(Optional.of(testSchedule2));
        when(scheduleRepository.findByTripIdAndDayNumber(tripId, 1))
                .thenReturn(daySchedules);
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(testRoom));
        when(routesApiService.calculateTravelTime(anyDouble(), anyDouble(), anyDouble(), anyDouble(), any()))
                .thenReturn(25);
        when(routesApiService.formatMinutesToReadable(anyInt())).thenReturn("25분");

        // when
        scheduleService.reorderSchedule(tripId, scheduleId, request, userId);

        // then
        verify(routesApiService, atLeastOnce()).calculateTravelTime(anyDouble(), anyDouble(), anyDouble(), anyDouble(), eq(TravelMode.DRIVE));
    }

    @Test
    @DisplayName("일정 순서 변경 실패 - 유효하지 않은 순서")
    void reorderSchedule_InvalidOrder() {
        // given
        Long scheduleId = 1L;
        ReorderScheduleRequest request = new ReorderScheduleRequest(10); // 유효하지 않은 순서

        List<Schedule> daySchedules = Arrays.asList(testSchedule1, testSchedule2);

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(testTrip));
        when(userRoomRepository.existsById(any(UserRoomId.class))).thenReturn(true);
        when(scheduleRepository.findByScheduleIdAndTripId(scheduleId, tripId))
                .thenReturn(Optional.of(testSchedule1));
        when(scheduleRepository.findByTripIdAndDayNumber(tripId, 1))
                .thenReturn(daySchedules);

        // when & then
        assertThatThrownBy(() -> scheduleService.reorderSchedule(tripId, scheduleId, request, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("유효하지 않은 방문 순서입니다");
    }

    @Test
    @DisplayName("일정 추가 성공")
    void addScheduleToDay_Success() {
        // given
        AddScheduleRequest request = AddScheduleRequest.builder()
                .dayNumber(1)
                .placeName("새로운 장소")
                .placeTag(PlaceTag.CAFE)
                .latitude(37.5800)
                .longitude(126.9900)
                .stayMinutes(60)
                .build();

        List<Schedule> existingSchedules = Arrays.asList(testSchedule1, testSchedule2);

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(testTrip));
        when(userRoomRepository.existsById(any(UserRoomId.class))).thenReturn(true);
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(testRoom));
        when(scheduleRepository.findByTripIdAndDayNumber(tripId, 1))
                .thenReturn(existingSchedules);
        when(scheduleRepository.save(any(Schedule.class))).thenAnswer(invocation -> {
            Schedule schedule = invocation.getArgument(0);
            schedule.setScheduleId(3L);
            return schedule;
        });
        when(routesApiService.calculateTravelTime(anyDouble(), anyDouble(), anyDouble(), anyDouble(), any()))
                .thenReturn(20);
        when(routesApiService.formatMinutesToReadable(anyInt())).thenReturn("20분");

        // when
        ScheduleItemResponse response = scheduleService.addScheduleToDay(tripId, request, userId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getDisplayName()).isEqualTo("새로운 장소");
        assertThat(testSchedule2.getTravelTime()).isEqualTo("20분");

        verify(scheduleRepository).save(any(Schedule.class));
        verify(routesApiService).calculateTravelTime(anyDouble(), anyDouble(), anyDouble(), anyDouble(), eq(TravelMode.DRIVE));
    }

    @Test
    @DisplayName("일정 삭제 - 이후 일정 순서 재정렬")
    void deleteSchedule_ReordersSubsequent() {
        // given
        Long scheduleId = 1L;
        Schedule schedule3 = Schedule.builder()
                .scheduleId(3L)
                .tripId(tripId)
                .dayNumber(1)
                .visitOrder(3)
                .placeName("장소3")
                .placeTag(PlaceTag.TOURIST_SPOT)
                .latitude(37.5900)
                .longitude(127.0000)
                .isVisit(false)
                .arrival(LocalDateTime.of(2025, 1, 1, 12, 0))
                .departure(LocalDateTime.of(2025, 1, 1, 13, 0))
                .travelTime(null)
                .build();

        List<Schedule> remainingSchedules = Arrays.asList(testSchedule2, schedule3);

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(testTrip));
        when(userRoomRepository.existsById(any(UserRoomId.class))).thenReturn(true);
        when(scheduleRepository.findByScheduleIdAndTripId(scheduleId, tripId))
                .thenReturn(Optional.of(testSchedule1));
        when(scheduleRepository.findByTripIdAndDayNumber(tripId, 1))
                .thenReturn(remainingSchedules);
        when(routesApiService.calculateTravelTime(anyDouble(), anyDouble(), anyDouble(), anyDouble(), any()))
                .thenReturn(30);
        when(routesApiService.formatMinutesToReadable(anyInt())).thenReturn("30분");

        // when
        DeleteScheduleResponse response = scheduleService.deleteSchedule(tripId, scheduleId, userId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getDeletedScheduleId()).isEqualTo(scheduleId);
        assertThat(testSchedule2.getVisitOrder()).isEqualTo(1);
        assertThat(schedule3.getVisitOrder()).isEqualTo(2);

        verify(scheduleRepository).delete(testSchedule1);
        verify(scheduleRepository, atLeastOnce()).findByTripIdAndDayNumber(tripId, 1);
    }

    @Test
    @DisplayName("체류시간 수정 - departure 시간 업데이트")
    void updateStayDuration_UpdatesDeparture() {
        // given
        Long scheduleId = 1L;
        UpdateStayDurationRequest request = new UpdateStayDurationRequest(90);

        LocalDateTime originalArrival = testSchedule1.getArrival();
        List<Schedule> daySchedules = Arrays.asList(testSchedule1, testSchedule2);

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(testTrip));
        when(userRoomRepository.existsById(any(UserRoomId.class))).thenReturn(true);
        when(scheduleRepository.findByScheduleIdAndTripId(scheduleId, tripId))
                .thenReturn(Optional.of(testSchedule1));
        when(scheduleRepository.findByTripIdAndDayNumber(tripId, 1))
                .thenReturn(daySchedules);

        // when
        ScheduleItemResponse response = scheduleService.updateStayDuration(tripId, scheduleId, request, userId);

        // then
        assertThat(response).isNotNull();
        assertThat(testSchedule1.getDeparture()).isEqualTo(originalArrival.plusMinutes(90));

        verify(scheduleRepository).findByScheduleIdAndTripId(scheduleId, tripId);
    }

    @Test
    @DisplayName("방문 시간 수정 - 연쇄 계산 확인")
    void updateVisitTime_RecalculatesChain() {
        // given
        Long scheduleId = 1L;
        UpdateVisitTimeRequest request = new UpdateVisitTimeRequest(LocalTime.of(10, 0));

        List<Schedule> daySchedules = Arrays.asList(testSchedule1, testSchedule2);

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(testTrip));
        when(userRoomRepository.existsById(any(UserRoomId.class))).thenReturn(true);
        when(scheduleRepository.findByScheduleIdAndTripId(scheduleId, tripId))
                .thenReturn(Optional.of(testSchedule1));
        when(scheduleRepository.findByTripIdAndDayNumber(tripId, 1))
                .thenReturn(daySchedules);

        // when
        ScheduleItemResponse response = scheduleService.updateVisitTime(tripId, scheduleId, request, userId);

        // then
        assertThat(response).isNotNull();
        assertThat(testSchedule1.getArrival().toLocalTime()).isEqualTo(LocalTime.of(10, 0));

        verify(scheduleRepository, atLeastOnce()).findByTripIdAndDayNumber(tripId, 1);
    }

    @Test
    @DisplayName("숙소 변경 성공")
    void updateAccommodation_Success() {
        // given
        Schedule accommodationSchedule = Schedule.builder()
                .scheduleId(3L)
                .tripId(tripId)
                .dayNumber(1)
                .visitOrder(3)
                .placeName("기존 숙소")
                .placeTag(PlaceTag.HOME)
                .latitude(37.5600)
                .longitude(126.9700)
                .isVisit(false)
                .arrival(LocalDateTime.of(2025, 1, 1, 18, 0))
                .departure(LocalDateTime.of(2025, 1, 2, 9, 0))
                .travelTime(null)
                .build();

        UpdateAccommodationRequest request = new UpdateAccommodationRequest(1, "새로운 숙소", 37.5650, 126.9750);

        List<Schedule> daySchedules = Arrays.asList(testSchedule1, testSchedule2, accommodationSchedule);

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(testTrip));
        when(userRoomRepository.existsById(any(UserRoomId.class))).thenReturn(true);
        when(scheduleRepository.findByTripIdAndDayNumber(tripId, 1))
                .thenReturn(daySchedules);
        when(scheduleRepository.findByTripIdAndDayNumber(tripId, 2))
                .thenReturn(Arrays.asList());
        when(routesApiService.calculateTravelTime(anyDouble(), anyDouble(), anyDouble(), anyDouble(), any()))
                .thenReturn(15);
        when(routesApiService.formatMinutesToReadable(anyInt())).thenReturn("15분");

        // when
        ScheduleItemResponse response = scheduleService.updateAccommodation(tripId, request, userId);

        // then
        assertThat(response).isNotNull();
        assertThat(accommodationSchedule.getPlaceName()).isEqualTo("새로운 숙소");
        assertThat(accommodationSchedule.getLatitude()).isEqualTo(37.5650);
        assertThat(accommodationSchedule.getLongitude()).isEqualTo(126.9750);

        verify(routesApiService, atLeastOnce()).calculateTravelTime(anyDouble(), anyDouble(), anyDouble(), anyDouble(), eq(TravelMode.DRIVE));
    }

    @Test
    @DisplayName("숙소 변경 실패 - 숙소가 존재하지 않음")
    void updateAccommodation_NotFound() {
        // given
        UpdateAccommodationRequest request = new UpdateAccommodationRequest(1, "새로운 숙소", 37.5650, 126.9750);

        // HOME 태그가 없는 일정들만 반환
        List<Schedule> daySchedules = Arrays.asList(testSchedule1, testSchedule2);

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(testTrip));
        when(userRoomRepository.existsById(any(UserRoomId.class))).thenReturn(true);
        when(scheduleRepository.findByTripIdAndDayNumber(tripId, 1))
                .thenReturn(daySchedules);

        // when & then
        assertThatThrownBy(() -> scheduleService.updateAccommodation(tripId, request, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("해당 날짜에 숙소를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("scheduleId로 숙소 변경 성공")
    void updateAccommodationByScheduleId_Success() {
        // given
        Schedule touristSpot = Schedule.builder()
                .scheduleId(1L)
                .tripId(tripId)
                .trip(testTrip)
                .dayNumber(1)
                .visitOrder(1)
                .placeName("경복궁")
                .placeTag(PlaceTag.TOURIST_SPOT)
                .latitude(37.5788)
                .longitude(126.9770)
                .isVisit(false)
                .arrival(LocalDateTime.of(2025, 1, 1, 10, 0))
                .departure(LocalDateTime.of(2025, 1, 1, 11, 0))
                .travelTime("1시간")
                .build();

        Schedule accommodation = Schedule.builder()
                .scheduleId(100L)
                .tripId(tripId)
                .trip(testTrip)
                .dayNumber(1)
                .visitOrder(2)
                .placeName("명동호텔")
                .placeTag(PlaceTag.HOME)
                .latitude(37.5600)
                .longitude(126.9700)
                .isVisit(false)
                .arrival(LocalDateTime.of(2025, 1, 1, 12, 0))
                .departure(LocalDateTime.of(2025, 1, 1, 22, 0))
                .travelTime("1시간 30분")
                .build();

        Schedule nextDaySchedule = Schedule.builder()
                .scheduleId(2L)
                .tripId(tripId)
                .trip(testTrip)
                .dayNumber(2)
                .visitOrder(1)
                .placeName("남산타워")
                .placeTag(PlaceTag.TOURIST_SPOT)
                .latitude(37.5512)
                .longitude(126.9882)
                .isVisit(false)
                .arrival(LocalDateTime.of(2025, 1, 2, 9, 0))
                .departure(LocalDateTime.of(2025, 1, 2, 10, 0))
                .travelTime(null)
                .build();

        List<Schedule> daySchedules = Arrays.asList(touristSpot, accommodation);
        List<Schedule> nextDaySchedules = Arrays.asList(nextDaySchedule);

        when(scheduleRepository.findById(100L)).thenReturn(Optional.of(accommodation));
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(testTrip));
        when(scheduleRepository.findByTripIdAndDayNumber(tripId, 1)).thenReturn(daySchedules);
        when(scheduleRepository.findByTripIdAndDayNumber(tripId, 2)).thenReturn(nextDaySchedules);
        when(routesApiService.calculateTravelTime(anyDouble(), anyDouble(), anyDouble(), anyDouble(), any()))
                .thenReturn(50) // 경복궁 → 강남호텔: 50분
                .thenReturn(40); // 강남호텔 → 남산타워: 40분
        when(routesApiService.formatMinutesToReadable(50)).thenReturn("50분");
        when(routesApiService.formatMinutesToReadable(40)).thenReturn("40분");

        // when
        PreviewScheduleRequest request = new PreviewScheduleRequest();
        request.setDayNumber(1);
        ScheduleModificationItem modification = ScheduleModificationItem.builder()
                .modificationType(ModificationType.UPDATE_ACCOMMODATION)
                .scheduleId(100L)
                .placeName("강남호텔")
                .latitude(37.4979)
                .longitude(127.0276)
                .build();
        request.setModifications(Arrays.asList(modification));

        // 미리보기 API 호출
        when(userRoomRepository.existsById(any(UserRoomId.class))).thenReturn(true);
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(testRoom));

        TripScheduleResponse response = scheduleService.previewScheduleChanges(tripId, request, userId);

        // then
        assertThat(response).isNotNull();
        assertThat(accommodation.getPlaceName()).isEqualTo("강남호텔");
        assertThat(accommodation.getLatitude()).isEqualTo(37.4979);
        assertThat(accommodation.getLongitude()).isEqualTo(127.0276);
        assertThat(accommodation.getPlaceTag()).isEqualTo(PlaceTag.HOME);

        verify(routesApiService, atLeastOnce()).calculateTravelTime(anyDouble(), anyDouble(), anyDouble(), anyDouble(), eq(TravelMode.DRIVE));
    }

    @Test
    @DisplayName("scheduleId로 숙소 변경 실패 - 존재하지 않는 scheduleId")
    void updateAccommodationByScheduleId_NotFound() {
        // given
        Long nonExistentId = 99999L;
        when(scheduleRepository.findById(nonExistentId)).thenReturn(Optional.empty());
        when(userRoomRepository.existsById(any(UserRoomId.class))).thenReturn(true);
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(testTrip));

        // when & then
        PreviewScheduleRequest request = new PreviewScheduleRequest();
        request.setDayNumber(1);
        ScheduleModificationItem modification = ScheduleModificationItem.builder()
                .modificationType(ModificationType.UPDATE_ACCOMMODATION)
                .scheduleId(nonExistentId)
                .placeName("새로운 숙소")
                .latitude(37.5650)
                .longitude(126.9750)
                .build();
        request.setModifications(Arrays.asList(modification));

        assertThatThrownBy(() -> scheduleService.previewScheduleChanges(tripId, request, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("해당 일정을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("scheduleId로 숙소 변경 실패 - PlaceTag.HOME이 아닌 일정")
    void updateAccommodationByScheduleId_NotHome() {
        // given
        Schedule touristSpot = Schedule.builder()
                .scheduleId(1L)
                .tripId(tripId)
                .trip(testTrip)
                .dayNumber(1)
                .visitOrder(1)
                .placeName("경복궁")
                .placeTag(PlaceTag.TOURIST_SPOT) // HOME이 아님
                .latitude(37.5788)
                .longitude(126.9770)
                .isVisit(false)
                .arrival(LocalDateTime.of(2025, 1, 1, 10, 0))
                .departure(LocalDateTime.of(2025, 1, 1, 11, 0))
                .travelTime(null)
                .build();

        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(touristSpot));
        when(userRoomRepository.existsById(any(UserRoomId.class))).thenReturn(true);
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(testTrip));

        // when & then
        PreviewScheduleRequest request = new PreviewScheduleRequest();
        request.setDayNumber(1);
        ScheduleModificationItem modification = ScheduleModificationItem.builder()
                .modificationType(ModificationType.UPDATE_ACCOMMODATION)
                .scheduleId(1L)
                .placeName("새로운 숙소")
                .latitude(37.5650)
                .longitude(126.9750)
                .build();
        request.setModifications(Arrays.asList(modification));

        assertThatThrownBy(() -> scheduleService.previewScheduleChanges(tripId, request, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("숙소(PlaceTag.HOME)만 변경할 수 있습니다");
    }

    @Test
    @DisplayName("일괄 수정 - 숙소 변경 포함")
    void batchUpdate_WithAccommodationChange() {
        // given
        Schedule schedule1 = Schedule.builder()
                .scheduleId(1L)
                .tripId(tripId)
                .trip(testTrip)
                .dayNumber(1)
                .visitOrder(1)
                .placeName("경복궁")
                .placeTag(PlaceTag.TOURIST_SPOT)
                .latitude(37.5788)
                .longitude(126.9770)
                .isVisit(false)
                .arrival(LocalDateTime.of(2025, 1, 1, 10, 0))
                .departure(LocalDateTime.of(2025, 1, 1, 11, 0))
                .travelTime("1시간")
                .build();

        Schedule accommodation = Schedule.builder()
                .scheduleId(2L)
                .tripId(tripId)
                .trip(testTrip)
                .dayNumber(1)
                .visitOrder(2)
                .placeName("명동호텔")
                .placeTag(PlaceTag.HOME)
                .latitude(37.5600)
                .longitude(126.9700)
                .isVisit(false)
                .arrival(LocalDateTime.of(2025, 1, 1, 12, 0))
                .departure(LocalDateTime.of(2025, 1, 1, 22, 0))
                .travelTime(null)
                .build();

        Schedule schedule3 = Schedule.builder()
                .scheduleId(3L)
                .tripId(tripId)
                .trip(testTrip)
                .dayNumber(1)
                .visitOrder(3)
                .placeName("인사동")
                .placeTag(PlaceTag.TOURIST_SPOT)
                .latitude(37.5730)
                .longitude(126.9850)
                .isVisit(false)
                .arrival(LocalDateTime.of(2025, 1, 1, 13, 0))
                .departure(LocalDateTime.of(2025, 1, 1, 14, 0))
                .travelTime(null)
                .build();

        List<Schedule> daySchedules = Arrays.asList(schedule1, accommodation, schedule3);

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(testTrip));
        when(userRoomRepository.existsById(any(UserRoomId.class))).thenReturn(true);
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(testRoom));
        when(scheduleRepository.findById(2L)).thenReturn(Optional.of(accommodation));
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(schedule1));
        when(scheduleRepository.findByTripIdAndDayNumber(tripId, 1)).thenReturn(daySchedules);
        when(scheduleRepository.findByTripIdAndDayNumber(tripId, 2)).thenReturn(Arrays.asList());
        when(routesApiService.calculateTravelTime(anyDouble(), anyDouble(), anyDouble(), anyDouble(), any()))
                .thenReturn(30);
        when(routesApiService.formatMinutesToReadable(anyInt())).thenReturn("30분");

        // when
        BatchUpdateScheduleRequest request = new BatchUpdateScheduleRequest();
        request.setDayNumber(1);

        ScheduleModificationItem reorder = ScheduleModificationItem.builder()
                .modificationType(ModificationType.REORDER)
                .scheduleId(1L)
                .newVisitOrder(2)
                .build();

        ScheduleModificationItem updateAccommodation = ScheduleModificationItem.builder()
                .modificationType(ModificationType.UPDATE_ACCOMMODATION)
                .scheduleId(2L)
                .placeName("이태원호텔")
                .latitude(37.5345)
                .longitude(126.9949)
                .build();

        ScheduleModificationItem updateDuration = ScheduleModificationItem.builder()
                .modificationType(ModificationType.UPDATE_STAY_DURATION)
                .scheduleId(1L)
                .stayMinutes(90)
                .build();

        request.setModifications(Arrays.asList(reorder, updateAccommodation, updateDuration));

        TripScheduleResponse response = scheduleService.batchUpdateSchedule(tripId, request, userId);

        // then
        assertThat(response).isNotNull();
        assertThat(accommodation.getPlaceName()).isEqualTo("이태원호텔");
        assertThat(accommodation.getLatitude()).isEqualTo(37.5345);
        assertThat(accommodation.getLongitude()).isEqualTo(126.9949);

        verify(routesApiService, atLeastOnce()).calculateTravelTime(anyDouble(), anyDouble(), anyDouble(), anyDouble(), eq(TravelMode.DRIVE));
    }

    @Test
    @DisplayName("미리보기 - 숙소 변경 포함")
    void preview_WithAccommodationChange() {
        // given
        Schedule accommodation = Schedule.builder()
                .scheduleId(100L)
                .tripId(tripId)
                .trip(testTrip)
                .dayNumber(1)
                .visitOrder(2)
                .placeName("명동호텔")
                .placeTag(PlaceTag.HOME)
                .latitude(37.5600)
                .longitude(126.9700)
                .isVisit(false)
                .arrival(LocalDateTime.of(2025, 1, 1, 12, 0))
                .departure(LocalDateTime.of(2025, 1, 1, 22, 0))
                .travelTime(null)
                .build();

        String originalPlaceName = accommodation.getPlaceName();
        Double originalLatitude = accommodation.getLatitude();
        Double originalLongitude = accommodation.getLongitude();

        List<Schedule> daySchedules = Arrays.asList(testSchedule1, accommodation);

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(testTrip));
        when(userRoomRepository.existsById(any(UserRoomId.class))).thenReturn(true);
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(testRoom));
        when(scheduleRepository.findById(100L)).thenReturn(Optional.of(accommodation));
        when(scheduleRepository.findByTripIdAndDayNumber(tripId, 1)).thenReturn(daySchedules);
        when(scheduleRepository.findByTripIdAndDayNumber(tripId, 2)).thenReturn(Arrays.asList());
        when(routesApiService.calculateTravelTime(anyDouble(), anyDouble(), anyDouble(), anyDouble(), any()))
                .thenReturn(40);
        when(routesApiService.formatMinutesToReadable(anyInt())).thenReturn("40분");

        // when
        PreviewScheduleRequest request = new PreviewScheduleRequest();
        request.setDayNumber(1);

        ScheduleModificationItem modification = ScheduleModificationItem.builder()
                .modificationType(ModificationType.UPDATE_ACCOMMODATION)
                .scheduleId(100L)
                .placeName("신라호텔")
                .latitude(37.5555)
                .longitude(126.9999)
                .build();

        request.setModifications(Arrays.asList(modification));

        TripScheduleResponse response = scheduleService.previewScheduleChanges(tripId, request, userId);

        // then
        assertThat(response).isNotNull();
        // 미리보기에서는 값이 변경되었지만 트랜잭션 롤백으로 인해 DB에 저장되지 않음을 확인
        assertThat(accommodation.getPlaceName()).isEqualTo("신라호텔");
        assertThat(accommodation.getLatitude()).isEqualTo(37.5555);
        assertThat(accommodation.getLongitude()).isEqualTo(126.9999);

        verify(routesApiService, atLeastOnce()).calculateTravelTime(anyDouble(), anyDouble(), anyDouble(), anyDouble(), eq(TravelMode.DRIVE));
    }

    @Test
    @DisplayName("일정 비용 정보 조회 성공 - 비용 정보가 있는 경우")
    void getScheduleCost_Success_WithCostInfo() {
        // given
        Long scheduleId = 1L;
        Schedule scheduleWithCost = Schedule.builder()
                .scheduleId(scheduleId)
                .tripId(tripId)
                .dayNumber(1)
                .date(LocalDate.of(2025, 1, 1))
                .visitOrder(1)
                .placeName("에버랜드")
                .placeTag(PlaceTag.TOURIST_SPOT)
                .latitude(37.5665)
                .longitude(126.9780)
                .isVisit(false)
                .arrival(LocalDateTime.of(2025, 1, 1, 9, 0))
                .departure(LocalDateTime.of(2025, 1, 1, 10, 0))
                .travelTime("30분")
                .estimatedCost(50000)
                .costExplanation("입장료 및 식사 비용 포함")
                .build();

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(testTrip));
        when(userRoomRepository.existsById(any(UserRoomId.class))).thenReturn(true);
        when(scheduleRepository.findByScheduleIdAndTripId(scheduleId, tripId))
                .thenReturn(Optional.of(scheduleWithCost));

        // when
        ScheduleCostResponse response = scheduleService.getScheduleCost(tripId, scheduleId, userId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getScheduleId()).isEqualTo(scheduleId);
        assertThat(response.getEstimatedCost()).isEqualTo(50000);
        assertThat(response.getCostExplanation()).isEqualTo("입장료 및 식사 비용 포함");

        verify(scheduleRepository).findByScheduleIdAndTripId(scheduleId, tripId);
    }

    @Test
    @DisplayName("일정 비용 정보 조회 성공 - 비용 정보가 없는 경우 (null)")
    void getScheduleCost_Success_WithoutCostInfo() {
        // given
        Long scheduleId = 1L;
        Schedule scheduleWithoutCost = Schedule.builder()
                .scheduleId(scheduleId)
                .tripId(tripId)
                .dayNumber(1)
                .date(LocalDate.of(2025, 1, 1))
                .visitOrder(1)
                .placeName("카페")
                .placeTag(PlaceTag.RESTAURANT)
                .latitude(37.5665)
                .longitude(126.9780)
                .isVisit(false)
                .arrival(LocalDateTime.of(2025, 1, 1, 9, 0))
                .departure(LocalDateTime.of(2025, 1, 1, 10, 0))
                .travelTime("30분")
                .estimatedCost(null)
                .costExplanation(null)
                .build();

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(testTrip));
        when(userRoomRepository.existsById(any(UserRoomId.class))).thenReturn(true);
        when(scheduleRepository.findByScheduleIdAndTripId(scheduleId, tripId))
                .thenReturn(Optional.of(scheduleWithoutCost));

        // when
        ScheduleCostResponse response = scheduleService.getScheduleCost(tripId, scheduleId, userId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getScheduleId()).isEqualTo(scheduleId);
        assertThat(response.getEstimatedCost()).isNull();
        assertThat(response.getCostExplanation()).isNull();

        verify(scheduleRepository).findByScheduleIdAndTripId(scheduleId, tripId);
    }

    @Test
    @DisplayName("일정 비용 정보 조회 실패 - 존재하지 않는 일정")
    void getScheduleCost_Fail_ScheduleNotFound() {
        // given
        Long scheduleId = 999L;

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(testTrip));
        when(userRoomRepository.existsById(any(UserRoomId.class))).thenReturn(true);
        when(scheduleRepository.findByScheduleIdAndTripId(scheduleId, tripId))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> scheduleService.getScheduleCost(tripId, scheduleId, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("일정을 찾을 수 없습니다.");

        verify(scheduleRepository).findByScheduleIdAndTripId(scheduleId, tripId);
    }

    @Test
    @DisplayName("일정 비용 정보 조회 실패 - 권한 없는 사용자")
    void getScheduleCost_Fail_Unauthorized() {
        // given
        Long scheduleId = 1L;
        Long unauthorizedUserId = 999L;

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(testTrip));
        when(userRoomRepository.existsById(any(UserRoomId.class))).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> scheduleService.getScheduleCost(tripId, scheduleId, unauthorizedUserId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("해당 여행에 접근 권한이 없습니다.");

        verify(userRoomRepository).existsById(any(UserRoomId.class));
        verify(scheduleRepository, never()).findByScheduleIdAndTripId(anyLong(), anyLong());
    }

    @Test
    @DisplayName("숙박 비용 정보 조회 성공 - 정보가 있는 경우")
    void getAccommodationCost_Success_WithInfo() {
        // given
        Trip tripWithAccommodationInfo = Trip.builder()
                .tripId(tripId)
                .roomId(roomId)
                .destination("Seoul")
                .accommodationCostInfo("1박당 15만원 예상")
                .build();

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(tripWithAccommodationInfo));
        when(userRoomRepository.existsById(any(UserRoomId.class))).thenReturn(true);

        // when
        AccommodationCostResponse response = scheduleService.getAccommodationCost(tripId, userId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getAccommodationCostInfo()).isEqualTo("1박당 15만원 예상");

        verify(tripRepository, times(2)).findById(tripId);
        verify(userRoomRepository).existsById(any(UserRoomId.class));
    }

    @Test
    @DisplayName("숙박 비용 정보 조회 성공 - 정보가 없는 경우 (null)")
    void getAccommodationCost_Success_WithoutInfo() {
        // given
        Trip tripWithoutAccommodationInfo = Trip.builder()
                .tripId(tripId)
                .roomId(roomId)
                .destination("Seoul")
                .accommodationCostInfo(null)
                .build();

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(tripWithoutAccommodationInfo));
        when(userRoomRepository.existsById(any(UserRoomId.class))).thenReturn(true);

        // when
        AccommodationCostResponse response = scheduleService.getAccommodationCost(tripId, userId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getAccommodationCostInfo()).isNull();

        verify(tripRepository, times(2)).findById(tripId);
    }

    @Test
    @DisplayName("숙박 비용 정보 조회 실패 - 존재하지 않는 여행")
    void getAccommodationCost_Fail_TripNotFound() {
        // given
        Long invalidTripId = 999L;

        when(tripRepository.findById(invalidTripId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> scheduleService.getAccommodationCost(invalidTripId, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("여행을 찾을 수 없습니다.");

        verify(tripRepository).findById(invalidTripId);
    }

    @Test
    @DisplayName("숙박 비용 정보 조회 실패 - 권한 없는 사용자")
    void getAccommodationCost_Fail_Unauthorized() {
        // given
        Long unauthorizedUserId = 999L;

        when(tripRepository.findById(tripId)).thenReturn(Optional.of(testTrip));
        when(userRoomRepository.existsById(any(UserRoomId.class))).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> scheduleService.getAccommodationCost(tripId, unauthorizedUserId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("해당 여행에 접근 권한이 없습니다.");

        verify(userRoomRepository).existsById(any(UserRoomId.class));
    }
}
