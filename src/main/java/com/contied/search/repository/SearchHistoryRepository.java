package com.contied.search.repository;

import com.contied.search.entity.SearchHistoryEntity;
import com.contied.user.entity.UserEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SearchHistoryRepository extends JpaRepository<SearchHistoryEntity, Long> {
    List<SearchHistoryEntity> findByUserOrderByUpdatedAtDesc(UserEntity user, Pageable pageable);
    
    Optional<SearchHistoryEntity> findByUserAndQuery(UserEntity user, String query);
    
    void deleteByUser(UserEntity user);
}
