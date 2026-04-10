package com.contied.search.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchHistoryResponse {
    private Long id;
    private String query;
    private LocalDateTime updatedAt;
}
