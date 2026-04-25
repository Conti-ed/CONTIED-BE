package com.contied.search.repository;

import com.contied.search.entity.SearchHistoryEntity;
import com.contied.user.entity.UserEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SearchHistoryRepository extends JpaRepository<SearchHistoryEntity, Long> {
    List<SearchHistoryEntity> findByUserOrderByUpdatedAtDesc(UserEntity user, Pageable pageable);

    Optional<SearchHistoryEntity> findByUserAndQuery(UserEntity user, String query);

    void deleteByUser(UserEntity user);

    /**
     * 동시 삽입에서 race condition 없이 UPSERT 처리한다.
     * (user_id, query) unique 제약이 DB에 반드시 존재해야 한다.
     */
    @Modifying
    @Query(value = "INSERT INTO search_history (user_id, query, created_at, updated_at) " +
                   "VALUES (:userId, :query, NOW(), NOW()) " +
                   "ON CONFLICT (user_id, query) DO UPDATE SET updated_at = NOW()",
           nativeQuery = true)
    void upsertSearch(@Param("userId") Long userId, @Param("query") String query);
}
