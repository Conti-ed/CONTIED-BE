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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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

    public Page<ContiResponse> getAllContis(Pageable pageable) {
        return contiRepository.findByStateOrderByCreatedAtDesc(State.ACTIVE, pageable)
                .map(ContiResponse::from);
    }

    public ContiResponse getContiById(Long id) {
        return contiRepository.findById(id)
                .map(ContiResponse::from)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 콘티입니다. ID: " + id));
    }

    public List<ContiResponse> getMyContis(String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
        
        return contiRepository.findByUserAndState(user, State.ACTIVE).stream()
                .map(ContiResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public ContiResponse createContiByAi(String email, PostContiByAiRequest request) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        AiResponse aiResponse = generativeAiService.generateContiByAi(request);
        
        List<Long> songIds = aiResponse.getData().getSongs().stream()
                .map(AiResponse.AiSong::getId)
                .collect(Collectors.toList());
        
        List<SongEntity> songs = songRepository.findAllById(songIds);

        ContiEntity conti = ContiEntity.builder()
                .title(aiResponse.getData().getTitle())
                .description(aiResponse.getData().getDescription())
                .user(user)
                .songs(songs)
                .state(State.ACTIVE)
                .build();

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

        if (likeRepository.findByUserAndConti(user, conti).isEmpty()) {
            likeRepository.save(com.contied.conti.entity.LikeEntity.builder()
                    .user(user)
                    .conti(conti)
                    .build());
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
}
