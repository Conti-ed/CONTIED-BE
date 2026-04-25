package com.contied.conti.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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

    @NotNull
    @Size(min = 1, max = 10, message = "키워드는 1개 이상 10개 이하여야 합니다.")
    private List<@NotBlank String> keywords;

    @Pattern(regexp = "^.+\\d+:\\d+(~.+\\d+:\\d+)?$|^$", message = "성경 범위 형식이 올바르지 않습니다.")
    private String bibleVerseRange;  // 빈 문자열도 허용

    private Integer seed;  // 다시 생성용 시드값 (Optional)
}
