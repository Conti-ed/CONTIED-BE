package com.contied.conti.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service("contiYoutubeService")
public class YoutubeService {

    private final WebClient webClient;

    @Value("${YOUTUBE_API_KEY:}")
    private String youtubeApiKey;

    @Value("${YOUTUBE_API_URL:https://www.googleapis.com/youtube/v3/playlistItems}")
    private String youtubeApiUrl;

    public YoutubeService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public static class SongDetailDto {
        public String title;
        public String videoId;
        public String thumbnail;
        public String artist;
        public String lyrics;
    }

    public List<SongDetailDto> getSongsByPlaylist(String url) {
        String playlistId = extractPlaylistId(url);
        if (playlistId == null) {
            throw new IllegalArgumentException("유효하지 않은 유튜브 재생목록 URL입니다.");
        }

        List<Map<String, Object>> playlistItems = getPlaylistItems(playlistId, 50);
        List<SongDetailDto> songDetails = new ArrayList<>();

        for (Map<String, Object> item : playlistItems) {
            String title = (String) item.get("title");
            String channelTitle = (String) item.get("channelTitle");
            String[] artistAndLyrics = extractArtistAndLyricsFromMelon(title, channelTitle);

            SongDetailDto dto = new SongDetailDto();
            dto.title = title;
            dto.videoId = (String) item.get("videoId");
            dto.thumbnail = (String) item.get("thumbnail");
            dto.artist = artistAndLyrics[0];
            dto.lyrics = artistAndLyrics[1];

            songDetails.add(dto);
        }
        return songDetails;
    }

    private String[] extractArtistAndLyricsFromMelon(String title, String channelTitle) {
        String extractedTitle = Normalizer.normalize(getCleanTitle(title), Normalizer.Form.NFC);
        
        // 검색어 강화: 영상 제목 + 유튜브 채널명 (정확도 극강화)
        String searchKeyword = extractedTitle;
        if (channelTitle != null && !channelTitle.isEmpty()) {
            searchKeyword = extractedTitle + " " + channelTitle;
        }
        
        String detailUrl = getSongDetailUrlFromMelon(searchKeyword, extractedTitle);

        if (detailUrl == null) {
            // 방어 로직: 멜론에서 전혀 찾지 못한 경우, 엉뚱한 정보 대신 원본 채널명으로 예쁘게 덮어쓰기
            String fallbackArtist = (channelTitle != null && !channelTitle.isEmpty()) ? channelTitle : "아티스트 정보를 입력해주세요.";
            return new String[]{fallbackArtist, "가사 정보를 찾을 수 없습니다."};
        }

        try {
            Document doc = Jsoup.connect(detailUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .get();

            Element artistEl = doc.selectFirst(".artist_name");
            String artistName = artistEl != null ? artistEl.attr("title") : "아티스트 정보를 입력해주세요.";

            Element lyricsEl = doc.selectFirst("#d_video_summary");
            String lyrics = "가사 정보를 입력해주세요.";

            if (lyricsEl != null) {
                String html = lyricsEl.html();
                html = html.replaceAll("(?i)<br\\s*/?>", "\n")
                           .replaceAll("<!--.*?-->", "")
                           .replaceAll("<[^>]*>", "");
                lyrics = html.trim();
            }

            return new String[]{artistName, lyrics};

        } catch (Exception e) {
            String fallbackArtist = (channelTitle != null && !channelTitle.isEmpty()) ? channelTitle : "아티스트 정보를 입력해주세요.";
            return new String[]{fallbackArtist, "가사 정보를 찾을 수 없습니다."};
        }
    }

    private String getSongDetailUrlFromMelon(String searchKeyword, String extractedTitle) {
        try {
            String encodedSearchQuery = URLEncoder.encode(searchKeyword, StandardCharsets.UTF_8.toString());
            String searchUrl = "https://www.melon.com/search/total/index.htm?q=" + encodedSearchQuery 
                    + "&section=&searchGnbYn=Y&kkoSpl=Y&kkoDpType=&mwkLogType=T";

            Document doc = Jsoup.connect(searchUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .get();

            Elements elements = doc.select(".fc_gray");
            for (Element element : elements) {
                String href = element.attr("href");
                String escapedTitle = Pattern.quote(extractedTitle);
                Matcher matcher = Pattern.compile(escapedTitle + "',\\s*'(\\d+)'").matcher(href);

                if (matcher.find()) {
                    String id = matcher.group(1).replaceAll("[,\\)';me]", "");
                    return "https://www.melon.com/song/detail.htm?songID=" + id;
                }
            }
            
            // 엄격한 일치가 안 될 경우, 검색어가 이미 채널명으로 고도화되었으므로 첫번째 검색 곡을 그대로 수용 (정확도 확보)
            if (!elements.isEmpty()) {
                for (Element element : elements) {
                    String href = element.attr("href");
                    Matcher fallbackMatcher = Pattern.compile("'(\\d{5,})'").matcher(href);
                    if (fallbackMatcher.find()) {
                        return "https://www.melon.com/song/detail.htm?songID=" + fallbackMatcher.group(1);
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("Melon crawling failed for: " + searchKeyword);
        }
        return null;
    }

    public String getPlaylistTitle(String url) {
        String playlistId = extractPlaylistId(url);
        if (playlistId == null) return null;
        
        if (youtubeApiKey == null || youtubeApiKey.isEmpty()) {
            youtubeApiKey = System.getenv("YOUTUBE_API_KEY");
        }

        try {
            Map<?, ?> response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https").host("www.googleapis.com").path("/youtube/v3/playlists")
                            .queryParam("part", "snippet")
                            .queryParam("id", playlistId)
                            .queryParam("key", youtubeApiKey)
                            .build())
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            
            if (response != null && response.get("items") instanceof List) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");
                if (items != null && !items.isEmpty()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> snippet = (Map<String, Object>) items.get(0).get("snippet");
                    return (String) snippet.get("title");
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to fetch playlist title: " + e.getMessage());
        }
        return null;
    }

    private List<Map<String, Object>> getPlaylistItems(String playlistId, int maxResults) {
        if (youtubeApiKey == null || youtubeApiKey.isEmpty()) { 
            youtubeApiKey = System.getenv("YOUTUBE_API_KEY");
        }

        Map<?, ?> response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https").host("www.googleapis.com").path("/youtube/v3/playlistItems")
                        .queryParam("part", "snippet")
                        .queryParam("playlistId", playlistId)
                        .queryParam("maxResults", maxResults)
                        .queryParam("key", youtubeApiKey)
                        .build())
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        List<Map<String, Object>> resultList = new ArrayList<>();

        if (response != null && response.get("items") instanceof List) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");
            if (items != null) {
                for (Map<String, Object> item : items) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> snippet = (Map<String, Object>) item.get("snippet");
                    @SuppressWarnings("unchecked")
                    Map<String, Object> resourceId = (Map<String, Object>) snippet.get("resourceId");
                    @SuppressWarnings("unchecked")
                    Map<String, Object> thumbnails = (Map<String, Object>) snippet.get("thumbnails");
                    @SuppressWarnings("unchecked")
                    Map<String, Object> highThumb = thumbnails != null ? (Map<String, Object>) thumbnails.get("high") : null;
                    
                    String channelTitle = (String) snippet.get("videoOwnerChannelTitle");
                    if (channelTitle != null) {
                        // 유튜브 뮤직에서 자동으로 생성된 채널명 끝의 " - Topic" 글자 잘라내기
                        channelTitle = channelTitle.replaceAll("(?i)\\s*-\\s*Topic$", "").trim();
                    }

                    resultList.add(Map.of(
                            "title", snippet.get("title"),
                            "videoId", resourceId.get("videoId"),
                            "thumbnail", highThumb != null ? highThumb.get("url") : "",
                            "channelTitle", channelTitle != null ? channelTitle : ""
                    ));
                }
            }
        }
        return resultList;
    }

    private String extractPlaylistId(String url) {
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("유효하지 않은 유튜브 URL입니다.");
        }
        Pattern pattern = Pattern.compile("[?&]list=([a-zA-Z0-9_-]+)");
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String getCleanTitle(String title) {
        title = title.replaceAll("\\[.*?\\]", "").trim();
        title = title.replaceAll("\\(.*?\\)|\\)", "").trim();
        title = title.replaceAll("ㅣ.*", "").trim();
        title = title.replaceAll("^\\d+\\.\\s*", "").trim();
        if (title.contains(" - ")) {
            title = title.split(" - ")[1];
        }
        return title.split("\\|")[0].trim();
    }
}
