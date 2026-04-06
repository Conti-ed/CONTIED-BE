package com.contied.conti.dto;

import com.contied.conti.entity.ContiEntity;
import com.contied.song.dto.SongResponse;
import com.contied.song.entity.State;
import lombok.*;

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
    private List<SongResponse> songs;

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
                .songs(entity.getSongs().stream()
                        .map(SongResponse::from)
                        .collect(Collectors.toList()))
                .build();
    }
}
