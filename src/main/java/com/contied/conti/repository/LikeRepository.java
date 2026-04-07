package com.contied.conti.repository;

import com.contied.conti.entity.ContiEntity;
import com.contied.conti.entity.LikeEntity;
import com.contied.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LikeRepository extends JpaRepository<LikeEntity, Long> {
    Optional<LikeEntity> findByUserAndConti(UserEntity user, ContiEntity conti);
    void deleteByUserAndConti(UserEntity user, ContiEntity conti);

    @org.springframework.data.jpa.repository.Query("SELECT l.conti FROM LikeEntity l JOIN FETCH l.conti.user WHERE l.user = :user AND l.conti.state = com.contied.song.entity.State.ACTIVE ORDER BY l.id DESC")
    java.util.List<ContiEntity> findLikedContisByUser(@org.springframework.data.repository.query.Param("user") UserEntity user);
}
