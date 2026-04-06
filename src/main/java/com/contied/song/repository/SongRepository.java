package com.contied.song.repository;

import com.contied.song.entity.SongEntity;
import com.contied.song.entity.State;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SongRepository extends JpaRepository<SongEntity, Long> {
    List<SongEntity> findByState(State state);
    Optional<SongEntity> findByVideoId(String videoId);
    List<SongEntity> findByTitleContainingAndState(String title, State state);
}
