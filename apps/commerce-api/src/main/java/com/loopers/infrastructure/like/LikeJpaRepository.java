package com.loopers.infrastructure.like;

import com.loopers.domain.like.Like;
import com.loopers.domain.like.LikeCountDto;
import com.loopers.domain.like.LikeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LikeJpaRepository extends JpaRepository<Like, Long> {

    Optional<Like> findByUserIdAndTargetIdAndType(Long userId, Long targetId, LikeType likeType);

    List<Like> findByUserIdAndType(Long userId, LikeType likeType);

    @Query("SELECT new com.loopers.domain.like.LikeCountDto(l.targetId, COUNT(l)) " +
            "FROM Like l " +
            "WHERE l.targetId IN :targetIds AND l.type = :type " +
            "GROUP BY l.targetId")
    List<LikeCountDto> countByTargetIdIn(@Param("targetIds") List<Long> targetIds, @Param("type") LikeType type);

    @Query(value = "SELECT l.targetId FROM Like l WHERE l.type = 'PRODUCT' " +
            "AND (:brandId IS NULL OR l.targetId IN (SELECT p.id FROM Product p WHERE p.brandId = :brandId AND p.status = 'ACTIVE')) " +
            "GROUP BY l.targetId ORDER BY COUNT(l) DESC",
            countQuery = "SELECT COUNT(DISTINCT l.targetId) FROM Like l WHERE l.type = 'PRODUCT' " +
                    "AND (:brandId IS NULL OR l.targetId IN (SELECT p.id FROM Product p WHERE p.brandId = :brandId AND p.status = 'ACTIVE'))")
    Page<Long> findProductIdsOrderByLikesDesc(@Param("brandId") Long brandId, Pageable pageable);

    @Modifying
    void deleteByUserIdAndTargetIdAndType(Long userId, Long targetId, LikeType likeType);

}
