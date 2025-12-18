package triB.triB.schedule.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import triB.triB.auth.entity.User;
import triB.triB.auth.repository.UserRepository;
import triB.triB.room.entity.Room;
import triB.triB.schedule.event.ScheduleBatchUpdatedEvent;
import triB.triB.room.entity.UserRoomId;
import triB.triB.room.repository.RoomRepository;
import triB.triB.room.repository.UserRoomRepository;
import triB.triB.chat.entity.PlaceTag;
import triB.triB.schedule.dto.AccommodationCostResponse;
import triB.triB.schedule.dto.AddScheduleRequest;
import triB.triB.schedule.dto.BatchUpdateScheduleRequest;
import triB.triB.schedule.dto.DeleteScheduleResponse;
import triB.triB.schedule.dto.ModificationType;
import triB.triB.schedule.dto.PreviewScheduleRequest;
import triB.triB.schedule.dto.ReorderScheduleRequest;
import triB.triB.schedule.dto.ScheduleCostResponse;
import triB.triB.schedule.dto.ScheduleItemResponse;
import triB.triB.schedule.dto.ScheduleItemWithLocationResponse;
import triB.triB.schedule.dto.ScheduleModificationItem;
import triB.triB.schedule.dto.TripScheduleResponse;
import triB.triB.schedule.dto.TripScheduleWithLocationResponse;
import triB.triB.schedule.dto.UpdateAccommodationRequest;
import triB.triB.schedule.dto.UpdateStayDurationRequest;
import triB.triB.schedule.dto.UpdateVisitTimeRequest;
import triB.triB.schedule.dto.VisitStatusUpdateRequest;
import triB.triB.schedule.dto.VisitStatusUpdateResponse;
import triB.triB.schedule.entity.Schedule;
import triB.triB.schedule.entity.TravelMode;
import triB.triB.schedule.entity.Trip;
import triB.triB.schedule.repository.ScheduleRepository;
import triB.triB.schedule.repository.TripRepository;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleService {

    private final TripRepository tripRepository;
    private final ScheduleRepository scheduleRepository;
    private final UserRoomRepository userRoomRepository;
    private final RoomRepository roomRepository;
    private final RoutesApiService routesApiService;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 사용자가 해당 여행에 접근 권한이 있는지 검증
     */
    private void validateUserInTrip(Long tripId, Long userId) {
        // Trip 존재 확인
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("여행을 찾을 수 없습니다."));

        // UserRoom 존재 확인 (권한 검증)
        Long roomId = trip.getRoomId();
        UserRoomId userRoomId = new UserRoomId(userId, roomId);

        if (!userRoomRepository.existsById(userRoomId)) {
            throw new IllegalArgumentException("해당 여행에 접근 권한이 없습니다.");
        }
    }

    /**
     * 특정 여행의 특정 날짜 일정 조회
     */
    public TripScheduleResponse getTripSchedules(Long tripId, Integer dayNumber, Long userId) {
        // 권한 검증
        validateUserInTrip(tripId, userId);

        // dayNumber가 null이면 기본값 1 사용
        Integer targetDayNumber = (dayNumber != null) ? dayNumber : 1;

        // Trip 조회
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("여행을 찾을 수 없습니다."));

        // Room 조회 (startDate, endDate 획득)
        Room room = roomRepository.findById(trip.getRoomId())
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));

        // 특정 날짜의 일정 조회
        List<Schedule> schedules = scheduleRepository.findByTripIdAndDayNumber(tripId, targetDayNumber);

        // Schedule 리스트를 ScheduleItemResponse로 매핑 (visitOrder 순으로 정렬)
        List<ScheduleItemResponse> scheduleItems = schedules.stream()
                .sorted(Comparator.comparing(Schedule::getVisitOrder))
                .map(this::mapToScheduleItemResponse)
                .collect(Collectors.toList());

        // TravelMode 설정 (null이면 기본값 DRIVE)
        TravelMode travelMode = trip.getTravelMode() != null ? trip.getTravelMode() : TravelMode.DRIVE;

        // TripScheduleResponse 생성 및 반환
        return TripScheduleResponse.builder()
                .tripId(trip.getTripId())
                .destination(trip.getDestination())
                .startDate(room.getStartDate())
                .endDate(room.getEndDate())
                .currentDay(targetDayNumber)
                .schedules(scheduleItems)
                .travelMode(travelMode)
                .budget(trip.getBudget())
                .build();
    }

    /**
     * 특정 여행의 특정 날짜 일정 조회 (위경도 포함)
     */
    public TripScheduleWithLocationResponse getTripSchedulesWithLocation(Long tripId, Integer dayNumber, Long userId) {
        // 권한 검증
        validateUserInTrip(tripId, userId);

        // dayNumber가 null이면 기본값 1 사용
        Integer targetDayNumber = (dayNumber != null) ? dayNumber : 1;

        // Trip 조회
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("여행을 찾을 수 없습니다."));

        // Room 조회 (startDate, endDate 획득)
        Room room = roomRepository.findById(trip.getRoomId())
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));

        // 특정 날짜의 일정 조회
        List<Schedule> schedules = scheduleRepository.findByTripIdAndDayNumber(tripId, targetDayNumber);

        // Schedule 리스트를 ScheduleItemWithLocationResponse로 매핑 (visitOrder 순으로 정렬)
        List<ScheduleItemWithLocationResponse> scheduleItems = schedules.stream()
                .sorted(Comparator.comparing(Schedule::getVisitOrder))
                .map(this::mapToScheduleItemWithLocationResponse)
                .collect(Collectors.toList());

        // TravelMode 설정 (null이면 기본값 DRIVE)
        TravelMode travelMode = trip.getTravelMode() != null ? trip.getTravelMode() : TravelMode.DRIVE;

        // TripScheduleWithLocationResponse 생성 및 반환
        return TripScheduleWithLocationResponse.builder()
                .tripId(trip.getTripId())
                .destination(trip.getDestination())
                .startDate(room.getStartDate())
                .endDate(room.getEndDate())
                .currentDay(targetDayNumber)
                .schedules(scheduleItems)
                .travelMode(travelMode)
                .budget(trip.getBudget())
                .build();
    }

    /**
     * 특정 여행의 특정 날짜 일정 조회 (공개 - 권한 검증 없음)
     * 커뮤니티 게시글에서 공유된 일정을 조회할 때 사용
     */
    public TripScheduleResponse getTripSchedulesPublic(Long tripId, Integer dayNumber) {
        // dayNumber가 null이면 기본값 1 사용
        Integer targetDayNumber = (dayNumber != null) ? dayNumber : 1;

        // Trip 조회
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("여행을 찾을 수 없습니다."));

        // Room 조회 (startDate, endDate 획득)
        Room room = roomRepository.findById(trip.getRoomId())
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));

        // 특정 날짜의 일정 조회
        List<Schedule> schedules = scheduleRepository.findByTripIdAndDayNumber(tripId, targetDayNumber);

        // Schedule 리스트를 ScheduleItemResponse로 매핑 (visitOrder 순으로 정렬)
        List<ScheduleItemResponse> scheduleItems = schedules.stream()
                .sorted(Comparator.comparing(Schedule::getVisitOrder))
                .map(this::mapToScheduleItemResponse)
                .collect(Collectors.toList());

        // TravelMode 설정 (null이면 기본값 DRIVE)
        TravelMode travelMode = trip.getTravelMode() != null ? trip.getTravelMode() : TravelMode.DRIVE;

        // TripScheduleResponse 생성 및 반환
        return TripScheduleResponse.builder()
                .tripId(trip.getTripId())
                .destination(trip.getDestination())
                .startDate(room.getStartDate())
                .endDate(room.getEndDate())
                .currentDay(targetDayNumber)
                .schedules(scheduleItems)
                .travelMode(travelMode)
                .budget(trip.getBudget())
                .build();
    }

    /**
     * 일정의 방문 완료 상태 변경
     */
    @Transactional
    public VisitStatusUpdateResponse updateVisitStatus(Long tripId, Long scheduleId, VisitStatusUpdateRequest request, Long userId) {
        // 권한 검증
        validateUserInTrip(tripId, userId);

        // Schedule 조회
        Schedule schedule = scheduleRepository.findByScheduleIdAndTripId(scheduleId, tripId)
                .orElseThrow(() -> new IllegalArgumentException("일정을 찾을 수 없습니다."));

        // 방문 상태 업데이트
        schedule.setIsVisit(request.getIsVisit());

        // JPA dirty checking으로 자동 업데이트

        // 응답 DTO 생성 및 반환
        return VisitStatusUpdateResponse.builder()
                .scheduleId(schedule.getScheduleId())
                .isVisit(schedule.getIsVisit())
                .build();
    }

    /**
     * 일정의 비용 정보 조회
     */
    public ScheduleCostResponse getScheduleCost(Long tripId, Long scheduleId, Long userId) {
        // 권한 검증
        validateUserInTrip(tripId, userId);

        // Schedule 조회
        Schedule schedule = scheduleRepository.findByScheduleIdAndTripId(scheduleId, tripId)
                .orElseThrow(() -> new IllegalArgumentException("일정을 찾을 수 없습니다."));

        // 응답 DTO 생성 및 반환
        return ScheduleCostResponse.builder()
                .scheduleId(schedule.getScheduleId())
                .estimatedCost(schedule.getEstimatedCost())
                .costExplanation(schedule.getCostExplanation())
                .build();
    }

    /**
     * 여행의 모든 일정 비용 정보 조회 (숙소 제외)
     */
    public List<ScheduleCostResponse> getAllScheduleCosts(Long tripId, Long userId) {
        // 권한 검증
        validateUserInTrip(tripId, userId);

        // 숙소를 제외한 모든 일정 조회
        List<Schedule> schedules = scheduleRepository.findByTripIdExcludingHome(tripId);

        // Schedule 리스트를 ScheduleCostResponse로 매핑
        return schedules.stream()
                .map(schedule -> ScheduleCostResponse.builder()
                        .scheduleId(schedule.getScheduleId())
                        .estimatedCost(schedule.getEstimatedCost())
                        .costExplanation(schedule.getCostExplanation())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 여행의 숙박 비용 정보 조회
     */
    public AccommodationCostResponse getAccommodationCost(Long tripId, Long userId) {
        // 권한 검증
        validateUserInTrip(tripId, userId);

        // Trip 조회
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("여행을 찾을 수 없습니다."));

        // 응답 DTO 생성 및 반환
        return AccommodationCostResponse.builder()
                .accommodationCostInfo(trip.getAccommodationCostInfo())
                .build();
    }

    /**
     * 일정 방문 순서 변경
     */
    @Transactional
    public TripScheduleResponse reorderSchedule(Long tripId, Long scheduleId, ReorderScheduleRequest request, Long userId) {
        // 권한 검증
        validateUserInTrip(tripId, userId);

        // Schedule 조회 및 dayNumber 확인
        Schedule targetSchedule = scheduleRepository.findByScheduleIdAndTripId(scheduleId, tripId)
                .orElseThrow(() -> new IllegalArgumentException("일정을 찾을 수 없습니다."));

        Integer dayNumber = targetSchedule.getDayNumber();
        Integer currentOrder = targetSchedule.getVisitOrder();
        Integer newOrder = request.getNewVisitOrder();

        // 같은 순서로 변경하려는 경우 아무 작업 없이 반환
        if (currentOrder.equals(newOrder)) {
            return getTripSchedules(tripId, dayNumber, userId);
        }

        // 해당 날짜의 모든 일정 조회 (visitOrder 순으로 정렬)
        List<Schedule> daySchedules = scheduleRepository.findByTripIdAndDayNumber(tripId, dayNumber)
                .stream()
                .sorted(Comparator.comparing(Schedule::getVisitOrder))
                .collect(Collectors.toList());

        // 새로운 순서가 유효한지 검증
        if (newOrder < 1 || newOrder > daySchedules.size()) {
            throw new IllegalArgumentException("유효하지 않은 방문 순서입니다. (1-" + daySchedules.size() + " 사이여야 합니다)");
        }

        // 순서 변경 로직
        // 1. 리스트에서 대상 일정 제거
        daySchedules.removeIf(s -> s.getScheduleId().equals(scheduleId));

        // 2. 새로운 위치에 삽입 (newOrder는 1-based이므로 index는 newOrder-1)
        daySchedules.add(newOrder - 1, targetSchedule);

        // 3. 모든 일정의 visitOrder 재정렬
        for (int i = 0; i < daySchedules.size(); i++) {
            daySchedules.get(i).setVisitOrder(i + 1);
        }

        // 4. 이동시간 재계산
        recalculateDayTravelTimes(tripId, dayNumber);

        // 5. 출발/도착 시간 재계산
        recalculateDepartureTimes(tripId, dayNumber);

        // 6. 업데이트된 일정 반환
        return getTripSchedules(tripId, dayNumber, userId);
    }

    /**
     * 일정의 체류시간 수정
     */
    @Transactional
    public ScheduleItemResponse updateStayDuration(Long tripId, Long scheduleId, UpdateStayDurationRequest request, Long userId) {
        // 권한 검증
        validateUserInTrip(tripId, userId);

        // Schedule 조회
        Schedule schedule = scheduleRepository.findByScheduleIdAndTripId(scheduleId, tripId)
                .orElseThrow(() -> new IllegalArgumentException("일정을 찾을 수 없습니다."));

        // 새로운 departure 시간 계산: arrival + stayMinutes
        LocalDateTime arrival = schedule.getArrival();
        LocalDateTime newDeparture = arrival.plusMinutes(request.getStayMinutes());

        // departure 시간 업데이트
        schedule.setDeparture(newDeparture);

        // 이후 일정들의 시간 연쇄 수정
        Integer dayNumber = schedule.getDayNumber();
        recalculateDepartureTimes(tripId, dayNumber);

        // JPA dirty checking으로 자동 업데이트

        // 응답 DTO 생성 및 반환
        return mapToScheduleItemResponse(schedule);
    }

    /**
     * 일정의 방문(arrival) 시간 수정
     */
    @Transactional
    public ScheduleItemResponse updateVisitTime(Long tripId, Long scheduleId, UpdateVisitTimeRequest request, Long userId) {
        // 권한 검증
        validateUserInTrip(tripId, userId);

        // Schedule 조회
        Schedule schedule = scheduleRepository.findByScheduleIdAndTripId(scheduleId, tripId)
                .orElseThrow(() -> new IllegalArgumentException("일정을 찾을 수 없습니다."));

        // 기존 체류시간 계산 (분 단위)
        LocalDateTime currentArrival = schedule.getArrival();
        LocalDateTime currentDeparture = schedule.getDeparture();
        long stayMinutes = Duration.between(currentArrival, currentDeparture).toMinutes();

        // 새로운 arrival 시간 설정: 현재 날짜 + 요청받은 시간
        LocalDateTime newArrival = schedule.getDate().atTime(request.getNewArrivalTime());

        // 새로운 departure 시간 계산: 새 arrival + 기존 체류시간
        LocalDateTime newDeparture = newArrival.plusMinutes(stayMinutes);

        // 시간 업데이트
        schedule.setArrival(newArrival);
        schedule.setDeparture(newDeparture);

        // 이후 일정들의 시간 연쇄 수정
        Integer dayNumber = schedule.getDayNumber();
        recalculateDepartureTimes(tripId, dayNumber);

        // JPA dirty checking으로 자동 업데이트

        // 응답 DTO 생성 및 반환
        return mapToScheduleItemResponse(schedule);
    }

    /**
     * 특정 날짜의 마지막 일정으로 새로운 장소 추가 (숙소 전)
     */
    @Transactional
    public ScheduleItemResponse addScheduleToDay(Long tripId, AddScheduleRequest request, Long userId) {
        // 권한 검증
        validateUserInTrip(tripId, userId);

        // Trip 조회
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("여행을 찾을 수 없습니다."));

        // Room 조회 (날짜 계산용)
        Room room = roomRepository.findById(trip.getRoomId())
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));

        // 날짜 계산: startDate + (dayNumber - 1)
        LocalDate scheduleDate = room.getStartDate().plusDays(request.getDayNumber() - 1);

        // 해당 날짜의 기존 일정 조회 (visitOrder 순으로 정렬)
        List<Schedule> daySchedules = scheduleRepository.findByTripIdAndDayNumber(tripId, request.getDayNumber())
                .stream()
                .sorted(Comparator.comparing(Schedule::getVisitOrder))
                .collect(Collectors.toList());

        // 마지막 visitOrder 확인 (숙소 제외)
        Integer newVisitOrder;
        Schedule lastNonAccommodationSchedule = null;

        if (daySchedules.isEmpty()) {
            // 해당 날짜에 일정이 없으면 첫 번째 일정
            newVisitOrder = 1;
        } else {
            // 숙소(HOME)가 아닌 마지막 일정 찾기
            for (int i = daySchedules.size() - 1; i >= 0; i--) {
                if (daySchedules.get(i).getPlaceTag() != PlaceTag.HOME) {
                    lastNonAccommodationSchedule = daySchedules.get(i);
                    break;
                }
            }

            if (lastNonAccommodationSchedule != null) {
                newVisitOrder = lastNonAccommodationSchedule.getVisitOrder() + 1;
            } else {
                // 모든 일정이 숙소인 경우 (드문 케이스)
                newVisitOrder = daySchedules.size() + 1;
            }
        }

        // 이전 일정과의 travelTime 계산 및 arrival/departure 시간 설정
        LocalDateTime newArrival;
        LocalDateTime newDeparture;

        if (lastNonAccommodationSchedule != null) {
            // 이전 일정이 있는 경우: 이전 departure + travelTime
            TravelMode travelMode = trip.getTravelMode() != null ? trip.getTravelMode() : TravelMode.DRIVE;

            // Routes API로 이동시간 계산 (분 단위)
            Integer travelMinutes = routesApiService.calculateTravelTime(
                    lastNonAccommodationSchedule.getLatitude(),
                    lastNonAccommodationSchedule.getLongitude(),
                    request.getLatitude(),
                    request.getLongitude(),
                    travelMode
            );

            // 이전 일정의 travelTime 업데이트
            String travelTimeText = routesApiService.formatMinutesToReadable(travelMinutes);
            lastNonAccommodationSchedule.setTravelTime(travelTimeText);

            // 새 일정의 arrival = 이전 departure + travelTime
            newArrival = lastNonAccommodationSchedule.getDeparture().plusMinutes(travelMinutes);
            newDeparture = newArrival.plusMinutes(request.getStayMinutes());
        } else {
            // 첫 번째 일정인 경우: 기본 시작 시간 설정 (오전 9시)
            newArrival = scheduleDate.atTime(9, 0);
            newDeparture = newArrival.plusMinutes(request.getStayMinutes());
        }

        // 새로운 Schedule 엔티티 생성
        Schedule newSchedule = Schedule.builder()
                .tripId(tripId)
                .dayNumber(request.getDayNumber())
                .date(scheduleDate)
                .visitOrder(newVisitOrder)
                .placeName(request.getPlaceName())
                .placeTag(request.getPlaceTag())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .isVisit(false)
                .arrival(newArrival)
                .departure(newDeparture)
                .travelTime(null) // 초기에는 null, 이후 계산
                .build();

        // 새 일정 저장
        Schedule savedSchedule = scheduleRepository.save(newSchedule);

        // 새 일정의 visitOrder보다 크거나 같은 기존 일정들의 visitOrder를 +1 증가
        for (Schedule schedule : daySchedules) {
            if (schedule.getVisitOrder() >= newVisitOrder) {
                schedule.setVisitOrder(schedule.getVisitOrder() + 1);
            }
        }

        // 숙소가 있다면 새 일정과 숙소 간 travelTime 계산
        Schedule accommodationSchedule = null;
        for (Schedule schedule : daySchedules) {
            if (schedule.getPlaceTag() == PlaceTag.HOME && schedule.getVisitOrder() > newVisitOrder) {
                accommodationSchedule = schedule;
                break;
            }
        }

        if (accommodationSchedule != null) {
            TravelMode travelMode = trip.getTravelMode() != null ? trip.getTravelMode() : TravelMode.DRIVE;

            // 새 일정에서 숙소까지 이동시간 계산
            Integer travelMinutes = routesApiService.calculateTravelTime(
                    savedSchedule.getLatitude(),
                    savedSchedule.getLongitude(),
                    accommodationSchedule.getLatitude(),
                    accommodationSchedule.getLongitude(),
                    travelMode
            );

            String travelTimeText = routesApiService.formatMinutesToReadable(travelMinutes);
            savedSchedule.setTravelTime(travelTimeText);

            // 숙소의 arrival 시간 업데이트
            LocalDateTime accommodationArrival = savedSchedule.getDeparture().plusMinutes(travelMinutes);
            accommodationSchedule.setArrival(accommodationArrival);

            // 숙소는 일반적으로 체크인 후 다음날까지 머물므로 departure는 다음날 아침
            // 기존 체류시간 유지
            long stayMinutes = Duration.between(accommodationSchedule.getArrival(), accommodationSchedule.getDeparture()).toMinutes();
            accommodationSchedule.setDeparture(accommodationArrival.plusMinutes(stayMinutes));
        }

        // 응답 DTO 생성 및 반환
        return mapToScheduleItemResponse(savedSchedule);
    }

    /**
     * 일정 삭제
     */
    @Transactional
    public DeleteScheduleResponse deleteSchedule(Long tripId, Long scheduleId, Long userId) {
        // 권한 검증
        validateUserInTrip(tripId, userId);

        // Schedule 조회
        Schedule schedule = scheduleRepository.findByScheduleIdAndTripId(scheduleId, tripId)
                .orElseThrow(() -> new IllegalArgumentException("일정을 찾을 수 없습니다."));

        // dayNumber, visitOrder 저장
        Integer dayNumber = schedule.getDayNumber();
        Integer deletedVisitOrder = schedule.getVisitOrder();

        // Schedule 삭제
        scheduleRepository.delete(schedule);

        // 해당 날짜의 모든 일정 조회 (삭제 후)
        List<Schedule> daySchedules = scheduleRepository.findByTripIdAndDayNumber(tripId, dayNumber)
                .stream()
                .sorted(Comparator.comparing(Schedule::getVisitOrder))
                .collect(Collectors.toList());

        // 삭제된 visitOrder보다 큰 일정들의 visitOrder를 -1씩 감소
        for (Schedule s : daySchedules) {
            if (s.getVisitOrder() > deletedVisitOrder) {
                s.setVisitOrder(s.getVisitOrder() - 1);
            }
        }

        // 이동시간 재계산
        recalculateDayTravelTimes(tripId, dayNumber);

        // 출발/도착 시간 재계산
        recalculateDepartureTimes(tripId, dayNumber);

        // JPA dirty checking으로 자동 업데이트

        // 응답 DTO 생성 및 반환
        return DeleteScheduleResponse.builder()
                .deletedScheduleId(scheduleId)
                .build();
    }

    /**
     * 특정 날짜(dayNumber)의 숙소를 변경합니다 (레거시 메서드).
     *
     * <p>해당 날짜에서 PlaceTag.HOME인 일정을 찾아 위치 정보를 업데이트하고,
     * 이전/다음 일정과의 이동시간을 재계산합니다.
     *
     * <p><strong>새로운 방식:</strong> scheduleId 기반 숙소 변경은
     * {@link #updateAccommodationByScheduleId(Long, String, Double, Double)} 메서드를 사용하세요.
     * 일괄 수정 API를 통해 다른 변경사항과 함께 적용할 수 있습니다.
     *
     * @param tripId 여행 ID
     * @param request 숙소 변경 요청 (dayNumber, placeName, latitude, longitude)
     * @param userId 사용자 ID
     * @return 변경된 숙소 정보
     * @throws IllegalArgumentException 숙소를 찾을 수 없는 경우
     * @see #updateAccommodationByScheduleId(Long, String, Double, Double)
     * @see #batchUpdateSchedule(Long, BatchUpdateScheduleRequest, Long)
     */
    @Transactional
    public ScheduleItemResponse updateAccommodation(Long tripId, UpdateAccommodationRequest request, Long userId) {
        // 권한 검증
        validateUserInTrip(tripId, userId);

        // 숙소 Schedule 조회
        Schedule accommodation = findAccommodationSchedule(tripId, request.getDayNumber());
        if (accommodation == null) {
            throw new IllegalArgumentException("해당 날짜에 숙소를 찾을 수 없습니다.");
        }

        // 기존 체류시간 저장 (arrival과 departure 간격)
        Duration existingStayDuration = Duration.between(accommodation.getArrival(), accommodation.getDeparture());

        // 숙소 정보 업데이트
        accommodation.setPlaceName(request.getPlaceName());
        accommodation.setLatitude(request.getLatitude());
        accommodation.setLongitude(request.getLongitude());

        // Trip의 travelMode 조회
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("여행을 찾을 수 없습니다."));
        TravelMode travelMode = trip.getTravelMode() != null ? trip.getTravelMode() : TravelMode.DRIVE;

        // 해당 날짜의 모든 일정 조회 (visitOrder 순)
        List<Schedule> daySchedules = scheduleRepository.findByTripIdAndDayNumber(tripId, request.getDayNumber())
                .stream()
                .sorted(Comparator.comparing(Schedule::getVisitOrder))
                .collect(Collectors.toList());

        // 숙소로 도착하는 이동시간 재계산 (이전 일정 → 숙소)
        Schedule previousSchedule = null;
        for (int i = 0; i < daySchedules.size(); i++) {
            if (daySchedules.get(i).getScheduleId().equals(accommodation.getScheduleId())) {
                if (i > 0) {
                    previousSchedule = daySchedules.get(i - 1);
                }
                break;
            }
        }

        if (previousSchedule != null && previousSchedule.getPlaceTag() != PlaceTag.HOME) {
            // 이전 일정에서 숙소까지의 이동시간 계산
            Integer travelTimeMinutes = routesApiService.calculateTravelTime(
                    previousSchedule.getLatitude(),
                    previousSchedule.getLongitude(),
                    accommodation.getLatitude(),
                    accommodation.getLongitude(),
                    travelMode
            );
            String travelTimeText = routesApiService.formatMinutesToReadable(travelTimeMinutes);
            previousSchedule.setTravelTime(travelTimeText);

            // 숙소 arrival 시간 재계산
            LocalDateTime newArrival = previousSchedule.getDeparture().plusMinutes(travelTimeMinutes);
            accommodation.setArrival(newArrival);
            accommodation.setDeparture(newArrival.plus(existingStayDuration));
        }

        // 다음날 첫 일정 확인 및 이동시간 재계산 (숙소 → 다음날 첫 일정)
        List<Schedule> nextDaySchedules = scheduleRepository.findByTripIdAndDayNumber(tripId, request.getDayNumber() + 1)
                .stream()
                .sorted(Comparator.comparing(Schedule::getVisitOrder))
                .collect(Collectors.toList());

        if (!nextDaySchedules.isEmpty()) {
            Schedule firstScheduleNextDay = nextDaySchedules.get(0);

            // 숙소에서 다음날 첫 일정까지의 이동시간 계산
            Integer travelTimeMinutes = routesApiService.calculateTravelTime(
                    accommodation.getLatitude(),
                    accommodation.getLongitude(),
                    firstScheduleNextDay.getLatitude(),
                    firstScheduleNextDay.getLongitude(),
                    travelMode
            );
            String travelTimeText = routesApiService.formatMinutesToReadable(travelTimeMinutes);
            accommodation.setTravelTime(travelTimeText);

            // 다음날 첫 일정의 arrival 시간 재계산
            LocalDateTime nextDayArrival = accommodation.getDeparture().plusMinutes(travelTimeMinutes);
            firstScheduleNextDay.setArrival(nextDayArrival);

            // 다음날 전체 일정의 departure 시간 재계산
            recalculateDepartureTimes(tripId, request.getDayNumber() + 1);
        }

        // 현재 날짜의 departure 시간 재계산
        recalculateDepartureTimes(tripId, request.getDayNumber());

        // JPA dirty checking으로 자동 업데이트

        // 응답 DTO 생성 및 반환
        return mapToScheduleItemResponse(accommodation);
    }

    /**
     * scheduleId로 기존 숙소를 새로운 위치의 숙소로 변경
     *
     * <p>일괄 수정 및 미리보기 API에서 사용되는 내부 메서드입니다.
     * PlaceTag.HOME인 일정만 변경할 수 있으며, 일반 일정을 숙소로 변경할 수 없습니다.
     *
     * @param scheduleId 변경할 숙소 일정의 ID
     * @param placeName 새로운 숙소 이름
     * @param latitude 새로운 숙소 위도
     * @param longitude 새로운 숙소 경도
     * @throws IllegalArgumentException 해당 scheduleId가 존재하지 않거나 PlaceTag.HOME이 아닌 경우
     */
    private void updateAccommodationByScheduleId(
            Long scheduleId,
            String placeName,
            Double latitude,
            Double longitude
    ) {
        // Schedule 조회 및 PlaceTag.HOME 검증
        Schedule accommodation = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("해당 일정을 찾을 수 없습니다."));

        if (accommodation.getPlaceTag() != PlaceTag.HOME) {
            throw new IllegalArgumentException("숙소(PlaceTag.HOME)만 변경할 수 있습니다.");
        }

        // 기존 체류시간 저장 (arrival과 departure 간격)
        Duration existingStayDuration = Duration.between(accommodation.getArrival(), accommodation.getDeparture());

        // 숙소 정보 업데이트
        accommodation.setPlaceName(placeName);
        accommodation.setLatitude(latitude);
        accommodation.setLongitude(longitude);
        // PlaceTag는 이미 HOME이므로 변경 불필요

        // Trip의 travelMode 조회
        Long tripId = accommodation.getTrip().getTripId();
        Integer dayNumber = accommodation.getDayNumber();
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("여행을 찾을 수 없습니다."));
        TravelMode travelMode = trip.getTravelMode() != null ? trip.getTravelMode() : TravelMode.DRIVE;

        // 해당 날짜의 모든 일정 조회 (visitOrder 순)
        List<Schedule> daySchedules = scheduleRepository.findByTripIdAndDayNumber(tripId, dayNumber)
                .stream()
                .sorted(Comparator.comparing(Schedule::getVisitOrder))
                .collect(Collectors.toList());

        // 이전 일정 → 숙소 이동시간 재계산
        Schedule previousSchedule = null;
        for (int i = 0; i < daySchedules.size(); i++) {
            if (daySchedules.get(i).getScheduleId().equals(accommodation.getScheduleId())) {
                if (i > 0) {
                    previousSchedule = daySchedules.get(i - 1);
                }
                break;
            }
        }

        if (previousSchedule != null && previousSchedule.getPlaceTag() != PlaceTag.HOME) {
            // 이전 일정에서 숙소까지의 이동시간 계산
            Integer travelTimeMinutes = routesApiService.calculateTravelTime(
                    previousSchedule.getLatitude(),
                    previousSchedule.getLongitude(),
                    accommodation.getLatitude(),
                    accommodation.getLongitude(),
                    travelMode
            );
            String travelTimeText = routesApiService.formatMinutesToReadable(travelTimeMinutes);
            previousSchedule.setTravelTime(travelTimeText);

            // 숙소 arrival 시간 재계산
            LocalDateTime newArrival = previousSchedule.getDeparture().plusMinutes(travelTimeMinutes);
            accommodation.setArrival(newArrival);
            accommodation.setDeparture(newArrival.plus(existingStayDuration));
        }

        // 숙소 → 다음 일정 이동시간 재계산
        // 다음 일정은 같은 날짜의 다음 일정 또는 다음날 첫 일정일 수 있음
        Schedule nextSchedule = null;

        // 같은 날짜의 다음 일정 찾기
        for (int i = 0; i < daySchedules.size(); i++) {
            if (daySchedules.get(i).getScheduleId().equals(accommodation.getScheduleId())) {
                if (i < daySchedules.size() - 1) {
                    nextSchedule = daySchedules.get(i + 1);
                }
                break;
            }
        }

        // 같은 날짜에 다음 일정이 없으면 다음날 첫 일정 조회
        if (nextSchedule == null) {
            List<Schedule> nextDaySchedules = scheduleRepository.findByTripIdAndDayNumber(tripId, dayNumber + 1)
                    .stream()
                    .sorted(Comparator.comparing(Schedule::getVisitOrder))
                    .collect(Collectors.toList());

            if (!nextDaySchedules.isEmpty()) {
                nextSchedule = nextDaySchedules.get(0);
            }
        }

        if (nextSchedule != null) {
            // 숙소에서 다음 일정까지의 이동시간 계산
            Integer travelTimeMinutes = routesApiService.calculateTravelTime(
                    accommodation.getLatitude(),
                    accommodation.getLongitude(),
                    nextSchedule.getLatitude(),
                    nextSchedule.getLongitude(),
                    travelMode
            );
            String travelTimeText = routesApiService.formatMinutesToReadable(travelTimeMinutes);
            accommodation.setTravelTime(travelTimeText);

            // 다음 일정의 arrival 시간 재계산
            LocalDateTime nextArrival = accommodation.getDeparture().plusMinutes(travelTimeMinutes);
            nextSchedule.setArrival(nextArrival);

            // 다음 일정이 다음날이면 다음날 전체 departure 재계산
            if (!nextSchedule.getDayNumber().equals(dayNumber)) {
                recalculateDepartureTimes(tripId, nextSchedule.getDayNumber());
            }
        }

        // 현재 날짜의 departure 시간 재계산
        recalculateDepartureTimes(tripId, dayNumber);

        // JPA dirty checking으로 자동 업데이트
    }

    /**
     * 특정 날짜의 일정들 간 이동시간 재계산
     */
    private void recalculateDayTravelTimes(Long tripId, Integer dayNumber) {
        // Trip 조회 및 travelMode 확인
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("여행을 찾을 수 없습니다."));

        TravelMode travelMode = trip.getTravelMode();
        // travelMode가 null이면 기본값 DRIVE 사용
        if (travelMode == null) {
            travelMode = TravelMode.DRIVE;
        }

        // 해당 날짜의 모든 일정을 visitOrder 순으로 조회
        List<Schedule> daySchedules = scheduleRepository.findByTripIdAndDayNumber(tripId, dayNumber)
                .stream()
                .sorted(Comparator.comparing(Schedule::getVisitOrder))
                .collect(Collectors.toList());

        // 각 일정의 다음 일정까지 이동시간 계산
        for (int i = 0; i < daySchedules.size(); i++) {
            Schedule currentSchedule = daySchedules.get(i);

            // 다음 일정이 있으면 이동시간 계산
            if (i < daySchedules.size() - 1) {
                Schedule nextSchedule = daySchedules.get(i + 1);

                // Routes API로 이동시간 계산 (분 단위)
                Integer travelMinutes = routesApiService.calculateTravelTime(
                        currentSchedule.getLatitude(),
                        currentSchedule.getLongitude(),
                        nextSchedule.getLatitude(),
                        nextSchedule.getLongitude(),
                        travelMode
                );

                // 한국어 형식으로 변환 ("30분", "1시간 30분" 등)
                String travelTimeText = routesApiService.formatMinutesToReadable(travelMinutes);
                currentSchedule.setTravelTime(travelTimeText);
            } else {
                // 마지막 일정은 travelTime null
                currentSchedule.setTravelTime(null);
            }
        }

        // JPA dirty checking으로 자동 업데이트
    }

    /**
     * 특정 날짜의 일정들의 departure 시간 재계산
     * 순서 변경 후 연쇄적으로 시간을 재계산합니다.
     */
    private void recalculateDepartureTimes(Long tripId, Integer dayNumber) {
        // 해당 날짜의 모든 일정을 visitOrder 순으로 조회
        List<Schedule> daySchedules = scheduleRepository.findByTripIdAndDayNumber(tripId, dayNumber)
                .stream()
                .sorted(Comparator.comparing(Schedule::getVisitOrder))
                .collect(Collectors.toList());

        // 각 일정에 대해 arrival과 departure 재계산
        for (int i = 0; i < daySchedules.size(); i++) {
            Schedule currentSchedule = daySchedules.get(i);

            if (i == 0) {
                // 첫 번째 일정: arrival은 유지, departure만 재계산
                LocalDateTime arrival = currentSchedule.getArrival();
                LocalDateTime originalDeparture = currentSchedule.getDeparture();

                // 체류시간 = 기존 (departure - arrival)
                long stayMinutes = Duration.between(arrival, originalDeparture).toMinutes();

                // departure = arrival + 체류시간
                LocalDateTime newDeparture = arrival.plusMinutes(stayMinutes);
                currentSchedule.setDeparture(newDeparture);
            } else {
                // 나머지 일정: 이전 일정의 departure + travelTime = 현재 arrival
                Schedule previousSchedule = daySchedules.get(i - 1);
                LocalDateTime previousDeparture = previousSchedule.getDeparture();
                String travelTimeText = previousSchedule.getTravelTime();

                // travelTime을 분 단위로 파싱
                Integer travelMinutes = parseTravelTimeToMinutes(travelTimeText);

                // 현재 일정의 arrival = 이전 departure + travelTime
                LocalDateTime newArrival = previousDeparture.plusMinutes(travelMinutes);

                // 체류시간 = 기존 (departure - arrival)
                LocalDateTime originalArrival = currentSchedule.getArrival();
                LocalDateTime originalDeparture = currentSchedule.getDeparture();
                long stayMinutes = Duration.between(originalArrival, originalDeparture).toMinutes();

                // 새로운 departure = 새로운 arrival + 체류시간
                LocalDateTime newDeparture = newArrival.plusMinutes(stayMinutes);

                currentSchedule.setArrival(newArrival);
                currentSchedule.setDeparture(newDeparture);
            }
        }

        // JPA dirty checking으로 자동 업데이트
    }

    /**
     * travelTime 문자열을 분 단위로 파싱
     * 예: "30분" -> 30, "1시간 30분" -> 90, "2시간" -> 120, "30" -> 30 (백워드 호환)
     */
    private Integer parseTravelTimeToMinutes(String travelTimeText) {
        if (travelTimeText == null || travelTimeText.isEmpty()) {
            return 0;
        }

        try {
            int totalMinutes = 0;

            // "시간" 파싱
            if (travelTimeText.contains("시간")) {
                String[] parts = travelTimeText.split("시간");
                String hourPart = parts[0].trim();
                totalMinutes += Integer.parseInt(hourPart) * 60;

                // "분" 파싱 (시간 뒤에 분이 있는 경우)
                if (parts.length > 1 && parts[1].contains("분")) {
                    String minutePart = parts[1].replace("분", "").trim();
                    if (!minutePart.isEmpty()) {
                        totalMinutes += Integer.parseInt(minutePart);
                    }
                }
            } else if (travelTimeText.contains("분")) {
                // "분"만 있는 경우
                String minutePart = travelTimeText.replace("분", "").trim();
                totalMinutes = Integer.parseInt(minutePart);
            } else {
                // 숫자만 있는 경우 (백워드 호환성을 위해 분 단위로 간주)
                totalMinutes = Integer.parseInt(travelTimeText.trim());
            }

            return totalMinutes;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 분 단위 숫자를 한국어 형식 문자열로 변환
     * 예: 30 -> "30분", 90 -> "1시간 30분", 120 -> "2시간"
     */
    private String convertMinutesToKoreanFormat(Integer minutes) {
        if (minutes == null || minutes == 0) {
            return "0분";
        }

        if (minutes < 60) {
            return minutes + "분";
        }

        int hours = minutes / 60;
        int remainingMinutes = minutes % 60;

        if (remainingMinutes == 0) {
            return hours + "시간";
        }

        return hours + "시간 " + remainingMinutes + "분";
    }

    /**
     * 특정 날짜의 숙소 일정 조회
     * @param tripId 여행 ID
     * @param dayNumber 숙소가 위치한 날짜 (일차)
     * @return 숙소 Schedule 엔티티, 없으면 null
     */
    private Schedule findAccommodationSchedule(Long tripId, Integer dayNumber) {
        // 해당 날짜의 모든 일정 조회
        List<Schedule> schedules = scheduleRepository.findByTripIdAndDayNumber(tripId, dayNumber);

        // PlaceTag가 HOME인 일정 찾기
        return schedules.stream()
                .filter(schedule -> schedule.getPlaceTag() == PlaceTag.HOME)
                .findFirst()
                .orElse(null);
    }

    /**
     * Schedule 엔티티를 ScheduleItemResponse로 매핑
     */
    private ScheduleItemResponse mapToScheduleItemResponse(Schedule schedule) {
        return ScheduleItemResponse.builder()
                .scheduleId(schedule.getScheduleId())
                .displayName(schedule.getPlaceName())
                .arrival(schedule.getArrival())
                .departure(schedule.getDeparture())
                .placeTag(schedule.getPlaceTag())
                .travelTime(parseTravelTimeToMinutes(schedule.getTravelTime()))
                .visitOrder(schedule.getVisitOrder())
                .isVisit(schedule.getIsVisit())
                .build();
    }

    /**
     * Schedule 엔티티를 ScheduleItemWithLocationResponse로 매핑 (위경도 포함)
     */
    private ScheduleItemWithLocationResponse mapToScheduleItemWithLocationResponse(Schedule schedule) {
        return ScheduleItemWithLocationResponse.builder()
                .scheduleId(schedule.getScheduleId())
                .displayName(schedule.getPlaceName())
                .arrival(schedule.getArrival())
                .departure(schedule.getDeparture())
                .placeTag(schedule.getPlaceTag())
                .travelTime(parseTravelTimeToMinutes(schedule.getTravelTime()))
                .visitOrder(schedule.getVisitOrder())
                .isVisit(schedule.getIsVisit())
                .latitude(schedule.getLatitude())
                .longitude(schedule.getLongitude())
                .build();
    }

    /**
     * 일정 변경사항 미리보기
     * 변경사항을 적용하되 DB에는 저장하지 않고 계산된 결과만 반환합니다.
     */
    @Transactional
    public TripScheduleResponse previewScheduleChanges(Long tripId, PreviewScheduleRequest request, Long userId) {
        // 권한 검증
        validateUserInTrip(tripId, userId);

        // 변경사항을 적용하되, 마지막에 롤백할 것임
        TripScheduleResponse result = applyModifications(tripId, request.getDayNumber(), request.getModifications(), userId);

        // 트랜잭션을 롤백 상태로 설정 (DB에 저장되지 않음)
        org.springframework.transaction.interceptor.TransactionAspectSupport
                .currentTransactionStatus()
                .setRollbackOnly();

        return result;
    }

    /**
     * 일정 일괄 수정
     * 변경사항을 순차적으로 적용하고 DB에 저장합니다.
     */
    @Transactional
    public TripScheduleResponse batchUpdateSchedule(Long tripId, BatchUpdateScheduleRequest request, Long userId) {
        // 권한 검증
        validateUserInTrip(tripId, userId);

        // 변경사항 적용 및 저장
        TripScheduleResponse response = applyModifications(tripId, request.getDayNumber(), request.getModifications(), userId);

        // 푸시 알림 이벤트 발행
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("여행을 찾을 수 없습니다."));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        eventPublisher.publishEvent(new ScheduleBatchUpdatedEvent(
                tripId,
                trip.getRoomId(),
                userId,
                user.getNickname(),
                request.getDayNumber()
        ));

        return response;
    }

    /**
     * 변경사항을 순차적으로 적용하는 내부 메서드
     */
    private TripScheduleResponse applyModifications(
            Long tripId,
            Integer dayNumber,
            List<ScheduleModificationItem> modifications,
            Long userId
    ) {
        // 변경사항을 타입별로 순서대로 적용
        // 순서: DELETE → ADD → REORDER → UPDATE_ACCOMMODATION → UPDATE_VISIT_TIME → UPDATE_STAY_DURATION

        // 1. DELETE 적용
        modifications.stream()
                .filter(m -> m.getModificationType() == ModificationType.DELETE)
                .forEach(m -> {
                    deleteSchedule(tripId, m.getScheduleId(), userId);
                });

        // 2. ADD 적용 (batch-update용: routes API 호출 없이 기본 일정만 생성)
        modifications.stream()
                .filter(m -> m.getModificationType() == ModificationType.ADD)
                .forEach(m -> {
                    // Trip 조회
                    Trip trip = tripRepository.findById(tripId)
                            .orElseThrow(() -> new IllegalArgumentException("여행을 찾을 수 없습니다."));

                    // Room 조회 (날짜 계산용)
                    Room room = roomRepository.findById(trip.getRoomId())
                            .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));

                    // 날짜 계산: startDate + (dayNumber - 1)
                    LocalDate scheduleDate = room.getStartDate().plusDays(m.getDayNumber() - 1);

                    // 해당 날짜의 기존 일정 조회
                    List<Schedule> daySchedules = scheduleRepository.findByTripIdAndDayNumber(tripId, m.getDayNumber())
                            .stream()
                            .sorted(Comparator.comparing(Schedule::getVisitOrder))
                            .collect(Collectors.toList());

                    // 마지막 visitOrder 확인 (숙소 제외)
                    Integer newVisitOrder;
                    Schedule lastNonAccommodationSchedule = null;

                    if (daySchedules.isEmpty()) {
                        newVisitOrder = 1;
                    } else {
                        for (int i = daySchedules.size() - 1; i >= 0; i--) {
                            if (daySchedules.get(i).getPlaceTag() != PlaceTag.HOME) {
                                lastNonAccommodationSchedule = daySchedules.get(i);
                                break;
                            }
                        }

                        if (lastNonAccommodationSchedule != null) {
                            newVisitOrder = lastNonAccommodationSchedule.getVisitOrder() + 1;
                        } else {
                            newVisitOrder = daySchedules.size() + 1;
                        }
                    }

                    // arrival/departure 시간 설정
                    LocalDateTime newArrival;
                    LocalDateTime newDeparture;

                    if (lastNonAccommodationSchedule != null) {
                        // 이전 일정의 departure를 사용 (travelTime은 UPDATE_TRAVEL_TIME으로 설정 예정)
                        newArrival = lastNonAccommodationSchedule.getDeparture();
                        newDeparture = newArrival.plusMinutes(m.getStayMinutes());
                    } else {
                        // 첫 번째 일정인 경우: 기본 시작 시간 설정 (오전 9시)
                        newArrival = scheduleDate.atTime(9, 0);
                        newDeparture = newArrival.plusMinutes(m.getStayMinutes());
                    }

                    // 새로운 Schedule 엔티티 생성
                    Schedule newSchedule = Schedule.builder()
                            .tripId(tripId)
                            .dayNumber(m.getDayNumber())
                            .date(scheduleDate)
                            .visitOrder(newVisitOrder)
                            .placeName(m.getPlaceName())
                            .placeTag(m.getPlaceTag())
                            .latitude(m.getLatitude())
                            .longitude(m.getLongitude())
                            .isVisit(false)
                            .arrival(newArrival)
                            .departure(newDeparture)
                            .travelTime(null) // routes API 호출하지 않음, UPDATE_TRAVEL_TIME으로 설정 예정
                            .build();

                    // 새 일정 저장
                    scheduleRepository.save(newSchedule);

                    // 새 일정의 visitOrder보다 크거나 같은 기존 일정들의 visitOrder를 +1 증가
                    for (Schedule schedule : daySchedules) {
                        if (schedule.getVisitOrder() >= newVisitOrder) {
                            schedule.setVisitOrder(schedule.getVisitOrder() + 1);
                        }
                    }
                });

        // 3. REORDER 적용 (batch-update용: routes API 호출 없이 순서만 변경)
        modifications.stream()
                .filter(m -> m.getModificationType() == ModificationType.REORDER)
                .forEach(m -> {
                    // Schedule 조회
                    Schedule targetSchedule = scheduleRepository.findByScheduleIdAndTripId(m.getScheduleId(), tripId)
                            .orElseThrow(() -> new IllegalArgumentException("일정을 찾을 수 없습니다."));

                    Integer currentOrder = targetSchedule.getVisitOrder();
                    Integer newOrder = m.getNewVisitOrder();

                    // 같은 순서면 스킵
                    if (currentOrder.equals(newOrder)) {
                        return;
                    }

                    // 해당 날짜의 모든 일정 조회 (visitOrder 순으로 정렬)
                    List<Schedule> daySchedules = scheduleRepository.findByTripIdAndDayNumber(tripId, dayNumber)
                            .stream()
                            .sorted(Comparator.comparing(Schedule::getVisitOrder))
                            .collect(Collectors.toList());

                    // 새로운 순서가 유효한지 검증
                    if (newOrder < 1 || newOrder > daySchedules.size()) {
                        throw new IllegalArgumentException("유효하지 않은 방문 순서입니다. (1-" + daySchedules.size() + " 사이여야 합니다)");
                    }

                    // 순서 변경 로직
                    daySchedules.removeIf(s -> s.getScheduleId().equals(m.getScheduleId()));
                    daySchedules.add(newOrder - 1, targetSchedule);

                    // 모든 일정의 visitOrder 재정렬
                    for (int i = 0; i < daySchedules.size(); i++) {
                        daySchedules.get(i).setVisitOrder(i + 1);
                    }
                    // 주의: recalculateDayTravelTimes 호출하지 않음 (routes API 호출 방지)
                });

        // 4. UPDATE_ACCOMMODATION 적용 (batch-update용: routes API 호출 없이 위치만 변경)
        modifications.stream()
                .filter(m -> m.getModificationType() == ModificationType.UPDATE_ACCOMMODATION)
                .forEach(m -> {
                    // Schedule 조회 및 PlaceTag.HOME 검증
                    Schedule accommodation = scheduleRepository.findById(m.getScheduleId())
                            .orElseThrow(() -> new IllegalArgumentException("해당 일정을 찾을 수 없습니다."));

                    if (accommodation.getPlaceTag() != PlaceTag.HOME) {
                        throw new IllegalArgumentException("숙소(PlaceTag.HOME)만 변경할 수 있습니다.");
                    }

                    // 숙소 정보 업데이트 (위치만 변경, travelTime은 UPDATE_TRAVEL_TIME으로 처리)
                    accommodation.setPlaceName(m.getPlaceName());
                    accommodation.setLatitude(m.getLatitude());
                    accommodation.setLongitude(m.getLongitude());
                    // 주의: routes API 호출하지 않음
                });

        // 5. UPDATE_VISIT_TIME 적용
        modifications.stream()
                .filter(m -> m.getModificationType() == ModificationType.UPDATE_VISIT_TIME)
                .forEach(m -> {
                    UpdateVisitTimeRequest updateTimeRequest = new UpdateVisitTimeRequest(m.getNewArrivalTime());
                    updateVisitTime(tripId, m.getScheduleId(), updateTimeRequest, userId);
                });

        // 6. UPDATE_STAY_DURATION 적용
        modifications.stream()
                .filter(m -> m.getModificationType() == ModificationType.UPDATE_STAY_DURATION)
                .forEach(m -> {
                    UpdateStayDurationRequest updateDurationRequest = new UpdateStayDurationRequest(m.getStayMinutes());
                    updateStayDuration(tripId, m.getScheduleId(), updateDurationRequest, userId);
                });

        // 7. UPDATE_TRAVEL_TIME 적용
        modifications.stream()
                .filter(m -> m.getModificationType() == ModificationType.UPDATE_TRAVEL_TIME)
                .forEach(m -> {
                    Schedule schedule = scheduleRepository.findByScheduleIdAndTripId(m.getScheduleId(), tripId)
                            .orElseThrow(() -> new IllegalArgumentException("일정을 찾을 수 없습니다."));

                    // 프론트엔드에서 전달받은 분 단위 이동시간을 한국어 형식으로 변환하여 업데이트
                    String travelTimeKorean = convertMinutesToKoreanFormat(m.getTravelTime());
                    schedule.setTravelTime(travelTimeKorean);
                });

        // 8. 모든 변경사항 적용 후 departure/arrival 시간 재계산
        recalculateDepartureTimes(tripId, dayNumber);

        // 최종 결과 조회 및 반환
        return getTripSchedules(tripId, dayNumber, userId);
    }
}
