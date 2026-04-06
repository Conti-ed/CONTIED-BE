package com.contied.conti.service;

import com.contied.conti.dto.AiResponse;
import com.contied.conti.dto.PostContiByAiRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

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

    public AiResponse generateContiByAi(PostContiByAiRequest request) {
        log.info("AI 서버로 요청 전송: {} -> keywords={}, bibleVerseRange={}", 
                aiServerUrl, request.getKeywords(), request.getBibleVerseRange());
        
        // AI 서버(FastAPI)는 snake_case를 기대하므로 변환
        Map<String, Object> aiPayload = new HashMap<>();
        aiPayload.put("keywords", request.getKeywords());
        aiPayload.put("bible_verse_range", request.getBibleVerseRange());
        
        try {
            AiResponse response = webClient.post()
                    .uri(aiServerUrl + "/api/conti")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(aiPayload)
                    .retrieve()
                    .bodyToMono(AiResponse.class)
                    .timeout(Duration.ofSeconds(120))
                    .block();
            
            log.info("AI 서버 응답 수신 완료: title={}", response != null ? response.getTitle() : "null");
            return response;
        } catch (WebClientResponseException e) {
            log.error("AI 서버 응답 오류 (HTTP {}): {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("AI 서버 응답 오류: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("AI 서버 통신 오류: {}", e.getMessage(), e);
            throw new RuntimeException("AI 서버와 통신할 수 없습니다: " + e.getMessage(), e);
        }
    }
}
