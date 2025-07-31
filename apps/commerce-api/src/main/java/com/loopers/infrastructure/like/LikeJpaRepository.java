package com.loopers.infrastructure.like;

import com.loopers.domain.like.Like;
import com.loopers.domain.like.LikeCountDto;
import com.loopers.domain.like.LikeType;
import org.springframework.data.jpa.repository.JpaRepository;
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

}
