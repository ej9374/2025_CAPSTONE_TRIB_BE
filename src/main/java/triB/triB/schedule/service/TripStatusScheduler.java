package triB.triB.schedule.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import triB.triB.schedule.entity.Trip;
import triB.triB.schedule.entity.TripStatus;
import triB.triB.schedule.repository.TripRepository;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TripStatusScheduler {

    private final TripRepository tripRepository;

    /**
     * 매일 새벽 2시에 과거 여행 상태를 자동으로 업데이트
     * READY 상태이면서 종료일이 지난 여행을 ACCEPTED로 변경
     * cron = "초 분 시 일 월 요일"
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void updatePastTripStatuses() {
        log.info("과거 여행 상태 업데이트 시작");

        try {
            LocalDate today = LocalDate.now();
            List<Trip> pastTrips = tripRepository.findReadyTripsBeforeEndDate(today);

            if (pastTrips.isEmpty()) {
                log.info("업데이트할 과거 여행이 없습니다.");
                return;
            }

            // READY -> ACCEPTED 상태 변경
            pastTrips.forEach(trip -> {
                trip.setTripStatus(TripStatus.ACCEPTED);
                log.debug("여행 상태 업데이트: tripId={}, destination={}, endDate={}",
                         trip.getTripId(),
                         trip.getDestination(),
                         trip.getRoom().getEndDate());
            });

            log.info("과거 여행 상태 업데이트 완료: {}개의 여행이 ACCEPTED로 변경되었습니다.", pastTrips.size());

        } catch (Exception e) {
            log.error("과거 여행 상태 업데이트 중 오류 발생", e);
        }
    }

    /**
     * 수동으로 과거 여행 상태를 업데이트하는 메서드
     * API 엔드포인트에서 호출용
     * @return 업데이트된 여행 수
     */
    @Transactional
    public int updatePastTripStatusesManually() {
        log.info("수동 과거 여행 상태 업데이트 시작");

        LocalDate today = LocalDate.now();
        List<Trip> pastTrips = tripRepository.findReadyTripsBeforeEndDate(today);

        if (pastTrips.isEmpty()) {
            log.info("업데이트할 과거 여행이 없습니다.");
            return 0;
        }

        // READY -> ACCEPTED 상태 변경
        pastTrips.forEach(trip -> {
            trip.setTripStatus(TripStatus.ACCEPTED);
            log.debug("여행 상태 업데이트: tripId={}, destination={}, endDate={}",
                     trip.getTripId(),
                     trip.getDestination(),
                     trip.getRoom().getEndDate());
        });

        int updatedCount = pastTrips.size();
        log.info("수동 과거 여행 상태 업데이트 완료: {}개의 여행이 ACCEPTED로 변경되었습니다.", updatedCount);

        return updatedCount;
    }
}
