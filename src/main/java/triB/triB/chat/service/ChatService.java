package triB.triB.chat.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import triB.triB.auth.entity.User;
import triB.triB.auth.entity.UserStatus;
import triB.triB.auth.repository.UserRepository;
import triB.triB.chat.dto.*;
import triB.triB.chat.entity.*;
import triB.triB.chat.event.TripCreatedEvent;
import triB.triB.chat.event.TripErrorEvent;
import triB.triB.chat.repository.MessageBookmarkRepository;
import triB.triB.chat.repository.MessagePlaceDetailRepository;
import triB.triB.chat.repository.MessagePlaceRepository;
import triB.triB.chat.repository.MessageRepository;
import triB.triB.community.entity.Post;
import triB.triB.community.repository.PostImageRepository;
import triB.triB.community.repository.PostRepository;
import triB.triB.friendship.dto.UserResponse;
import triB.triB.global.exception.CustomException;
import triB.triB.global.exception.ErrorCode;
import triB.triB.global.infra.RedisClient;
import triB.triB.room.entity.Room;
import triB.triB.room.repository.RoomRepository;
import triB.triB.room.repository.UserRoomRepository;
import triB.triB.schedule.entity.Schedule;
import triB.triB.schedule.entity.Trip;
import triB.triB.schedule.entity.TripStatus;
import triB.triB.schedule.entity.VersionStatus;
import triB.triB.schedule.repository.ScheduleRepository;
import triB.triB.schedule.repository.TripRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatService {

    private final RoomRepository roomRepository;
    private final UserRoomRepository userRoomRepository;
    private final MessageRepository messageRepository;
    private final MessageBookmarkRepository messageBookmarkRepository;
    private final MessagePlaceRepository messagePlaceRepository;
    private final MessagePlaceDetailRepository messagePlaceDetailRepository;
    private final @Qualifier("aiModelWebClient") WebClient aiModelWebClient;
    private final ScheduleRepository scheduleRepository;
    private final TripRepository tripRepository;
    private final RedisClient redisClient;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher publisher;
    private final PostRepository postRepository;
    private final PostImageRepository postImageRepository;

    public RoomChatResponse getRoomMessages(Long userId, Long roomId){
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("해당 채팅방이 존재하지 않습니다."));

        if (!userRoomRepository.existsByUser_UserIdAndRoom_RoomId(userId, roomId))
            throw new BadCredentialsException("해당 채팅방에 대한 권한이 없습니다.");

        log.info("채팅 내용 조회 시작");

        List<Message> messages = messageRepository.findAllByRoom_RoomIdOrderByCreatedAtAsc(roomId);
        List<Long> messageIds = messages.stream()
                .map(Message::getMessageId)
                .toList();

        Map<Long, PlaceTag> placeTagMap = messagePlaceRepository.findByMessageIds(messageIds).stream()
                .collect(Collectors.toMap(mp -> mp.getMessage().getMessageId(), MessagePlace::getPlaceTag));

        Map<Long, Boolean> bookmarkMap = messageBookmarkRepository.findByMessageIds(messageIds).stream()
                .collect(Collectors.toMap(mb -> mb.getMessage().getMessageId(), mb -> true));

        Map<Long, MessagePlaceDetail> placeDetailMap = messagePlaceDetailRepository.findByMessageIds(messageIds).stream()
                .collect(Collectors.toMap(mpd -> mpd.getMessage().getMessageId(), mpd -> mpd));

        List<MessageResponse> response = messages.stream()
                .filter(Objects::nonNull)
                .map(message -> {
                    User user = message.getUser();
                    PlaceTag tag = placeTagMap.getOrDefault(message.getMessageId(), null);
                    Boolean isBookmarked = bookmarkMap.getOrDefault(message.getMessageId(), false);
                    PlaceDetail placeDetail = makePlaceDetail(placeDetailMap.getOrDefault(message.getMessageId(), null));

                    CommunityDetail communityDetail = null;
                    if (message.getMessageType() == MessageType.COMMUNITY_SHARE) {
                        Post p = postRepository.findById(Long.parseLong(message.getContent())).orElse(null);
                        if (p != null) {
                            String imageUrl = postImageRepository.findImageUrlByPostId(p.getPostId());
                            communityDetail = new CommunityDetail(p.getPostId(), p.getTitle(), imageUrl);
                        } else {
                            message.setMessageType(MessageType.TEXT);
                            message.setMessageStatus(MessageStatus.DELETE);
                            message.setContent("삭제된 게시글입니다.");
                            messageRepository.save(message);
                        }
                    }

                    Message reply = message.getReplyMessage();
                    ReplyMessage replyMessage = null;
                    if (reply != null) {
                        String replyContent;
                        if (reply.getMessageStatus() == MessageStatus.DELETE) {
                            // 삭제된 커뮤니티 게시글인 경우 내용을 그대로 사용
                            replyContent = reply.getContent();
                        } else if (reply.getMessageType() == MessageType.COMMUNITY_SHARE) {
                            replyContent = postRepository.findTitleByPostId(Long.parseLong(reply.getContent()));
                            if (replyContent == null) {
                                replyContent = "삭제된 게시글입니다.";
                            }
                        } else {
                            replyContent = reply.getContent();
                        }
                        replyMessage = new ReplyMessage(reply.getMessageId(), replyContent);
                    }

                    return MessageResponse.builder()
                            .actionType(null)
                            .user(new UserResponse(user.getUserId(), user.getNickname(), user.getPhotoUrl()))
                            .message(
                                    MessageDto.builder()
                                            .messageId(message.getMessageId())
                                            .content(message.getContent())
                                            .messageType(message.getMessageType())
                                            .messageStatus(message.getMessageStatus())
                                            .tag(tag)
                                            .isBookmarked(isBookmarked)
                                            .placeDetail(placeDetail)
                                            .communityDetail(communityDetail)
                                            .replyMessage(replyMessage)
                                            .build()
                            )
                            .createdAt(message.getCreatedAt())
                            .build();
                })
                .toList();

        return RoomChatResponse.builder()
                .roomName(room.getRoomName())
                .messages(response)
                .build();
    }

    public void startTripAsync(Long userId, Long roomId) {
        // 동기적으로 권한 체크
        if (!userRoomRepository.existsByUser_UserIdAndRoom_RoomId(userId, roomId))
            throw new BadCredentialsException("해당 권한이 없습니다.");

        // 동기적으로 락 획득 시도
        Boolean locked = redisClient.setIfAbsent("trip:create:lock",
                                                 String.valueOf(roomId),
                                                 String.valueOf(TripCreateStatus.WAITING),
                                                 600);

        if (!Boolean.TRUE.equals(locked)) {
            throw new CustomException(ErrorCode.TRIP_CREATING_IN_PROGRESS);
        }

        // 락 획득 성공 후 비동기 작업 시작
        try {
            makeTrip(roomId)
                    .subscribe(
                            tripId -> log.info("Trip 생성 완료: roomId={}, tripId={}", roomId, tripId),
                            err    -> log.error("Trip 생성 실패: roomId={}, err={}", roomId, err.toString())
                    );
        } catch (Exception e) {
            // subscribe 시작 실패 시 락 해제
            redisClient.deleteData("trip:create:lock", String.valueOf(roomId));
            log.error("Trip 생성 시작 실패: roomId={}, err={}", roomId, e.getMessage());
            throw e;
        }
    }

    protected Mono<Long> makeTrip(Long roomId){

        return Mono.fromCallable(() ->
                    roomRepository.findById(roomId)
                            .orElseThrow(()-> new EntityNotFoundException("해당 룸이 존재하지 않습니다."))
                )
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(room ->
                        Mono.fromCallable(() -> buildModelRequest(roomId,room))
                                .subscribeOn(Schedulers.boundedElastic())
                                .map(req -> Map.entry(room, req)))
                .flatMap(entry -> {
                    Room room = entry.getKey();
                    ModelRequest modelRequest = entry.getValue();

                    redisClient.setData("trip:create:lock", String.valueOf(roomId), String.valueOf(TripCreateStatus.RUNNING), 900);
                    log.info("모델 통신 시작: roomId={}", roomId);

                    return aiModelWebClient.post()
                            .uri("/api/v2/itinerary/generate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(modelRequest)
                            .retrieve()
                            .onStatus(
                                    HttpStatusCode::is4xxClientError,
                                    res -> res.createException().flatMap(e -> {
                                        log.error("AI 모델 요청 오류: {}", e.getMessage());
                                        publisher.publishEvent(new TripErrorEvent(roomId));
                                        return Mono.error(new CustomException(ErrorCode.MODEL_REQUEST_ERROR));
                                    })
                            )
                            .onStatus(
                                    HttpStatusCode::is5xxServerError,
                                    res -> res.createException().flatMap(e -> {
                                        log.error("AI 모델 서버 오류: {}", e.getMessage());
                                        publisher.publishEvent(new TripErrorEvent(roomId));
                                        return Mono.error(new CustomException(ErrorCode.MODEL_ERROR));
                                    })
                            )
                            .bodyToMono(ModelResponse.class)
                            .map(body -> Map.entry(room, body));
                })
                .flatMap(entry ->
                        Mono.fromCallable(() -> saveTripAndSchedule(entry.getKey(), entry.getValue()))
                                .subscribeOn(Schedulers.boundedElastic())
                )
                .doOnSuccess(tripId -> {
                    redisClient.deleteData("trip:create:lock", String.valueOf(roomId));
                    log.info("락 해제 완료(SUCCESS): roomId={}, tripId={}", roomId, tripId);
                })
                .onErrorResume(e -> {
                    try {
                        publisher.publishEvent(new TripErrorEvent(roomId));
                    } catch (Exception ignore) { /* 이벤트 발행 실패 무시 */ }
                    // 항상 락 해제
                    redisClient.deleteData("trip:create:lock", String.valueOf(roomId));
                    log.info("락 해제 완료(FAIL): roomId={}", roomId);

                    if (e instanceof CustomException) {
                        return Mono.error(e);
                    }
                    if (e instanceof WebClientRequestException) {
                        return Mono.error(new CustomException(ErrorCode.MODEL_CONNECTION_FAIL));
                    }
                    return Mono.error(new CustomException(ErrorCode.TRIP_SAVE_FAIL));
                });
    }

    @Transactional
    protected Long saveTripAndSchedule(Room room, ModelResponse body) {
        Trip existingTrip = tripRepository.findByRoomId(room.getRoomId());
        if (existingTrip != null){
            existingTrip.setVersionStatus(VersionStatus.OLD);
            tripRepository.save(existingTrip);
        }
        Trip t = Trip.builder()
                .roomId(room.getRoomId())
                .destination(room.getDestination())
                .versionStatus(VersionStatus.NEW)
                .travelMode(body.getTravelMode())
                .accommodationCostInfo(body.getAccommodationCostInfo())
                .budget(body.getBudget())
                .build();
        tripRepository.save(t);

        // DB에 성공적으로 저장
        body.getItinerary().stream()
                .forEach(itinerary -> {
                    itinerary.getVisits()
                            .forEach((visit) -> {
                                LocalDate date = room.getStartDate()
                                        .plusDays(itinerary.getDay() - 1); // + dayNumber - 1
                                LocalDateTime arrival = date.atTime(LocalTime.parse(visit.getArrival()));
                                LocalDateTime departure = date.atTime(LocalTime.parse(visit.getDeparture()));

                                Schedule schedule = Schedule.builder()
                                        .tripId(t.getTripId())
                                        .dayNumber(itinerary.getDay())
                                        .date(date)
                                        .visitOrder(visit.getOrder())
                                        .placeName(visit.getDisplayName())
                                        .placeTag(visit.getPlaceTag())
                                        .latitude(visit.getLatitude())
                                        .longitude(visit.getLongitude())
                                        .isVisit(false)
                                        .arrival(arrival)
                                        .departure(departure)
                                        .travelTime(String.valueOf(visit.getTravelTime()))
                                        .estimatedCost(visit.getEstimatedCost())
                                        .costExplanation(visit.getCostExplanation())
                                        .build();
                                scheduleRepository.save(schedule);
                            });
                });
        publisher.publishEvent(new TripCreatedEvent(
                t.getTripId(), room.getRoomId()
        ));
        return t.getTripId();
    }

    public TripCreateStatusResponse getTripStatus(Long userId, Long roomId) {
        if (!userRoomRepository.existsByUser_UserIdAndRoom_RoomId(userId, roomId))
            throw new BadCredentialsException("해당 권한이 없습니다.");
        Trip t;
        if (redisClient.getData("trip:create:lock", String.valueOf(roomId)) != null)
            return new TripCreateStatusResponse(TripCreateStatus.WAITING, null);
        else if ((t = tripRepository.findByRoomId(roomId)) != null)
            return new TripCreateStatusResponse(TripCreateStatus.SUCCESS, t.getTripId());
        else
            return new TripCreateStatusResponse(TripCreateStatus.NOT_STARTED, null);
    }

    private ModelRequest buildModelRequest(Long roomId, Room room){
        List<Message> messages = messageRepository.findAllByRoom_RoomIdOrderByCreatedAtAsc(roomId).stream()
                .filter(m -> m.getMessageStatus() != MessageStatus.DELETE)
                .toList();

        List<ModelRequest.ModelPlaceRequest> places = new ArrayList<>();
        List<String> mustVisit = new ArrayList<>();
        List<String> rule = new ArrayList<>();
        List<String> chat = new ArrayList<>();

        MessagePlace place = null;
        MessageBookmark bookmark = null;

        for (Message message : messages) {
            place = messagePlaceRepository.findByMessage_MessageId(message.getMessageId());
            bookmark = messageBookmarkRepository.findByMessage_MessageId(message.getMessageId());

            String content = message.getContent();

            if (message.getMessageType().equals(MessageType.COMMUNITY_SHARE)){
                Post p = postRepository.findById(Long.parseLong(content))
                        .orElseThrow(() -> new EntityNotFoundException("해당 게시글이 존재하지 않습니다."));
                Long tripId = p.getTripId();

                List<Schedule> schedules = scheduleRepository.findByTripIdOrderByDayNumberAscVisitOrderAsc(tripId).stream()
                        .filter(s -> s.getPlaceTag() != PlaceTag.HOME)
                        .toList();

                schedules.forEach(s ->
                        places.add(new ModelRequest.ModelPlaceRequest(s.getPlaceName(), s.getPlaceTag())));

            } // 장소 태그가 저장 되어있고 북마크 되어있음
            else if (place != null && bookmark != null){
                places.add(new ModelRequest.ModelPlaceRequest(content, place.getPlaceTag()));
                mustVisit.add(content);
            } // 장소태그만 저장되어있음
            else if (place != null) {
                places.add(new ModelRequest.ModelPlaceRequest(content, place.getPlaceTag()));
            } // 북마크만 되어있음
            else if (bookmark != null) {
                rule.add(content);
            }
            // 커뮤니티가 아닌 메세지의 경우 싹다 chat에 넣음
            if (!message.getMessageType().equals(MessageType.COMMUNITY_SHARE))
                chat.add(content);
        }
        return ModelRequest.builder()
                .days((int) ChronoUnit.DAYS.between(room.getStartDate(), room.getEndDate()) + 1)
                .startDate(room.getStartDate().toString())
                .country(room.getDestination())
                .members(userRoomRepository.countByRoom_RoomIdAndUserStatus(roomId, UserStatus.ACTIVE))
                .places(places)
                .mustVisit(mustVisit)
                .rule(rule)
                .chat(chat)
                .build();
    }

    private PlaceDetail makePlaceDetail(MessagePlaceDetail mpd){
        if (mpd == null)
            return null;

        return PlaceDetail.builder()
                .placeId(mpd.getPlaceId())
                .displayName(mpd.getDisplayName())
                .latitude(mpd.getLatitude())
                .longitude(mpd.getLongitude())
                .photoUrl(mpd.getPhotoUrl())
                .build();
    }
}
