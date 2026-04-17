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

    // songs 컬렉션은 ContiEntity 에 @BatchSize(100) 이 설정돼 있어 배치 로딩으로 처리됨.
    // (JPQL 에서 DISTINCT + ORDER BY 조합은 PostgreSQL 에서 "ORDER BY expressions must appear in select list" 로 실패)
    @Query("SELECT l.conti FROM LikeEntity l " +
            "JOIN FETCH l.conti.user " +
            "WHERE l.user = :user " +
            "AND l.conti.state = com.contied.song.entity.State.ACTIVE " +
            "ORDER BY l.id DESC")
    List<ContiEntity> findLikedContisByUser(@Param("user") UserEntity user);
}
