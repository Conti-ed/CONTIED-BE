package com.contied.conti.controller;

import com.contied.conti.dto.ContiResponse;
import com.contied.conti.dto.PostContiByAiRequest;
import com.contied.conti.dto.PostContiByCreationRequest;
import com.contied.conti.dto.PostContiByYoutubeRequest;
import com.contied.conti.service.ContiService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/conti")
@RequiredArgsConstructor
public class ContiController {

    private final ContiService contiService;

    @GetMapping
    public List<ContiResponse> getAllContis() {
        return contiService.getAllContis();
    }

    @GetMapping("/{id}")
    public ContiResponse getConti(@PathVariable Long id) {
        return contiService.getContiById(id);
    }

    @GetMapping("/myconti")
    public com.contied.conti.dto.MyContiesResponse getMyContis(
            @AuthenticationPrincipal String email,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") Long cursor,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "100") int take) {
        return contiService.getMyContis(email, cursor, take);
    }

    @PostMapping("/myconti/custom")
    public ContiResponse createContiByCreation(
            @AuthenticationPrincipal String email, 
            @RequestBody PostContiByCreationRequest request) {
        return contiService.createConti(email, request);
    }

    @PostMapping("/myconti/ai")
    public ContiResponse createContiByAi(
            @AuthenticationPrincipal String email, 
            @RequestBody PostContiByAiRequest request) {
        return contiService.createContiByAi(email, request);
    }

    @PostMapping("/myconti/youtube")
    public ContiResponse createContiByYoutube(
            @AuthenticationPrincipal String email, 
            @RequestBody PostContiByYoutubeRequest request) {
        return contiService.createContiByYoutube(email, request);
    }

    @GetMapping("/myconti/{id}")
    public ContiResponse getMyContiById(@AuthenticationPrincipal String email, @PathVariable Long id) {
        return contiService.getMyContiById(email, id);
    }

    @PatchMapping("/myconti/{id}")
    public ContiResponse updateContiById(
            @AuthenticationPrincipal String email, 
            @RequestBody com.contied.conti.dto.PatchContiDto request, 
            @PathVariable Long id) {
        return contiService.updateContiById(email, request, id);
    }

    @DeleteMapping("/myconti/{id}")
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

    @GetMapping("/like")
    public List<ContiResponse> getLikedContis(@AuthenticationPrincipal String email) {
        return contiService.getLikedContis(email);
    }
}
