package triB.triB.schedule.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import triB.triB.schedule.entity.Trip;
import triB.triB.schedule.entity.TripStatus;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TripRepository extends JpaRepository<Trip, Long> {

    Trip findByRoomId(Long roomId);

    /**
     * 사용자가 참여한 특정 상태의 여행 목록 조회
     * @param userId 사용자 ID
     * @param tripStatus 여행 상태 (ACCEPTED, READY 등)
     * @return 여행 목록 (최신순)
     */
    @Query("SELECT t FROM Trip t " +
           "JOIN t.room r " +
           "WHERE t.tripStatus = :tripStatus " +
           "AND EXISTS (SELECT 1 FROM UserRoom ur WHERE ur.room.roomId = r.roomId AND ur.user.userId = :userId) " +
           "ORDER BY t.createdAt DESC")
    List<Trip> findByUserIdAndTripStatus(@Param("userId") Long userId, @Param("tripStatus") TripStatus tripStatus);

    /**
     * READY 상태이면서 종료일이 지난 여행 목록 조회
     * @param currentDate 현재 날짜
     * @return READY 상태의 과거 여행 목록
     */
    @Query("SELECT t FROM Trip t " +
           "JOIN t.room r " +
           "WHERE t.tripStatus = 'READY' " +
           "AND r.endDate < :currentDate")
    List<Trip> findReadyTripsBeforeEndDate(@Param("currentDate") LocalDate currentDate);

    /**
     * 사용자의 미래 여행 조회 (READY 상태, startDate 오름차순)
     * @param userId 사용자 ID
     * @param tripStatus 여행 상태
     * @return 미래 여행 목록 (시작일 오름차순)
     */
    @Query("SELECT t FROM Trip t " +
           "JOIN t.room r " +
           "WHERE t.tripStatus = :tripStatus " +
           "AND EXISTS (SELECT 1 FROM UserRoom ur WHERE ur.room.roomId = r.roomId AND ur.user.userId = :userId) " +
           "ORDER BY r.startDate ASC, t.createdAt DESC")
    List<Trip> findFutureTripsByUserId(@Param("userId") Long userId, @Param("tripStatus") TripStatus tripStatus);

    /**
     * 사용자의 과거 여행 조회 (ACCEPTED 상태, startDate 내림차순)
     * @param userId 사용자 ID
     * @param tripStatus 여행 상태
     * @return 과거 여행 목록 (시작일 내림차순)
     */
    @Query("SELECT t FROM Trip t " +
           "JOIN t.room r " +
           "WHERE t.tripStatus = :tripStatus " +
           "AND EXISTS (SELECT 1 FROM UserRoom ur WHERE ur.room.roomId = r.roomId AND ur.user.userId = :userId) " +
           "ORDER BY r.startDate DESC, t.createdAt DESC")
    List<Trip> findPastTripsByUserId(@Param("userId") Long userId, @Param("tripStatus") TripStatus tripStatus);

    /**
     * 현재 진행 중인 여행 조회 (시작일 <= 오늘 <= 종료일)
     * @param userId 사용자 ID
     * @param currentDate 현재 날짜
     * @return 진행 중인 여행 목록 (종료일 내림차순 - 가장 늦게 끝나는 것 우선)
     */
    @Query("SELECT t FROM Trip t " +
           "JOIN t.room r " +
           "WHERE r.startDate <= :currentDate " +
           "AND r.endDate >= :currentDate " +
           "AND EXISTS (SELECT 1 FROM UserRoom ur WHERE ur.room.roomId = r.roomId AND ur.user.userId = :userId) " +
           "ORDER BY r.endDate DESC, t.tripId ASC")
    List<Trip> findOngoingTripsByUserId(@Param("userId") Long userId, @Param("currentDate") LocalDate currentDate);

    /**
     * 가장 가까운 미래 여행 조회 (시작일 > 오늘)
     * @param userId 사용자 ID
     * @param currentDate 현재 날짜
     * @return 미래 여행 목록 (시작일 오름차순, 같으면 tripId 오름차순)
     */
    @Query("SELECT t FROM Trip t " +
           "JOIN t.room r " +
           "WHERE r.startDate > :currentDate " +
           "AND EXISTS (SELECT 1 FROM UserRoom ur WHERE ur.room.roomId = r.roomId AND ur.user.userId = :userId) " +
           "ORDER BY r.startDate ASC, t.tripId ASC")
    List<Trip> findUpcomingTripsByUserId(@Param("userId") Long userId, @Param("currentDate") LocalDate currentDate);

    /**
     * 가장 최근에 종료된 여행 조회 (종료일 < 오늘)
     * @param userId 사용자 ID
     * @param currentDate 현재 날짜
     * @return 과거 여행 목록 (종료일 내림차순 - 가장 최근에 끝난 것 우선)
     */
    @Query("SELECT t FROM Trip t " +
           "JOIN t.room r " +
           "WHERE r.endDate < :currentDate " +
           "AND EXISTS (SELECT 1 FROM UserRoom ur WHERE ur.room.roomId = r.roomId AND ur.user.userId = :userId) " +
           "ORDER BY r.endDate DESC, t.tripId ASC")
    List<Trip> findRecentlyEndedTripsByUserId(@Param("userId") Long userId, @Param("currentDate") LocalDate currentDate);
}
