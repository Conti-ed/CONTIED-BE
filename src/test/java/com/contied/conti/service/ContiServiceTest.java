package com.contied.conti.service;

import com.contied.conti.dto.AiResponse;
import com.contied.conti.dto.ContiResponse;
import com.contied.conti.dto.PostContiByAiRequest;
import com.contied.conti.entity.ContiEntity;
import com.contied.conti.repository.ContiRepository;
import com.contied.conti.repository.LikeRepository;
import com.contied.global.exception.AiMappingException;
import com.contied.song.entity.SongEntity;
import com.contied.song.entity.State;
import com.contied.song.repository.SongRepository;
import com.contied.user.entity.UserEntity;
import com.contied.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContiServiceTest {

    @Mock
    private ContiRepository contiRepository;

    @Mock
    private SongRepository songRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LikeRepository likeRepository;

    @Mock
    private GenerativeAiService generativeAiService;

    @Mock
    private YoutubeService youtubeService;

    @InjectMocks
    private ContiService contiService;

    private UserEntity testUser;
    private PostContiByAiRequest testRequest;

    @BeforeEach
    void setUp() {
        testUser = UserEntity.builder()
                .id(1L)
                .email("test@example.com")
                .nickname("테스터")
                .build();

        testRequest = new PostContiByAiRequest();
    }

    /** 헬퍼: SongEntity 생성 */
    private SongEntity song(Long id, String videoId, String title, String artist) {
        return SongEntity.builder()
                .id(id)
                .videoId(videoId)
                .title(title)
                .artist(artist)
                .state(State.ACTIVE)
                .build();
    }

    /** 헬퍼: AiResponse.AiSong 생성 */
    private AiResponse.AiSong aiSong(Long id, String videoId, String title, String artist) {
        return AiResponse.AiSong.builder()
                .id(id)
                .videoId(videoId)
                .title(title)
                .artist(artist)
                .build();
    }

    /** 헬퍼: 저장된 ContiEntity 모킹 */
    private ContiEntity savedConti(String title, UserEntity user, List<SongEntity> songs) {
        ContiEntity conti = ContiEntity.builder()
                .id(100L)
                .title(title)
                .user(user)
                .songs(songs)
                .state(State.ACTIVE)
                .build();
        return conti;
    }

    // -------------------------------------------------------------------------
    // 테스트 1: videoId 매핑 성공 — 5곡 모두 매핑
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("videoId 매핑 성공: AiResponse의 모든 곡이 DB에 존재하면 5곡 모두 매핑")
    void createContiByAi_videoIdMapping_allFiveSongsFound() {
        // given
        List<AiResponse.AiSong> aiSongs = List.of(
                aiSong(1L, "vid1", "찬양1", "아티스트1"),
                aiSong(2L, "vid2", "찬양2", "아티스트2"),
                aiSong(3L, "vid3", "찬양3", "아티스트3"),
                aiSong(4L, "vid4", "찬양4", "아티스트4"),
                aiSong(5L, "vid5", "찬양5", "아티스트5")
        );
        AiResponse aiResponse = AiResponse.builder()
                .title("5곡 콘티")
                .description("테스트 콘티")
                .songs(aiSongs)
                .build();

        List<SongEntity> dbSongs = List.of(
                song(1L, "vid1", "찬양1", "아티스트1"),
                song(2L, "vid2", "찬양2", "아티스트2"),
                song(3L, "vid3", "찬양3", "아티스트3"),
                song(4L, "vid4", "찬양4", "아티스트4"),
                song(5L, "vid5", "찬양5", "아티스트5")
        );

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(generativeAiService.generateContiByAi(any())).thenReturn(Mono.just(aiResponse));
        when(songRepository.findByVideoId("vid1")).thenReturn(Optional.of(dbSongs.get(0)));
        when(songRepository.findByVideoId("vid2")).thenReturn(Optional.of(dbSongs.get(1)));
        when(songRepository.findByVideoId("vid3")).thenReturn(Optional.of(dbSongs.get(2)));
        when(songRepository.findByVideoId("vid4")).thenReturn(Optional.of(dbSongs.get(3)));
        when(songRepository.findByVideoId("vid5")).thenReturn(Optional.of(dbSongs.get(4)));

        ContiEntity saved = savedConti("5곡 콘티", testUser, dbSongs);
        when(contiRepository.save(any(ContiEntity.class))).thenReturn(saved);

        // when
        ContiResponse result = contiService.createContiByAi("test@example.com", testRequest);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getSongs()).hasSize(5);
        assertThat(result.getTitle()).isEqualTo("5곡 콘티");
    }

    // -------------------------------------------------------------------------
    // 테스트 2: videoId 없으면 id로 폴백
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("videoId 없으면 id로 폴백")
    void createContiByAi_fallbackToId_whenVideoIdIsNull() {
        // given: videoId 없고 id만 있는 5곡
        List<AiResponse.AiSong> aiSongs = List.of(
                aiSong(10L, null, "곡A", "가수A"),
                aiSong(11L, null, "곡B", "가수B"),
                aiSong(12L, null, "곡C", "가수C"),
                aiSong(13L, null, "곡D", "가수D"),
                aiSong(14L, null, "곡E", "가수E")
        );
        AiResponse aiResponse = AiResponse.builder()
                .title("id폴백 콘티")
                .description("")
                .songs(aiSongs)
                .build();

        List<SongEntity> dbSongs = List.of(
                song(10L, null, "곡A", "가수A"),
                song(11L, null, "곡B", "가수B"),
                song(12L, null, "곡C", "가수C"),
                song(13L, null, "곡D", "가수D"),
                song(14L, null, "곡E", "가수E")
        );

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(generativeAiService.generateContiByAi(any())).thenReturn(Mono.just(aiResponse));
        when(songRepository.findById(10L)).thenReturn(Optional.of(dbSongs.get(0)));
        when(songRepository.findById(11L)).thenReturn(Optional.of(dbSongs.get(1)));
        when(songRepository.findById(12L)).thenReturn(Optional.of(dbSongs.get(2)));
        when(songRepository.findById(13L)).thenReturn(Optional.of(dbSongs.get(3)));
        when(songRepository.findById(14L)).thenReturn(Optional.of(dbSongs.get(4)));

        ContiEntity saved = savedConti("id폴백 콘티", testUser, dbSongs);
        when(contiRepository.save(any(ContiEntity.class))).thenReturn(saved);

        // when
        ContiResponse result = contiService.createContiByAi("test@example.com", testRequest);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getSongs()).hasSize(5);
    }

    // -------------------------------------------------------------------------
    // 테스트 3: videoId/id 모두 실패 시 (title, artist) 폴백
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("videoId/id 모두 실패 시 (title, artist) 폴백")
    void createContiByAi_fallbackToTitleArtist_whenVideoIdAndIdBothFail() {
        // given: videoId 있지만 DB에 없고, id 도 없음 → title+artist 폴백
        List<AiResponse.AiSong> aiSongs = List.of(
                aiSong(null, "noVid1", "곡A", "가수A"),
                aiSong(null, "noVid2", "곡B", "가수B"),
                aiSong(null, "noVid3", "곡C", "가수C"),
                aiSong(null, "noVid4", "곡D", "가수D"),
                aiSong(null, "noVid5", "곡E", "가수E")
        );
        AiResponse aiResponse = AiResponse.builder()
                .title("titleArtist폴백 콘티")
                .description("")
                .songs(aiSongs)
                .build();

        SongEntity sa = song(20L, null, "곡A", "가수A");
        SongEntity sb = song(21L, null, "곡B", "가수B");
        SongEntity sc = song(22L, null, "곡C", "가수C");
        SongEntity sd = song(23L, null, "곡D", "가수D");
        SongEntity se = song(24L, null, "곡E", "가수E");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(generativeAiService.generateContiByAi(any())).thenReturn(Mono.just(aiResponse));

        // videoId로는 못 찾음
        when(songRepository.findByVideoId("noVid1")).thenReturn(Optional.empty());
        when(songRepository.findByVideoId("noVid2")).thenReturn(Optional.empty());
        when(songRepository.findByVideoId("noVid3")).thenReturn(Optional.empty());
        when(songRepository.findByVideoId("noVid4")).thenReturn(Optional.empty());
        when(songRepository.findByVideoId("noVid5")).thenReturn(Optional.empty());

        // id 는 null이므로 findById 호출 안 됨 → title+artist 폴백
        when(songRepository.findByTitleAndArtistAndState("곡A", "가수A", State.ACTIVE)).thenReturn(Optional.of(sa));
        when(songRepository.findByTitleAndArtistAndState("곡B", "가수B", State.ACTIVE)).thenReturn(Optional.of(sb));
        when(songRepository.findByTitleAndArtistAndState("곡C", "가수C", State.ACTIVE)).thenReturn(Optional.of(sc));
        when(songRepository.findByTitleAndArtistAndState("곡D", "가수D", State.ACTIVE)).thenReturn(Optional.of(sd));
        when(songRepository.findByTitleAndArtistAndState("곡E", "가수E", State.ACTIVE)).thenReturn(Optional.of(se));

        ContiEntity saved = savedConti("titleArtist폴백 콘티", testUser, List.of(sa, sb, sc, sd, se));
        when(contiRepository.save(any(ContiEntity.class))).thenReturn(saved);

        // when
        ContiResponse result = contiService.createContiByAi("test@example.com", testRequest);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getSongs()).hasSize(5);
    }

    // -------------------------------------------------------------------------
    // 테스트 4: 매핑 성공 곡이 3곡 미만이면 AiMappingException 발생
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("매핑 성공 곡이 3곡 미만이면 AiMappingException 발생")
    void createContiByAi_throwsAiMappingException_whenMappedSongsLessThanThree() {
        // given: 5곡 요청하지만 2곡만 매핑
        List<AiResponse.AiSong> aiSongs = List.of(
                aiSong(1L, "vid1", "곡1", "가수1"),
                aiSong(2L, "vid2", "곡2", "가수2"),
                aiSong(null, "unmapped1", "없는곡A", "없는가수A"),
                aiSong(null, "unmapped2", "없는곡B", "없는가수B"),
                aiSong(null, "unmapped3", "없는곡C", "없는가수C")
        );
        AiResponse aiResponse = AiResponse.builder()
                .title("매핑부족 콘티")
                .description("")
                .songs(aiSongs)
                .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(generativeAiService.generateContiByAi(any())).thenReturn(Mono.just(aiResponse));

        when(songRepository.findByVideoId("vid1")).thenReturn(Optional.of(song(1L, "vid1", "곡1", "가수1")));
        when(songRepository.findByVideoId("vid2")).thenReturn(Optional.of(song(2L, "vid2", "곡2", "가수2")));
        when(songRepository.findByVideoId("unmapped1")).thenReturn(Optional.empty());
        when(songRepository.findByVideoId("unmapped2")).thenReturn(Optional.empty());
        when(songRepository.findByVideoId("unmapped3")).thenReturn(Optional.empty());

        // id 없으므로 findById 미호출; title/artist 폴백도 실패
        when(songRepository.findByTitleAndArtistAndState(eq("없는곡A"), eq("없는가수A"), eq(State.ACTIVE)))
                .thenReturn(Optional.empty());
        when(songRepository.findByTitleAndArtistAndState(eq("없는곡B"), eq("없는가수B"), eq(State.ACTIVE)))
                .thenReturn(Optional.empty());
        when(songRepository.findByTitleAndArtistAndState(eq("없는곡C"), eq("없는가수C"), eq(State.ACTIVE)))
                .thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> contiService.createContiByAi("test@example.com", testRequest))
                .isInstanceOf(AiMappingException.class)
                .hasMessageContaining("매핑된 곡 수: 2곡 (최소 3곡 필요)");
    }

    // -------------------------------------------------------------------------
    // 테스트 5: 정상 매핑 시 ContiResponse user/title/songs 채워져 있음
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("정상 매핑 시 ContiResponse가 반환되며 user/title/songs 채워져 있음")
    void createContiByAi_returnFullContiResponse_whenMappingSucceeds() {
        // given
        List<AiResponse.AiSong> aiSongs = List.of(
                aiSong(1L, "v1", "주찬양", "아티스트A"),
                aiSong(2L, "v2", "할렐루야", "아티스트B"),
                aiSong(3L, "v3", "감사해", "아티스트C")
        );
        AiResponse aiResponse = AiResponse.builder()
                .title("정상콘티")
                .description("테스트 설명")
                .songs(aiSongs)
                .build();

        SongEntity s1 = song(1L, "v1", "주찬양", "아티스트A");
        SongEntity s2 = song(2L, "v2", "할렐루야", "아티스트B");
        SongEntity s3 = song(3L, "v3", "감사해", "아티스트C");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(generativeAiService.generateContiByAi(any())).thenReturn(Mono.just(aiResponse));
        when(songRepository.findByVideoId("v1")).thenReturn(Optional.of(s1));
        when(songRepository.findByVideoId("v2")).thenReturn(Optional.of(s2));
        when(songRepository.findByVideoId("v3")).thenReturn(Optional.of(s3));

        ContiEntity saved = savedConti("정상콘티", testUser, List.of(s1, s2, s3));
        when(contiRepository.save(any(ContiEntity.class))).thenReturn(saved);

        // when
        ContiResponse result = contiService.createContiByAi("test@example.com", testRequest);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("정상콘티");
        assertThat(result.getSongs()).hasSize(3);
        assertThat(result.getUserInfo()).isNotNull();
        assertThat(result.getUserInfo().getEmail()).isEqualTo("test@example.com");
        assertThat(result.getUserInfo().getNickname()).isEqualTo("테스터");
    }
}
