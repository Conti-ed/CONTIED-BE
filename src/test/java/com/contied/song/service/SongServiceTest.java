package com.contied.song.service;

import com.contied.song.entity.SongEntity;
import com.contied.song.entity.State;
import com.contied.song.repository.SongRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SongServiceTest {

    @Mock
    private SongRepository songRepository;

    @InjectMocks
    private SongService songService;

    private Slice<SongEntity> emptySlice() {
        return new SliceImpl<>(Collections.emptyList());
    }

    // -------------------------------------------------------------------------
    // 테스트 1: take가 음수이면 1로 클램프
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("take가 음수이면 1로 클램프")
    void searchSongs_clampsTakeToOne_whenNegative() {
        // given
        when(songRepository.findByStateWithCursor(eq(State.ACTIVE), eq(0L), any(Pageable.class)))
                .thenReturn(emptySlice());

        // when
        songService.searchSongs(null, null, -5);

        // then: size=1 로 PageRequest가 만들어져야 한다
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(songRepository).findByStateWithCursor(eq(State.ACTIVE), eq(0L), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(1);
    }

    // -------------------------------------------------------------------------
    // 테스트 2: take가 0이면 1로 클램프
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("take가 0이면 1로 클램프")
    void searchSongs_clampsTakeToOne_whenZero() {
        when(songRepository.findByStateWithCursor(eq(State.ACTIVE), eq(0L), any(Pageable.class)))
                .thenReturn(emptySlice());

        songService.searchSongs(null, null, 0);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(songRepository).findByStateWithCursor(eq(State.ACTIVE), eq(0L), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(1);
    }

    // -------------------------------------------------------------------------
    // 테스트 3: take가 200 초과면 200으로 클램프
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("take가 200 초과면 200으로 클램프")
    void searchSongs_clampsTakeTo200_whenExceeds200() {
        when(songRepository.findByStateWithCursor(eq(State.ACTIVE), eq(0L), any(Pageable.class)))
                .thenReturn(emptySlice());

        songService.searchSongs(null, null, 9999);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(songRepository).findByStateWithCursor(eq(State.ACTIVE), eq(0L), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(200);
    }

    // -------------------------------------------------------------------------
    // 테스트 4: title이 null이면 findByStateWithCursor 호출
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("title이 null이면 findByStateWithCursor 호출")
    void searchSongs_callsFindByStateWithCursor_whenTitleIsNull() {
        when(songRepository.findByStateWithCursor(eq(State.ACTIVE), eq(0L), any(Pageable.class)))
                .thenReturn(emptySlice());

        songService.searchSongs(null, null, 10);

        verify(songRepository).findByStateWithCursor(eq(State.ACTIVE), eq(0L), any(Pageable.class));
    }

    // -------------------------------------------------------------------------
    // 테스트 5: title이 blank("")이면 findByStateWithCursor 호출
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("title이 blank이면 findByStateWithCursor 호출")
    void searchSongs_callsFindByStateWithCursor_whenTitleIsBlank() {
        when(songRepository.findByStateWithCursor(eq(State.ACTIVE), eq(0L), any(Pageable.class)))
                .thenReturn(emptySlice());

        songService.searchSongs("   ", null, 10);

        verify(songRepository).findByStateWithCursor(eq(State.ACTIVE), eq(0L), any(Pageable.class));
    }

    // -------------------------------------------------------------------------
    // 테스트 6: title이 있으면 findByTitleContainingWithCursor 호출
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("title이 있으면 findByTitleContainingWithCursor 호출")
    void searchSongs_callsFindByTitleContainingWithCursor_whenTitlePresent() {
        when(songRepository.findByTitleContainingWithCursor(eq("찬양"), eq(State.ACTIVE), eq(0L), any(Pageable.class)))
                .thenReturn(emptySlice());

        songService.searchSongs("찬양", null, 10);

        verify(songRepository).findByTitleContainingWithCursor(
                eq("찬양"), eq(State.ACTIVE), eq(0L), any(Pageable.class));
    }

    // -------------------------------------------------------------------------
    // 테스트 7: cursor가 null이면 0L 사용
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("cursor가 null이면 0L 사용")
    void searchSongs_usesZero_whenCursorIsNull() {
        when(songRepository.findByStateWithCursor(eq(State.ACTIVE), eq(0L), any(Pageable.class)))
                .thenReturn(emptySlice());

        songService.searchSongs(null, null, 10);

        verify(songRepository).findByStateWithCursor(eq(State.ACTIVE), eq(0L), any(Pageable.class));
    }

    // -------------------------------------------------------------------------
    // 테스트 8: cursor가 있으면 해당 값 사용
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("cursor가 있으면 해당 값 사용")
    void searchSongs_usesGivenCursor_whenCursorIsProvided() {
        when(songRepository.findByStateWithCursor(eq(State.ACTIVE), eq(50L), any(Pageable.class)))
                .thenReturn(emptySlice());

        songService.searchSongs(null, 50L, 10);

        verify(songRepository).findByStateWithCursor(eq(State.ACTIVE), eq(50L), any(Pageable.class));
    }
}
