package com.contied.search.controller;

import com.contied.search.dto.SearchHistoryResponse;
import com.contied.search.service.SearchHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/search/recent")
@RequiredArgsConstructor
public class SearchHistoryController {

    private final SearchHistoryService searchHistoryService;

    @GetMapping
    public List<SearchHistoryResponse> getRecentSearches(@AuthenticationPrincipal String email) {
        return searchHistoryService.getRecentSearches(email);
    }

    @PostMapping
    public void saveSearch(@AuthenticationPrincipal String email, @RequestParam String query) {
        searchHistoryService.saveSearch(email, query);
    }

    @DeleteMapping("/{id}")
    public void deleteSearch(@AuthenticationPrincipal String email, @PathVariable Long id) {
        searchHistoryService.deleteSearch(email, id);
    }

    @DeleteMapping("/all")
    public void clearAll(@AuthenticationPrincipal String email) {
        searchHistoryService.clearAll(email);
    }
}
