package com.contied.song.service;

import com.contied.song.dto.SongResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class YoutubeService {

    private final WebClient.Builder webClientBuilder;

    /**
     * 유튜브 플레이리스트 URL에서 곡 정보를 가져옵니다.
     * 실제 운영시에는 YouTube Data API v3 키가 필요합니다.
     */
    public List<SongResponse> getSongsFromPlaylist(String playlistUrl) {
        // TODO: YouTube Data API v3 연동 로직 구현
        // 현재는 마이그레이션을 위해 빈 리스트 또는 목업 데이터를 반환 구조만 잡습니다.
        List<SongResponse> songs = new ArrayList<>();
        
        // 예시 구조:
        // String playlistId = extractPlaylistId(playlistUrl);
        // JsonNode response = webClientBuilder.build().get()
        //    .uri("https://www.googleapis.com/youtube/v3/playlistItems?part=snippet&playlistId=" + playlistId + "&key=" + apiKey)
        //    .retrieve().bodyToMono(JsonNode.class).block();
        
        return songs;
    }

    private String extractPlaylistId(String url) {
        if (url.contains("list=")) {
            return url.split("list=")[1].split("&")[0];
        }
        return url;
    }
}
