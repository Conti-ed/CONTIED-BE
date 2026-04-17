package com.contied.conti.repository;

import com.contied.conti.entity.ContiEntity;
import com.contied.conti.entity.LikeEntity;
import com.contied.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LikeRepository extends JpaRepository<LikeEntity, Long> {
    Optional<LikeEntity> findByUserAndConti(UserEntity user, ContiEntity conti);
    boolean existsByUserAndConti(UserEntity user, ContiEntity conti);
    void deleteByUserAndConti(UserEntity user, ContiEntity conti);

    // songs 컬렉션까지 함께 fetch 하여 ContiResponse.from() 에서의 N+1 방지.
    // @ManyToMany 를 LEFT JOIN FETCH 하면 row 수가 곡 수만큼 늘어나므로 DISTINCT 로 중복 제거.
    @Query("SELECT DISTINCT l.conti FROM LikeEntity l " +
            "JOIN FETCH l.conti.user " +
            "LEFT JOIN FETCH l.conti.songs " +
            "WHERE l.user = :user " +
            "AND l.conti.state = com.contied.song.entity.State.ACTIVE " +
            "ORDER BY l.id DESC")
    List<ContiEntity> findLikedContisByUser(@Param("user") UserEntity user);
}
