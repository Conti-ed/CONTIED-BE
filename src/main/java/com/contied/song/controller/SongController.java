package com.contied.song.controller;

import com.contied.song.dto.SongResponse;
import com.contied.song.service.SongService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/song")
@RequiredArgsConstructor
public class SongController {

    private final SongService songService;

    @GetMapping
    public List<SongResponse> getAllSongs(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int cursor,
            @RequestParam(defaultValue = "500") int take) {
        return songService.searchSongs(keyword);
    }

    @GetMapping("/search")
    public List<SongResponse> searchSongs(
            @RequestParam(name = "q", required = false) String keyword,
            @RequestParam(name = "keyword", required = false) String keywordAlternative) {
        String finalKeyword = (keyword != null) ? keyword : keywordAlternative;
        return songService.searchSongs(finalKeyword);
    }

    @GetMapping("/{id}")
    public SongResponse getSong(@PathVariable Long id) {
        return songService.getSongById(id);
    }
}
