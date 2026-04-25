package com.contied.conti.service;

import com.contied.conti.dto.AiResponse;
import com.contied.conti.dto.PostContiByAiRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class GenerativeAiService {

    private final WebClient webClient;

    @Value("${ai.server.url:http://localhost:8000}")
    private String aiServerUrl;

    public GenerativeAiService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    /**
     * AI 서버에 콘티 생성 요청을 보내고 Mono<AiResponse>를 반환한다.
     * 호출자는 .block(Duration) 으로 명시적 타임아웃과 함께 구독해야 한다.
     */
    public Mono<AiResponse> generateContiByAi(PostContiByAiRequest request) {
        log.info("AI 서버로 요청 전송: {} -> keywords={}, bibleVerseRange={}",
                aiServerUrl, request.getKeywords(), request.getBibleVerseRange());

        Map<String, Object> aiPayload = new HashMap<>();
        aiPayload.put("keywords", request.getKeywords());
        aiPayload.put("bible_verse_range", request.getBibleVerseRange());
        if (request.getSeed() != null) {
            aiPayload.put("seed", request.getSeed());
        }

        return webClient.post()
                .uri(aiServerUrl + "/api/conti")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(aiPayload)
                .retrieve()
                .bodyToMono(AiResponse.class)
                .timeout(Duration.ofSeconds(120))
                .doOnSuccess(r -> log.info("AI 서버 응답 수신 완료: title={}", r != null ? r.getTitle() : "null"))
                .doOnError(WebClientResponseException.class,
                        e -> log.error("AI 서버 응답 오류 (HTTP {}): {}", e.getStatusCode(), e.getResponseBodyAsString()))
                .doOnError(e -> !(e instanceof WebClientResponseException),
                        e -> log.error("AI 서버 통신 오류: {}", e.getMessage(), e));
    }
}
