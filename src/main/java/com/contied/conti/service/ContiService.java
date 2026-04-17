package com.contied.conti.service;

import com.contied.conti.dto.AiResponse;
import com.contied.conti.dto.ContiResponse;
import com.contied.conti.dto.PostContiByAiRequest;
import com.contied.conti.entity.ContiEntity;
import com.contied.conti.repository.ContiRepository;
import com.contied.conti.repository.LikeRepository;
import com.contied.song.entity.SongEntity;
import com.contied.song.entity.State;
import com.contied.song.repository.SongRepository;
import com.contied.user.entity.UserEntity;
import com.contied.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContiService {

    private final ContiRepository contiRepository;
    private final SongRepository songRepository;
    private final UserRepository userRepository;
    private final LikeRepository likeRepository;
    private final GenerativeAiService generativeAiService;
    private final YoutubeService youtubeService; // 추가된 유튜브 엔진

    public List<ContiResponse> getAllContis() {
        return contiRepository.findByStateOrderByCreatedAtDesc(State.ACTIVE).stream()
                .map(ContiResponse::from)
                .collect(Collectors.toList());
    }

    public ContiResponse getContiById(Long id) {
        return contiRepository.findById(id)
                .map(ContiResponse::from)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 콘티입니다. ID: " + id));
    }

    public com.contied.conti.dto.MyContiesResponse getMyContis(String email, Long cursor, int take) {
        String normalizedEmail = email.toLowerCase().trim();
        UserEntity user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다. (" + normalizedEmail + ")"));
        
        org.springframework.data.domain.Slice<ContiEntity> slice = contiRepository.findByUserAndStateWithCursor(
                user, State.ACTIVE, cursor, org.springframework.data.domain.PageRequest.of(0, take));

        java.util.List<ContiResponse> myContiData = slice.getContent().stream()
                .map(ContiResponse::from)
                .collect(Collectors.toList());

        Long nextCursor = null;
        if (slice.hasNext() && !myContiData.isEmpty()) {
            nextCursor = myContiData.get(myContiData.size() - 1).getId();
        }

        return new com.contied.conti.dto.MyContiesResponse(myContiData, nextCursor);
    }

    @Transactional
    public ContiResponse createContiByAi(String email, PostContiByAiRequest request) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        AiResponse aiResponse = generativeAiService.generateContiByAi(request);
        
        List<Long> songIds = aiResponse.getSongs().stream()
                .map(AiResponse.AiSong::getId)
                .collect(Collectors.toList());
        
        List<SongEntity> songs = songRepository.findAllById(songIds);

        ContiEntity conti = ContiEntity.builder()
                .title(aiResponse.getTitle())
                .description(aiResponse.getDescription())
                .user(user)
                .songs(songs)
                .state(State.ACTIVE)
                .build();

        conti.updateTotalDuration();
        return ContiResponse.from(contiRepository.save(conti));
    }

    @Transactional
    public ContiResponse createConti(String email, com.contied.conti.dto.PostContiByCreationRequest request) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        List<SongEntity> songs = java.util.Collections.emptyList();
        if (request.getSongs() != null && !request.getSongs().isEmpty()) {
            songs = songRepository.findAllById(request.getSongs());
        }

        ContiEntity conti = ContiEntity.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .user(user)
                .songs(songs)
                .state(State.ACTIVE)
                .build();

        conti.updateTotalDuration();
        return ContiResponse.from(contiRepository.save(conti));
    }

    public ContiResponse getMyContiById(String email, Long id) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        ContiEntity conti = contiRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 콘티입니다. ID: " + id));

        if (!conti.getUser().getId().equals(user.getId())) {
            throw new IllegalStateException("해당 콘티에 접근할 권한이 없습니다.");
        }

        return ContiResponse.from(conti);
    }

    @Transactional
    public ContiResponse updateContiById(String email, com.contied.conti.dto.PatchContiDto dto, Long id) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        ContiEntity conti = contiRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 콘티입니다."));

        if (!conti.getUser().getId().equals(user.getId())) {
            throw new IllegalStateException("해당 콘티를 수정할 권한이 없습니다.");
        }

        if (dto.getTitle() != null && !dto.getTitle().isEmpty()) {
            conti.setTitle(dto.getTitle());
        }
        if (dto.getDescription() != null) {
            conti.setDescription(dto.getDescription());
        }

        // 노래 목록 동기화: 기존 목록과 비교해 delta(제거/추가)만 적용 (불필요한 DELETE+INSERT 방지)
        if (dto.getSongs() != null) {
            List<SongEntity> newSongs = songRepository.findAllById(dto.getSongs());
            Set<Long> newIds = newSongs.stream().map(SongEntity::getId).collect(Collectors.toSet());
            Set<Long> currentIds = conti.getSongs().stream().map(SongEntity::getId).collect(Collectors.toSet());

            // 제거 대상
            conti.getSongs().removeIf(song -> !newIds.contains(song.getId()));

            // 추가 대상 (요청 순서 유지)
            List<SongEntity> toAdd = new ArrayList<>();
            for (SongEntity s : newSongs) {
                if (!currentIds.contains(s.getId())) {
                    toAdd.add(s);
                }
            }
            if (!toAdd.isEmpty()) {
                conti.getSongs().addAll(toAdd);
            }

            conti.updateTotalDuration();
        }

        return ContiResponse.from(conti);
    }

    @Transactional
    public ContiResponse createContiByYoutube(String email, com.contied.conti.dto.PostContiByYoutubeRequest request) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        String youtubeUrl = request.getYoutubeUrl();
        String description = request.getDescription() != null ? request.getDescription() : "";
        
        // Jsoup Melon 크롤러 & YouTube API 실행
        List<YoutubeService.SongDetailDto> scrapedSongs = youtubeService.getSongsByPlaylist(youtubeUrl);
        
        List<SongEntity> songs = new java.util.ArrayList<>();
        String parsedThumbnail = null;
        
        for (YoutubeService.SongDetailDto dto : scrapedSongs) {
            // 첫 번째 영상의 썸네일을 콘티 대표 썸네일로 설정
            if (parsedThumbnail == null && dto.thumbnail != null) {
                parsedThumbnail = dto.thumbnail;
            }
            
            // 기존 등록된 곡인지 확인 (videoId 유니크 제약조건 회피)
            SongEntity existingSong = songRepository.findByVideoId(dto.videoId).orElse(null);
            
            if (existingSong != null) {
                songs.add(existingSong);
            } else {
                SongEntity newSong = SongEntity.builder()
                        .title(dto.title)
                        .artist(dto.artist)
                        .lyrics(dto.lyrics)
                        .videoId(dto.videoId)
                        .thumbnail(dto.thumbnail)
                        .duration(dto.duration)
                        .state(com.contied.song.entity.State.ACTIVE)
                        .build();
                songs.add(songRepository.save(newSong));
            }
        }
        
        String finalTitle = (request.getTitle() != null && !request.getTitle().isEmpty()) ? request.getTitle() : "YouTube Playlist";
        
        // 프론트엔드에서 제목이 비어있었다면 유튜브 실제 재생목록 제목 채우기
        if (finalTitle.equals("YouTube Playlist")) {
            String actualTitle = youtubeService.getPlaylistTitle(youtubeUrl);
            if (actualTitle != null) {
                finalTitle = actualTitle;
            }
        }

        ContiEntity conti = ContiEntity.builder()
                .title(finalTitle)
                .thumbnail(parsedThumbnail) 
                .description(description)
                .youtubeUrl(youtubeUrl)
                .user(user)
                .songs(songs) // 새롭게 긁어온 노래 리스트 무장!
                .state(State.ACTIVE)
                .build();

        conti.updateTotalDuration();
        return ContiResponse.from(contiRepository.save(conti));
    }

    @Transactional
    public void deleteConti(String email, Long id) {
        ContiEntity conti = contiRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 콘티입니다."));
        
        if (!conti.getUser().getEmail().equals(email)) {
            throw new IllegalStateException("해당 콘티를 삭제할 권한이 없습니다.");
        }

        conti.setState(State.DELETED);
    }

    @Transactional
    public void likeConti(String email, Long id) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
        ContiEntity conti = contiRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 콘티입니다."));

        if (likeRepository.existsByUserAndConti(user, conti)) {
            return;
        }

        try {
            likeRepository.save(com.contied.conti.entity.LikeEntity.builder()
                    .user(user)
                    .conti(conti)
                    .build());
        } catch (DataIntegrityViolationException e) {
            // 동시에 같은 (user, conti) 좋아요가 들어온 경우 unique constraint 가 막아줌 → 무시
        }
    }

    @Transactional
    public void unlikeConti(String email, Long id) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
        ContiEntity conti = contiRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 콘티입니다."));

        likeRepository.deleteByUserAndConti(user, conti);
    }

    public List<ContiResponse> getLikedContis(String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        return likeRepository.findLikedContisByUser(user).stream()
                .map(ContiResponse::from)
                .collect(Collectors.toList());
    }
}
