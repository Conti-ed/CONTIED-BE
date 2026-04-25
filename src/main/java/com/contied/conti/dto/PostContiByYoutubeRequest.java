package com.contied.conti.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PostContiByYoutubeRequest {

    @NotBlank
    @Size(max = 255)
    private String title;

    @Size(max = 2000)
    private String description;

    @NotBlank
    @Pattern(regexp = "^https?://(www\\.)?youtube\\.com/.+|^https?://youtu\\.be/.+", message = "YouTube URL 형식이 아닙니다.")
    @JsonProperty("youtubeURL")
    private String youtubeUrl;
}
