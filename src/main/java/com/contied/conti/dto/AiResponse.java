package com.contied.conti.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiResponse {
    private String title;
    private String description;
    private List<AiSong> songs;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AiSong {
        private Long id;

        @JsonProperty("video_id")
        private String videoId;

        private String title;
        private String artist;
    }
}
