package com.contied.search.service;

import com.contied.search.dto.SearchHistoryResponse;
import com.contied.search.entity.SearchHistoryEntity;
import com.contied.search.repository.SearchHistoryRepository;
import com.contied.user.entity.UserEntity;
import com.contied.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchHistoryService {

    private final SearchHistoryRepository searchHistoryRepository;
    private final UserRepository userRepository;

    public List<SearchHistoryResponse> getRecentSearches(String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // 최근 20개 조회
        return searchHistoryRepository.findByUserOrderByUpdatedAtDesc(user, PageRequest.of(0, 20))
                .stream()
                .map(entity -> SearchHistoryResponse.builder()
                        .id(entity.getId())
                        .query(entity.getQuery())
                        .updatedAt(entity.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public void saveSearch(String email, String query) {
        if (query == null || query.trim().isEmpty()) return;

        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // 중복 체크: 이미 있으면 updatedAt 갱신
        searchHistoryRepository.findByUserAndQuery(user, query)
                .ifPresentOrElse(
                        entity -> {
                            entity.setUpdatedAt(LocalDateTime.now());
                        },
                        () -> {
                            SearchHistoryEntity newEntity = SearchHistoryEntity.builder()
                                    .user(user)
                                    .query(query)
                                    .build();
                            searchHistoryRepository.save(newEntity);
                        }
                );
    }

    @Transactional
    public void deleteSearch(String email, Long id) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        SearchHistoryEntity entity = searchHistoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Search history not found"));

        if (!entity.getUser().getId().equals(user.getId())) {
            throw new IllegalStateException("Unauthorized access to delete search history");
        }

        searchHistoryRepository.delete(entity);
    }

    @Transactional
    public void clearAll(String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        searchHistoryRepository.deleteByUser(user);
    }
}
