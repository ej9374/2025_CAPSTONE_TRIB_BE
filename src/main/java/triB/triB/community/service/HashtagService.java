package triB.triB.community.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import triB.triB.chat.dto.GeminiHashtagResponse;
import triB.triB.chat.dto.GeminiRequest;
import triB.triB.community.entity.Hashtag;
import triB.triB.community.entity.TagType;
import triB.triB.community.repository.HashtagRepository;
import triB.triB.schedule.entity.Schedule;
import triB.triB.schedule.entity.Trip;
import triB.triB.schedule.repository.ScheduleRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 해시태그 생성 및 관리 서비스
 * Gemini API를 활용한 AI 해시태그 자동 생성
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class HashtagService {

    @Value("${gemini.model}")
    private String model;

    @Value("${gemini.api-key}")
    private String apiKey;

    private final @Qualifier("geminiWebClient") WebClient geminiWebClient;
    private final HashtagRepository hashtagRepository;
    private final ScheduleRepository scheduleRepository;

    /**
     * 일정 공유 게시글을 위한 AI 해시태그 생성
     * 목적지, 여행 기간, 여행 일정을 기반으로 해시태그를 생성합니다.
     *
     * @param trip 여행 정보 (목적지, 기간 포함)
     * @return 생성된 해시태그 리스트
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<Hashtag> generateHashtagsForTripShare(Trip trip) {
        try {
            // 1. 여행 일정 조회
            List<Schedule> schedules = scheduleRepository.findByTripIdOrderByDayNumberAscVisitOrderAsc(trip.getTripId());

            // 2. Gemini에 전달할 프롬프트 구성
            String prompt = buildPromptForTripShare();
            String content = buildContentForTripShare(trip, schedules);

            // 3. Gemini API 호출
            List<String> tagNames = callGeminiApiForHashtags(prompt, content);

            // 4. 해시태그 저장 및 반환 (findOrCreate 패턴)
            return getOrCreateHashtags(tagNames, TagType.CUSTOM_AI);

        } catch (Exception e) {
            log.error("AI 해시태그 생성 실패. Fallback 해시태그 반환: {}", e.getMessage(), e);
            // AI 생성 실패 시 기본 해시태그 반환
            return getFallbackHashtags(trip);
        }
    }

    /**
     * Gemini API 프롬프트 생성 - 시스템 지시
     */
    private String buildPromptForTripShare() {
        return """
                당신은 여행 일정과 목적지를 분석하여 관련성 높은 해시태그를 생성하는 전문가입니다.

                [규칙]
                1) 주어진 여행 정보(목적지, 기간, 일정)를 종합적으로 분석하여 해시태그를 생성합니다.
                2) 해시태그는 3~5개로 제한하며, 여행의 핵심 키워드를 반영합니다.
                3) 각 해시태그는 한글로 작성하며, 50자 이내로 제한합니다.
                4) '#' 기호 없이 단어만 작성합니다.
                5) 해시태그는 쉼표(,)로 구분하여 작성합니다.
                6) 중복되는 의미의 해시태그는 제외합니다.
                7) 국가/도시명, 방문 장소의 특성, 여행 테마, 특별한 경험 등을 반영합니다.

                [해시태그 예시]
                - 목적지 관련: 파리여행, 프랑스, 유럽여행
                - 장소 관련: 에펠탑, 루브르박물관, 몽마르뜨
                - 테마 관련: 맛집투어, 박물관, 자연여행, 도시여행
                - 스타일 관련: 힐링, 가족여행, 액티비티, 럭셔리
                - 경험 관련: 일출, 야경, 현지음식, 쇼핑

                [출력 형식]
                쉼표로 구분된 해시태그만 출력합니다. 부가 설명이나 따옴표는 포함하지 않습니다.
                예시: 파리여행, 에펠탑, 루브르박물관, 유럽, 가을여행

                이제 아래 여행 정보를 분석하여 해시태그를 생성하십시오.
                """;
    }

    /**
     * Gemini API에 전달할 실제 여행 정보 컨텐츠
     * 목적지, 여행 기간, 여행 일정을 포함
     */
    private String buildContentForTripShare(Trip trip, List<Schedule> schedules) {
        StringBuilder content = new StringBuilder();
        content.append("여행 정보:\n");
        content.append("- 목적지: ").append(trip.getDestination()).append("\n");

        if (trip.getRoom() != null) {
            content.append("- 여행 시작일: ").append(trip.getRoom().getStartDate()).append("\n");
            content.append("- 여행 종료일: ").append(trip.getRoom().getEndDate()).append("\n");
        }

        // 여행 일정 추가
        if (schedules != null && !schedules.isEmpty()) {
            content.append("\n여행 일정:\n");

            int currentDay = -1;
            for (Schedule schedule : schedules) {
                // 새로운 일차 시작
                if (schedule.getDayNumber() != currentDay) {
                    currentDay = schedule.getDayNumber();
                    content.append("\n[").append(currentDay).append("일차] ");
                } else {
                    content.append(" → ");
                }

                // 장소명 추가
                content.append(schedule.getPlaceName());
            }
        }

        return content.toString();
    }

    /**
     * Gemini API 호출 및 해시태그 파싱
     */
    private List<String> callGeminiApiForHashtags(String prompt, String content) {
        // 1. GeminiRequest 생성
        GeminiRequest request = GeminiRequest.builder()
                .systemInstruction(
                        GeminiRequest.SystemInstruction.builder()
                                .parts(List.of(
                                        GeminiRequest.ofText(prompt)
                                ))
                                .build()
                )
                .contents(List.of(
                        GeminiRequest.Content.builder()
                                .parts(List.of(
                                        GeminiRequest.ofText(content)
                                ))
                                .build()
                ))
                .build();

        // 2. API URL 구성
        String url = "/v1beta/models/" + model + ":generateContent?key=" + apiKey;

        // 3. WebClient로 Gemini API 호출
        String generatedText = geminiWebClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .onStatus(
                        HttpStatusCode::is4xxClientError,
                        res -> res.createException().flatMap(Mono::error)
                )
                .onStatus(
                        HttpStatusCode::is5xxServerError,
                        res -> res.createException().flatMap(Mono::error)
                )
                .bodyToMono(String.class)
                .map(body -> {
                    try {
                        ObjectMapper mapper = new ObjectMapper();
                        GeminiHashtagResponse response = mapper.readValue(body, GeminiHashtagResponse.class);

                        // 응답에서 텍스트 추출
                        String text = response.extractText();

                        // Markdown 코드 블록 제거
                        text = text.trim()
                                .replaceAll("```json", "")
                                .replaceAll("```", "")
                                .trim();

                        return text;
                    } catch (Exception e) {
                        log.error("Gemini 응답 파싱 실패: {}", e.getMessage(), e);
                        throw new RuntimeException("Gemini 응답 파싱에 실패했습니다.", e);
                    }
                })
                .block(); // 동기 방식으로 결과 대기

        // null 체크
        if (generatedText == null || generatedText.isEmpty()) {
            log.error("Gemini API 응답이 비어있습니다.");
            throw new RuntimeException("Gemini API 응답이 비어있습니다.");
        }

        // 4. 해시태그 파싱
        List<String> parsedTags = parseHashtagsFromText(generatedText);

        // 빈 리스트 체크
        if (parsedTags.isEmpty()) {
            log.error("파싱된 해시태그가 없습니다. 원본 텍스트: {}", generatedText);
            throw new RuntimeException("해시태그 파싱 결과가 비어있습니다.");
        }

        return parsedTags;
    }

    /**
     * 쉼표로 구분된 텍스트에서 해시태그 추출
     */
    private List<String> parseHashtagsFromText(String text) {
        if (text == null || text.isEmpty()) {
            return new ArrayList<>();
        }

        return Arrays.stream(text.split(","))
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .filter(tag -> tag.length() <= 50) // 50자 제한
                .limit(5) // 최대 5개
                .collect(Collectors.toList());
    }

    /**
     * 해시태그 findOrCreate 패턴
     * 이미 존재하는 해시태그는 재사용하고, 없으면 새로 생성
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<Hashtag> getOrCreateHashtags(List<String> tagNames, TagType tagType) {
        List<Hashtag> hashtags = new ArrayList<>();

        for (String tagName : tagNames) {
            Hashtag hashtag = findOrCreateHashtag(tagName, tagType);
            hashtags.add(hashtag);
        }

        return hashtags;
    }

    /**
     * 단일 해시태그 findOrCreate
     * 먼저 조회하고, 없을 때만 저장
     */
    private Hashtag findOrCreateHashtag(String tagName, TagType tagType) {
        // 1. 먼저 DB에서 조회
        Optional<Hashtag> existing = hashtagRepository.findByTagName(tagName);
        if (existing.isPresent()) {
            log.debug("기존 해시태그 사용: {}", tagName);
            return existing.get();
        }

        // 2. 없으면 새로 생성하여 저장
        try {
            Hashtag newTag = Hashtag.builder()
                    .tagName(tagName)
                    .tagType(tagType)
                    .build();

            log.debug("새 해시태그 저장: {}", tagName);
            return hashtagRepository.save(newTag);

        } catch (Exception e) {
            // 동시성 이슈로 다른 트랜잭션이 먼저 저장한 경우, 다시 조회
            log.warn("해시태그 저장 중 중복 발생, 재조회: {} - {}", tagName, e.getMessage());
            return hashtagRepository.findByTagName(tagName)
                    .orElseThrow(() -> new RuntimeException("해시태그 생성 실패: " + tagName, e));
        }
    }

    /**
     * AI 생성 실패 시 기본 해시태그 반환
     */
    private List<Hashtag> getFallbackHashtags(Trip trip) {
        log.info("Fallback 해시태그 생성: {}", trip.getDestination());

        List<String> fallbackTags = Arrays.asList(
                trip.getDestination(),
                "여행",
                "추억"
        );

        return getOrCreateHashtags(fallbackTags, TagType.CUSTOM_AI);
    }
}