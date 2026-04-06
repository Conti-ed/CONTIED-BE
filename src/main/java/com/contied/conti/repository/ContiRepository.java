package com.contied.conti.repository;

import com.contied.conti.entity.ContiEntity;
import com.contied.song.entity.State;
import com.contied.user.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContiRepository extends JpaRepository<ContiEntity, Long> {
    Page<ContiEntity> findByStateOrderByCreatedAtDesc(State state, Pageable pageable);
    List<ContiEntity> findByUserAndState(UserEntity user, State state);
}
