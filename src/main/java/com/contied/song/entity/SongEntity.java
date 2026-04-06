package com.contied.song.entity;

import com.contied.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "\"Song\"")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SongEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private State state = State.ACTIVE;

    @Column(name = "video_id", unique = true, length = 30)
    private String videoId;

    @Builder.Default
    private Integer tempo = 0;

    @Column(name = "key_scale", length = 30)
    private String keyScale;

    @Builder.Default
    private BigDecimal danceability = BigDecimal.ZERO;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, length = 255)
    private String artist;

    @Column(length = 1024)
    private String thumbnail;

    @Column(name = "release_date")
    private LocalDateTime releaseDate;

    @Column(columnDefinition = "TEXT")
    private String lyrics;

    @Builder.Default
    private Integer duration = 0;
}
