package com.contied.conti.entity;

import com.contied.global.entity.BaseEntity;
import com.contied.song.entity.SongEntity;
import com.contied.song.entity.State;
import com.contied.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "\"Conti\"")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContiEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private State state = State.ACTIVE;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 1024)
    private String thumbnail;

    @Builder.Default
    private Integer duration = 0;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "youtube_url", length = 255)
    private String youtubeUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @Builder.Default
    @org.hibernate.annotations.BatchSize(size = 100)
    @ManyToMany
    @JoinTable(
            name = "\"ContiToSong\"",
            joinColumns = @JoinColumn(name = "conti_id"),
            inverseJoinColumns = @JoinColumn(name = "song_id")
    )
    private List<SongEntity> songs = new ArrayList<>();
}
