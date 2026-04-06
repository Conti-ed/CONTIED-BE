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
