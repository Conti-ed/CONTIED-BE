package com.contied.conti.controller;

import com.contied.conti.dto.ContiResponse;
import com.contied.conti.dto.PostContiByAiRequest;
import com.contied.conti.service.ContiService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/conti")
@RequiredArgsConstructor
public class ContiController {

    private final ContiService contiService;

    @GetMapping
    public Page<ContiResponse> getAllContis(Pageable pageable) {
        return contiService.getAllContis(pageable);
    }

    @GetMapping("/{id}")
    public ContiResponse getConti(@PathVariable Long id) {
        return contiService.getContiById(id);
    }

    @GetMapping("/my")
    public List<ContiResponse> getMyContis(@AuthenticationPrincipal String email) {
        return contiService.getMyContis(email);
    }

    @PostMapping("/ai")
    public ContiResponse createContiByAi(@AuthenticationPrincipal String email, @RequestBody PostContiByAiRequest request) {
        return contiService.createContiByAi(email, request);
    }

    @DeleteMapping("/{id}")
    public void deleteConti(@AuthenticationPrincipal String email, @PathVariable Long id) {
        contiService.deleteConti(email, id);
    }

    @PostMapping("/{id}/like")
    public void likeConti(@AuthenticationPrincipal String email, @PathVariable Long id) {
        contiService.likeConti(email, id);
    }

    @DeleteMapping("/{id}/like")
    public void unlikeConti(@AuthenticationPrincipal String email, @PathVariable Long id) {
        contiService.unlikeConti(email, id);
    }
}
