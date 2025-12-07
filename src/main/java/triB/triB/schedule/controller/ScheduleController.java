package triB.triB.schedule.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import triB.triB.global.response.ApiResponse;
import triB.triB.global.security.UserPrincipal;
import triB.triB.schedule.dto.AccommodationCostResponse;
import triB.triB.schedule.dto.AddScheduleRequest;
import triB.triB.schedule.dto.BatchUpdateScheduleRequest;
import triB.triB.schedule.dto.DeleteScheduleResponse;
import triB.triB.schedule.dto.PreviewScheduleRequest;
import triB.triB.schedule.dto.ReorderScheduleRequest;
import triB.triB.schedule.dto.RepresentativeTripResponse;
import triB.triB.schedule.dto.ScheduleCostResponse;
import triB.triB.schedule.dto.ScheduleItemResponse;
import triB.triB.schedule.dto.TripListResponse;
import triB.triB.schedule.dto.TripScheduleResponse;
import triB.triB.schedule.dto.TripScheduleWithLocationResponse;
import triB.triB.schedule.dto.UpdateAccommodationRequest;
import triB.triB.schedule.dto.UpdateStayDurationRequest;
import triB.triB.schedule.dto.UpdateVisitTimeRequest;
import triB.triB.schedule.dto.VisitStatusUpdateRequest;
import triB.triB.schedule.dto.VisitStatusUpdateResponse;
import triB.triB.schedule.entity.TripFilterType;
import triB.triB.schedule.service.ScheduleService;
import triB.triB.schedule.service.TripService;
import triB.triB.schedule.service.TripStatusScheduler;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
@Tag(name = "Schedule", description = "일정 관리 API")
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final TripService tripService;
    private final TripStatusScheduler tripStatusScheduler;

    @GetMapping("/trips")
    @Operation(
            summary = "여행 목록 조회",
            description = "로그인한 사용자가 참여 중인 승인된 여행 목록을 조회합니다. 각 여행의 기본 정보, 참여자, 예산 정보를 포함합니다."
    )
    public ResponseEntity<ApiResponse<List<TripListResponse>>> getTripList(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        Long userId = userPrincipal.getUserId();
        List<TripListResponse> tripList = tripService.getMyTripList(userId);

        return ApiResponse.ok("여행 목록을 조회했습니다.", tripList);
    }

    @GetMapping("/trips/filtered")
    @Operation(
            summary = "필터링된 여행 목록 조회",
            description = "로그인한 사용자가 참여 중인 여행 목록을 필터링하여 조회합니다. " +
                    "필터 옵션: FUTURE(미래 여행, 기본값), PAST(과거 여행). " +
                    "미래 여행은 시작일 오름차순, 과거 여행은 시작일 내림차순으로 정렬됩니다."
    )
    public ResponseEntity<ApiResponse<List<TripListResponse>>> getFilteredTripList(
            @Parameter(
                    description = "여행 필터 타입 (FUTURE: 미래 여행, PAST: 과거 여행)",
                    example = "FUTURE"
            )
            @RequestParam(value = "filter", defaultValue = "FUTURE") TripFilterType filter,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        Long userId = userPrincipal.getUserId();
        List<TripListResponse> tripList = tripService.getFilteredTripList(userId, filter);

        return ApiResponse.ok("필터링된 여행 목록을 조회했습니다.", tripList);
    }

    @GetMapping("/trips/representative")
    @Operation(
            summary = "대표 여행 ID 조회",
            description = "사용자의 대표 여행 ID를 우선순위에 따라 반환합니다. " +
                    "우선순위: 1) 현재 진행 중인 여행 (가장 늦게 끝나는 것), " +
                    "2) 가장 가까운 미래 여행, " +
                    "3) 가장 최근에 종료된 여행, " +
                    "4) 여행이 없으면 null 반환"
    )
    public ResponseEntity<ApiResponse<RepresentativeTripResponse>> getRepresentativeTrip(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        Long userId = userPrincipal.getUserId();
        RepresentativeTripResponse response = tripService.getRepresentativeTrip(userId);

        return ApiResponse.ok("대표 여행을 조회했습니다.", response);
    }

    @GetMapping("/trips/{tripId}/schedules")
    @Operation(
            summary = "여행 일정 조회",
            description = "특정 여행의 특정 날짜 일정을 조회합니다. dayNumber를 지정하지 않으면 1일차를 반환합니다."
    )
    public ResponseEntity<ApiResponse<TripScheduleWithLocationResponse>> getTripSchedules(
            @Parameter(description = "여행 ID", required = true)
            @PathVariable Long tripId,

            @Parameter(description = "조회할 일차 (기본값: 1)", example = "1")
            @RequestParam(required = false, defaultValue = "1") Integer dayNumber,

            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        TripScheduleWithLocationResponse response = scheduleService.getTripSchedulesWithLocation(
                tripId,
                dayNumber,
                userPrincipal.getUserId()
        );

        return ApiResponse.ok("일정을 조회했습니다.", response);
    }

    @PatchMapping("/trips/{tripId}/schedules/{scheduleId}/visit-status")
    @Operation(
            summary = "방문 완료/미완료 변경",
            description = "일정의 방문 완료 상태를 변경합니다."
    )
    public ResponseEntity<ApiResponse<VisitStatusUpdateResponse>> updateVisitStatus(
            @Parameter(description = "여행 ID", required = true)
            @PathVariable Long tripId,

            @Parameter(description = "일정 ID", required = true)
            @PathVariable Long scheduleId,

            @Parameter(description = "방문 상태 변경 요청", required = true)
            @RequestBody @Valid VisitStatusUpdateRequest request,

            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        VisitStatusUpdateResponse response = scheduleService.updateVisitStatus(
                tripId,
                scheduleId,
                request,
                userPrincipal.getUserId()
        );

        return ApiResponse.ok("방문 상태를 변경했습니다.", response);
    }

    @GetMapping("/trips/{tripId}/schedules/{scheduleId}/cost")
    @Operation(
            summary = "일정 비용 정보 조회",
            description = "특정 일정의 예상 비용과 비용 설명을 조회합니다."
    )
    public ResponseEntity<ApiResponse<ScheduleCostResponse>> getScheduleCost(
            @Parameter(description = "여행 ID", required = true)
            @PathVariable Long tripId,

            @Parameter(description = "일정 ID", required = true)
            @PathVariable Long scheduleId,

            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        ScheduleCostResponse response = scheduleService.getScheduleCost(
                tripId,
                scheduleId,
                userPrincipal.getUserId()
        );

        return ApiResponse.ok("일정 비용 정보를 조회했습니다.", response);
    }

    @GetMapping("/trips/{tripId}/schedules/cost")
    @Operation(
            summary = "여행 전체 일정 비용 정보 조회",
            description = "특정 여행의 모든 일정(숙소 제외)에 대한 예상 비용과 비용 설명을 조회합니다."
    )
    public ResponseEntity<ApiResponse<List<ScheduleCostResponse>>> getAllScheduleCosts(
            @Parameter(description = "여행 ID", required = true)
            @PathVariable Long tripId,

            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        List<ScheduleCostResponse> response = scheduleService.getAllScheduleCosts(
                tripId,
                userPrincipal.getUserId()
        );

        return ApiResponse.ok("전체 일정 비용 정보를 조회했습니다.", response);
    }

    @GetMapping("/trips/{tripId}/accommodation-cost")
    @Operation(
            summary = "여행 숙박 비용 정보 조회",
            description = "특정 여행의 전체 숙박 비용 정보를 조회합니다."
    )
    public ResponseEntity<ApiResponse<AccommodationCostResponse>> getAccommodationCost(
            @Parameter(description = "여행 ID", required = true)
            @PathVariable Long tripId,

            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        AccommodationCostResponse response = scheduleService.getAccommodationCost(
                tripId,
                userPrincipal.getUserId()
        );

        return ApiResponse.ok("숙박 비용 정보를 조회했습니다.", response);
    }

    @PatchMapping("/trips/{tripId}/schedules/{scheduleId}/reorder")
    @Operation(
            summary = "일정 순서 변경",
            description = "특정 날짜의 일정 순서를 변경하고 이동시간을 재계산합니다."
    )
    public ResponseEntity<ApiResponse<TripScheduleResponse>> reorderSchedule(
            @Parameter(description = "여행 ID", required = true)
            @PathVariable Long tripId,

            @Parameter(description = "일정 ID", required = true)
            @PathVariable Long scheduleId,

            @Parameter(description = "순서 변경 요청", required = true)
            @RequestBody @Valid ReorderScheduleRequest request,

            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        TripScheduleResponse response = scheduleService.reorderSchedule(
                tripId,
                scheduleId,
                request,
                userPrincipal.getUserId()
        );

        return ApiResponse.ok("일정 순서를 변경했습니다.", response);
    }

    @PatchMapping("/trips/{tripId}/schedules/{scheduleId}/stay-duration")
    @Operation(
            summary = "체류시간 수정",
            description = "특정 일정의 체류시간을 수정하고 이후 일정 시간을 재계산합니다."
    )
    public ResponseEntity<ApiResponse<ScheduleItemResponse>> updateStayDuration(
            @Parameter(description = "여행 ID", required = true)
            @PathVariable Long tripId,

            @Parameter(description = "일정 ID", required = true)
            @PathVariable Long scheduleId,

            @Parameter(description = "체류시간 수정 요청", required = true)
            @RequestBody @Valid UpdateStayDurationRequest request,

            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        ScheduleItemResponse response = scheduleService.updateStayDuration(
                tripId,
                scheduleId,
                request,
                userPrincipal.getUserId()
        );

        return ApiResponse.ok("체류시간을 수정했습니다.", response);
    }

    @PatchMapping("/trips/{tripId}/schedules/{scheduleId}/visit-time")
    @Operation(
            summary = "방문 시간 수정",
            description = "특정 일정의 방문 시간을 수정하고 이후 일정 시간을 재계산합니다."
    )
    public ResponseEntity<ApiResponse<ScheduleItemResponse>> updateVisitTime(
            @Parameter(description = "여행 ID", required = true)
            @PathVariable Long tripId,

            @Parameter(description = "일정 ID", required = true)
            @PathVariable Long scheduleId,

            @Parameter(description = "방문 시간 수정 요청", required = true)
            @RequestBody @Valid UpdateVisitTimeRequest request,

            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        ScheduleItemResponse response = scheduleService.updateVisitTime(
                tripId,
                scheduleId,
                request,
                userPrincipal.getUserId()
        );

        return ApiResponse.ok("방문 시간을 수정했습니다.", response);
    }

    @PostMapping("/trips/{tripId}/schedules")
    @Operation(
            summary = "일정 추가",
            description = "특정 날짜의 마지막 일정으로 새로운 장소를 추가합니다."
    )
    public ResponseEntity<ApiResponse<ScheduleItemResponse>> addSchedule(
            @Parameter(description = "여행 ID", required = true)
            @PathVariable Long tripId,

            @Parameter(description = "일정 추가 요청", required = true)
            @RequestBody @Valid AddScheduleRequest request,

            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        ScheduleItemResponse response = scheduleService.addScheduleToDay(
                tripId,
                request,
                userPrincipal.getUserId()
        );

        return ApiResponse.created("일정을 추가했습니다.", response);
    }

    @DeleteMapping("/trips/{tripId}/schedules/{scheduleId}")
    @Operation(
            summary = "일정 삭제",
            description = "특정 일정을 삭제하고 이후 일정의 순서와 이동시간을 재계산합니다."
    )
    public ResponseEntity<ApiResponse<DeleteScheduleResponse>> deleteSchedule(
            @Parameter(description = "여행 ID", required = true)
            @PathVariable Long tripId,

            @Parameter(description = "일정 ID", required = true)
            @PathVariable Long scheduleId,

            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        DeleteScheduleResponse response = scheduleService.deleteSchedule(
                tripId,
                scheduleId,
                userPrincipal.getUserId()
        );

        return ApiResponse.ok("일정을 삭제했습니다.", response);
    }

    @PatchMapping("/trips/{tripId}/accommodation")
    @Operation(
            summary = "숙소 변경 (레거시 API)",
            description = "dayNumber 기반으로 해당 날짜의 숙소를 변경합니다. " +
                    "새로운 일괄 수정 API(POST /api/v1/trips/{tripId}/schedules/batch-update)에서 " +
                    "ModificationType.UPDATE_ACCOMMODATION을 사용하면 scheduleId 기반으로 더 정확한 숙소 변경이 가능합니다."
    )
    public ResponseEntity<ApiResponse<ScheduleItemResponse>> updateAccommodation(
            @Parameter(description = "여행 ID", required = true)
            @PathVariable Long tripId,

            @Parameter(description = "숙소 변경 요청", required = true)
            @RequestBody @Valid UpdateAccommodationRequest request,

            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        ScheduleItemResponse response = scheduleService.updateAccommodation(
                tripId,
                request,
                userPrincipal.getUserId()
        );

        return ApiResponse.ok("숙소를 변경했습니다.", response);
    }

    @PostMapping("/trips/{tripId}/schedules/preview")
    @Operation(
            summary = "일정 변경 미리보기",
            description = "여러 일정 변경사항을 적용한 결과를 미리 확인합니다. DB에는 저장되지 않습니다."
    )
    public ResponseEntity<ApiResponse<TripScheduleResponse>> previewScheduleChanges(
            @Parameter(description = "여행 ID", required = true)
            @PathVariable Long tripId,

            @Parameter(description = "일정 변경 미리보기 요청", required = true)
            @RequestBody @Valid PreviewScheduleRequest request,

            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        TripScheduleResponse response = scheduleService.previewScheduleChanges(
                tripId,
                request,
                userPrincipal.getUserId()
        );

        return ApiResponse.ok("일정 변경사항을 미리보기합니다.", response);
    }

    @PostMapping("/trips/{tripId}/schedules/batch-update")
    @Operation(
            summary = "일정 일괄 수정",
            description = "여러 일정 변경사항을 한 번에 적용하고 DB에 저장합니다."
    )
    public ResponseEntity<ApiResponse<TripScheduleResponse>> batchUpdateSchedule(
            @Parameter(description = "여행 ID", required = true)
            @PathVariable Long tripId,

            @Parameter(description = "일정 일괄 수정 요청", required = true)
            @RequestBody @Valid BatchUpdateScheduleRequest request,

            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        TripScheduleResponse response = scheduleService.batchUpdateSchedule(
                tripId,
                request,
                userPrincipal.getUserId()
        );

        return ApiResponse.ok("일정 변경사항을 저장했습니다.", response);
    }

    @PostMapping("/trips/status/update-past")
    @Operation(
            summary = "과거 여행 상태 수동 업데이트",
            description = "종료일이 지난 READY 상태의 여행을 ACCEPTED 상태로 수동 업데이트합니다. " +
                    "정상적으로는 매일 새벽 2시에 자동 실행되지만, 필요시 수동으로 실행할 수 있습니다. " +
                    "인증이 필요하지 않습니다."
    )
    public ResponseEntity<ApiResponse<Integer>> updatePastTripStatuses() {
        int updatedCount = tripStatusScheduler.updatePastTripStatusesManually();

        return ApiResponse.ok(
                updatedCount > 0
                    ? updatedCount + "개의 여행 상태가 ACCEPTED로 변경되었습니다."
                    : "업데이트할 과거 여행이 없습니다.",
                updatedCount
        );
    }
}
