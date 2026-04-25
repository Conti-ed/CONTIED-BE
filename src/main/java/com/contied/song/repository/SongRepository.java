package com.contied.song.repository;

import com.contied.song.entity.SongEntity;
import com.contied.song.entity.State;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SongRepository extends JpaRepository<SongEntity, Long> {
    List<SongEntity> findByState(State state);
    Optional<SongEntity> findByVideoId(String videoId);
    List<SongEntity> findByTitleContainingAndState(String title, State state);
    Optional<SongEntity> findByTitleAndArtistAndState(String title, String artist, State state);

    @Query("SELECT s FROM SongEntity s WHERE s.state = :state AND (:cursor = 0L OR s.id > :cursor) ORDER BY s.id ASC")
    Slice<SongEntity> findByStateWithCursor(@Param("state") State state, @Param("cursor") Long cursor, Pageable pageable);

    @Query("SELECT s FROM SongEntity s WHERE s.state = :state AND s.title LIKE %:title% AND (:cursor = 0L OR s.id > :cursor) ORDER BY s.id ASC")
    Slice<SongEntity> findByTitleContainingWithCursor(@Param("title") String title, @Param("state") State state, @Param("cursor") Long cursor, Pageable pageable);
}
