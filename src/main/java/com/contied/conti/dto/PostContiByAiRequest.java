package com.contied.conti.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostContiByAiRequest {
    private List<String> keywords;
    private String bibleVerseRange;
}
