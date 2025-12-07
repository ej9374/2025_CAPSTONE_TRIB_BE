package triB.triB.schedule.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import triB.triB.schedule.entity.Schedule;

import java.util.List;
import java.util.Optional;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    /**
     * 특정 여행의 모든 일정 조회 (일차, 방문 순서 정렬)
     */
    List<Schedule> findByTripIdOrderByDayNumberAscVisitOrderAsc(Long tripId);

    /**
     * 특정 여행의 특정 날짜 일정 조회 (방문 순서 정렬)
     */
    List<Schedule> findByTripIdAndDayNumber(Long tripId, Integer dayNumber);

    /**
     * 특정 일정 조회 (권한 검증용)
     */
    Optional<Schedule> findByScheduleIdAndTripId(Long scheduleId, Long tripId);

    /**
     * 특정 여행의 모든 일정 조회 (숙소 제외, 일차 및 방문 순서 정렬)
     */
    @Query("SELECT s FROM Schedule s WHERE s.tripId = :tripId AND s.placeTag != triB.triB.chat.entity.PlaceTag.HOME ORDER BY s.dayNumber ASC, s.visitOrder ASC")
    List<Schedule> findByTripIdExcludingHome(@Param("tripId") Long tripId);
}
