package triB.triB.schedule.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import triB.triB.auth.entity.User;
import triB.triB.auth.entity.UserStatus;
import triB.triB.budget.entity.TripUserBudget;
import triB.triB.budget.repository.TripUserBudgetRepository;
import triB.triB.room.entity.Room;
import triB.triB.room.repository.RoomRepository;
import triB.triB.room.repository.UserRoomRepository;
import triB.triB.schedule.dto.RepresentativeTripResponse;
import triB.triB.schedule.dto.TripListResponse;
import triB.triB.schedule.dto.TripParticipantResponse;
import triB.triB.schedule.entity.Trip;
import triB.triB.schedule.entity.TripFilterType;
import triB.triB.schedule.entity.TripStatus;
import triB.triB.schedule.repository.TripRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TripService {

    private final TripRepository tripRepository;
    private final RoomRepository roomRepository;
    private final UserRoomRepository userRoomRepository;
    private final TripUserBudgetRepository tripUserBudgetRepository;

    /**
     * 로그인한 사용자의 승인된 여행 목록 조회
     * @param userId 사용자 ID
     * @return 여행 목록
     */
    public List<TripListResponse> getMyTripList(Long userId) {
        // 1. 사용자의 ACCEPTED 여행 조회
        List<Trip> trips = tripRepository.findByUserIdAndTripStatus(userId, TripStatus.ACCEPTED);

        // 2. 각 Trip을 TripListResponse로 변환
        return trips.stream()
                .map(trip -> buildTripListResponse(trip, userId))
                .collect(Collectors.toList());
    }

    /**
     * 필터링된 여행 목록 조회 (과거/미래)
     * @param userId 사용자 ID
     * @param filterType 필터 타입 (FUTURE, PAST)
     * @return 필터링된 여행 목록
     */
    public List<TripListResponse> getFilteredTripList(Long userId, TripFilterType filterType) {
        List<Trip> trips;

        switch (filterType) {
            case FUTURE:
                // 미래 여행 조회 (READY 상태, startDate 오름차순)
                trips = tripRepository.findFutureTripsByUserId(userId, TripStatus.READY);
                break;
            case PAST:
                // 과거 여행 조회 (ACCEPTED 상태, startDate 내림차순)
                trips = tripRepository.findPastTripsByUserId(userId, TripStatus.ACCEPTED);
                break;
            default:
                // 기본값: 미래 여행
                trips = tripRepository.findFutureTripsByUserId(userId, TripStatus.READY);
        }

        // 각 Trip을 TripListResponse로 변환
        return trips.stream()
                .map(trip -> buildTripListResponse(trip, userId))
                .collect(Collectors.toList());
    }

    /**
     * Trip 엔티티를 TripListResponse로 변환
     */
    private TripListResponse buildTripListResponse(Trip trip, Long userId) {
        // Room 정보 조회
        Room room = roomRepository.findById(trip.getRoomId())
                .orElseThrow(() -> new IllegalArgumentException("방을 찾을 수 없습니다."));

        // 참여자 목록 조회
        List<User> participants = userRoomRepository.findUsersByRoomId(room.getRoomId());
        List<TripParticipantResponse> participantResponses = participants.stream()
                .filter(user -> user.getUserStatus().equals(UserStatus.ACTIVE))
                .map(user -> TripParticipantResponse.builder()
                        .userId(user.getUserId())
                        .nickname(user.getNickname())
                        .photoUrl(user.getPhotoUrl())
                        .build())
                .toList();

        // 사용자 설정 예산 조회
        BigDecimal userBudget = tripUserBudgetRepository
                .findByTripIdAndUserId(trip.getTripId(), userId)
                .map(TripUserBudget::getAmount)
                .orElse(null);

        // TripListResponse 생성
        return TripListResponse.builder()
                .tripId(trip.getTripId())
                .destination(room.getDestination())
                .roomName(room.getRoomName())
                .startDate(room.getStartDate())
                .endDate(room.getEndDate())
                .participants(participantResponses)
                .budget(trip.getBudget())  // AI 추정 예산
                .userBudget(userBudget)    // 사용자 설정 예산
                .build();
    }

    /**
     * 대표 여행 ID 조회 (우선순위에 따라 하나의 여행 선택)
     * 우선순위:
     * 1. 현재 진행 중인 여행 (시작일 <= 오늘 <= 종료일) - 가장 늦게 끝나는 일정
     * 2. 가장 가까운 미래의 여행 (시작일 > 오늘) - 시작일이 가장 가까운 일정
     * 3. 가장 최근에 종료된 여행 (종료일 < 오늘) - 종료일이 가장 가까운 일정
     * 4. 여행이 없는 경우 null 반환
     *
     * @param userId 사용자 ID
     * @return 대표 여행 ID 응답 (여행이 없으면 tripId가 null)
     */
    public RepresentativeTripResponse getRepresentativeTrip(Long userId) {
        LocalDate today = LocalDate.now();

        // 우선순위 1: 현재 진행 중인 여행 (가장 늦게 끝나는 것)
        List<Trip> ongoingTrips = tripRepository.findOngoingTripsByUserId(userId, today);
        if (!ongoingTrips.isEmpty()) {
            return RepresentativeTripResponse.builder()
                    .tripId(ongoingTrips.get(0).getTripId())
                    .build();
        }

        // 우선순위 2: 가장 가까운 미래 여행
        List<Trip> upcomingTrips = tripRepository.findUpcomingTripsByUserId(userId, today);
        if (!upcomingTrips.isEmpty()) {
            return RepresentativeTripResponse.builder()
                    .tripId(upcomingTrips.get(0).getTripId())
                    .build();
        }

        // 우선순위 3: 가장 최근에 종료된 여행
        List<Trip> recentlyEndedTrips = tripRepository.findRecentlyEndedTripsByUserId(userId, today);
        if (!recentlyEndedTrips.isEmpty()) {
            return RepresentativeTripResponse.builder()
                    .tripId(recentlyEndedTrips.get(0).getTripId())
                    .build();
        }

        // 우선순위 4: 여행이 없는 경우 null 반환
        return RepresentativeTripResponse.builder()
                .tripId(null)
                .build();
    }
}
