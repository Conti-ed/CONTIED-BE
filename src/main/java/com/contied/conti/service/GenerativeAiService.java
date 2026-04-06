package com.contied.conti.service;

import com.contied.conti.dto.AiResponse;
import com.contied.conti.dto.PostContiByAiRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class GenerativeAiService {

    private final WebClient webClient;

    @Value("${AI_SERVER:http://localhost:8000}")
    private String aiServerUrl;

    public GenerativeAiService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public AiResponse generateContiByAi(PostContiByAiRequest request) {
        return webClient.post()
                .uri(aiServerUrl + "/api/conti")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(AiResponse.class)
                .block(); // Synchronous for simplicity of migration
    }
}
