package com.contied.conti.dto;

import com.contied.conti.entity.ContiEntity;
import com.contied.song.dto.SongResponse;
import com.contied.song.entity.State;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContiResponse {
    private Long id;
    private State state;
    private String title;
    private String thumbnail;
    private Integer duration;
    private String description;
    private String youtubeUrl;
    private String creatorNickname;

    @JsonProperty("User")
    private UserDto userInfo;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 프론트엔드가 의존하는 기존 Prisma 중개 테이블 구조 완벽 재현
    @JsonProperty("ContiToSong")
    private List<ContiToSongDto> ContiToSong;
    
    // 호환성을 위해 직관적인 배열도 유지
    private List<SongResponse> songs;

    @Getter
    @Builder
    public static class ContiToSongDto {
        private Long songId;
        private Long contiId;
        private SongResponse song;
    }

    @Getter
    @Builder
    public static class UserDto {
        private String nickname;
        private String email;
    }

    public static ContiResponse from(ContiEntity entity) {
        return ContiResponse.builder()
                .id(entity.getId())
                .state(entity.getState())
                .title(entity.getTitle())
                .thumbnail(entity.getThumbnail())
                .duration(entity.getDuration())
                .description(entity.getDescription())
                .youtubeUrl(entity.getYoutubeUrl())
                .creatorNickname(entity.getUser() != null ? entity.getUser().getNickname() : "Unknown")
                .userInfo(UserDto.builder()
                        .nickname(entity.getUser() != null ? entity.getUser().getNickname() : "Unknown")
                        .email(entity.getUser() != null ? entity.getUser().getEmail() : "")
                        .build())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .songs(entity.getSongs().stream()
                        .map(SongResponse::from)
                        .collect(Collectors.toList()))
                .ContiToSong(entity.getSongs().stream()
                        .map(song -> ContiToSongDto.builder()
                                .songId(song.getId())
                                .contiId(entity.getId())
                                .song(SongResponse.from(song))
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }
}
