package com.contied.song.service;

import com.contied.song.dto.SongResponse;
import com.contied.song.entity.State;
import com.contied.song.repository.SongRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SongService {

    private final SongRepository songRepository;

    public List<SongResponse> searchSongs(String title) {
        if (title == null || title.isBlank()) {
            return songRepository.findByState(State.ACTIVE).stream()
                    .map(SongResponse::from)
                    .collect(Collectors.toList());
        }
        
        return songRepository.findByTitleContainingAndState(title, State.ACTIVE).stream()
                .map(SongResponse::from)
                .collect(Collectors.toList());
    }

    public SongResponse getSongById(Long id) {
        return songRepository.findById(id)
                .map(SongResponse::from)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 찬양입니다. ID: " + id));
    }
}
