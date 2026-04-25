package com.contied.search.entity;

import com.contied.global.entity.BaseEntity;
import com.contied.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;

// TODO: deploy 시 아래 SQL을 운영 DB에 수동 적용 필요
// -- 중복 row 정리 후 unique 제약 추가
// DELETE FROM search_history a USING search_history b
//   WHERE a.id < b.id AND a.user_id = b.user_id AND a.query = b.query;
// ALTER TABLE search_history ADD CONSTRAINT uk_search_user_query UNIQUE (user_id, query);
@Entity
@Table(
    name = "search_history",
    uniqueConstraints = @UniqueConstraint(name = "uk_search_user_query", columnNames = {"user_id", "query"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchHistoryEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(nullable = false)
    private String query;
}
