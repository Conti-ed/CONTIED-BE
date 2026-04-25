package com.contied.conti.service;

import lombok.extern.slf4j.Slf4j;
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

@Slf4j
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
        public int duration; // 초 단위
    }

    public List<SongDetailDto> getSongsByPlaylist(String url) {
        String playlistId = extractPlaylistId(url);
        if (playlistId == null) {
            throw new IllegalArgumentException("유효하지 않은 유튜브 재생목록 URL입니다.");
        }

        List<Map<String, Object>> playlistItems = getPlaylistItems(playlistId, 50);
        
        // videoId 목록 수집하여 Videos API로 duration 일괄 조회
        List<String> videoIds = new ArrayList<>();
        for (Map<String, Object> item : playlistItems) {
            videoIds.add((String) item.get("videoId"));
        }
        Map<String, Integer> durationMap = getVideoDurations(videoIds);
        
        return playlistItems.parallelStream().map(item -> {
            String title = (String) item.get("title");
            String channelTitle = (String) item.get("channelTitle");
            String videoId = (String) item.get("videoId");
            String thumbnail = (String) item.get("thumbnail");

            String[] artistAndLyrics = extractArtistAndLyricsFromMelon(title, channelTitle);

            SongDetailDto dto = new SongDetailDto();
            dto.title = title;
            dto.videoId = videoId;
            dto.thumbnail = thumbnail;
            dto.artist = artistAndLyrics[0];
            dto.lyrics = artistAndLyrics[1];
            dto.duration = durationMap.getOrDefault(videoId, 0);

            return dto;
        }).collect(java.util.stream.Collectors.toList());
    }

    /**
     * YouTube Data API Videos 리소스로 duration 일괄 조회
     */
    private Map<String, Integer> getVideoDurations(List<String> videoIds) {
        Map<String, Integer> durationMap = new java.util.HashMap<>();
        if (videoIds == null || videoIds.isEmpty()) return durationMap;

        if (youtubeApiKey == null || youtubeApiKey.isEmpty()) {
            youtubeApiKey = System.getenv("YOUTUBE_API_KEY");
        }

        // YouTube API는 최대 50개씩 요청 가능
        int batchSize = 50;
        for (int i = 0; i < videoIds.size(); i += batchSize) {
            List<String> batch = videoIds.subList(i, Math.min(i + batchSize, videoIds.size()));
            String ids = String.join(",", batch);

            try {
                Map<?, ?> response = webClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .scheme("https").host("www.googleapis.com").path("/youtube/v3/videos")
                                .queryParam("part", "contentDetails")
                                .queryParam("id", ids)
                                .queryParam("key", youtubeApiKey)
                                .build())
                        .retrieve()
                        .bodyToMono(Map.class)
                        .block();

                if (response != null && response.get("items") instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");
                    for (Map<String, Object> item : items) {
                        String videoId = (String) item.get("id");
                        @SuppressWarnings("unchecked")
                        Map<String, Object> contentDetails = (Map<String, Object>) item.get("contentDetails");
                        if (contentDetails != null) {
                            String isoDuration = (String) contentDetails.get("duration");
                            durationMap.put(videoId, parseIsoDuration(isoDuration));
                        }
                    }
                }
            } catch (Exception e) {
                log.error("동영상 재생시간 조회 실패: {}", e.getMessage(), e);
            }
        }
        return durationMap;
    }

    /**
     * ISO 8601 duration (예: PT4M13S, PT1H2M30S)을 초 단위로 파싱
     */
    private int parseIsoDuration(String isoDuration) {
        if (isoDuration == null || isoDuration.isEmpty()) return 0;
        try {
            Pattern pattern = Pattern.compile("PT(?:(\\d+)H)?(?:(\\d+)M)?(?:(\\d+)S)?");
            Matcher matcher = pattern.matcher(isoDuration);
            if (matcher.matches()) {
                int hours = matcher.group(1) != null ? Integer.parseInt(matcher.group(1)) : 0;
                int minutes = matcher.group(2) != null ? Integer.parseInt(matcher.group(2)) : 0;
                int seconds = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : 0;
                return hours * 3600 + minutes * 60 + seconds;
            }
        } catch (Exception e) {
            log.warn("ISO duration 파싱 실패: {}", isoDuration);
        }
        return 0;
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
            log.warn("멜론 크롤링 실패 (검색어: {}): {}", searchKeyword, e.getMessage());
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
            log.error("재생목록 제목 조회 실패: {}", e.getMessage(), e);
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
