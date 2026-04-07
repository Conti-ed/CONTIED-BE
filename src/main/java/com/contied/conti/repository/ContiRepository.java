package com.contied.conti.repository;

import com.contied.conti.entity.ContiEntity;
import com.contied.song.entity.State;
import com.contied.user.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContiRepository extends JpaRepository<ContiEntity, Long> {
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"user"})
    Page<ContiEntity> findByStateOrderByCreatedAtDesc(State state, Pageable pageable);
    
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"user"})
    List<ContiEntity> findByStateOrderByCreatedAtDesc(State state);
    
    @Query("SELECT c FROM ContiEntity c WHERE c.user = :user AND c.state = :state AND (:cursor = 0L OR c.id < :cursor) ORDER BY c.id DESC")
    Slice<ContiEntity> findByUserAndStateWithCursor(@Param("user") UserEntity user, @Param("state") State state, @Param("cursor") Long cursor, Pageable pageable);
}
