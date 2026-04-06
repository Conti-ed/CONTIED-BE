package com.contied.song.dto;

import com.contied.song.entity.SongEntity;
import com.contied.song.entity.State;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SongResponse {
    private Long id;
    private State state;
    private String videoId;
    private Integer tempo;
    private String keyScale;
    private BigDecimal danceability;
    private String title;
    private String artist;
    private String thumbnail;
    private LocalDateTime releaseDate;
    private String lyrics;
    private Integer duration;

    public static SongResponse from(SongEntity entity) {
        return SongResponse.builder()
                .id(entity.getId())
                .state(entity.getState())
                .videoId(entity.getVideoId())
                .tempo(entity.getTempo())
                .keyScale(entity.getKeyScale())
                .danceability(entity.getDanceability())
                .title(entity.getTitle())
                .artist(entity.getArtist())
                .thumbnail(entity.getThumbnail())
                .releaseDate(entity.getReleaseDate())
                .lyrics(entity.getLyrics())
                .duration(entity.getDuration())
                .build();
    }
}
