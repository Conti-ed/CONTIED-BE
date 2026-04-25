package com.contied.song.service;

import com.contied.song.dto.SongResponse;
import com.contied.song.entity.State;
import com.contied.song.repository.SongRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SongService {

    private final SongRepository songRepository;

    public List<SongResponse> searchSongs(String title, Long cursor, int take) {
        int safeTake = Math.min(Math.max(take, 1), 200);   // 1~200 범위 클램프
        long safeCursor = cursor == null ? 0L : cursor;
        Pageable pageable = PageRequest.of(0, safeTake);

        Slice<com.contied.song.entity.SongEntity> slice;
        if (title == null || title.isBlank()) {
            slice = songRepository.findByStateWithCursor(State.ACTIVE, safeCursor, pageable);
        } else {
            slice = songRepository.findByTitleContainingWithCursor(title, State.ACTIVE, safeCursor, pageable);
        }

        return slice.getContent().stream()
                .map(SongResponse::from)
                .collect(Collectors.toList());
    }

    public SongResponse getSongById(Long id) {
        return songRepository.findById(id)
                .map(SongResponse::from)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 찬양입니다. ID: " + id));
    }
}
