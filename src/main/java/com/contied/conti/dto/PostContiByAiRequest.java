package com.contied.conti.dto;

import lombok.*;

import java.util.List;

/**
 * 프론트엔드로부터 AI 콘티 생성 요청을 받는 DTO.
 * 프론트엔드는 camelCase (bibleVerseRange)로 보냅니다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostContiByAiRequest {
    private List<String> keywords;
    private String bibleVerseRange;
}
