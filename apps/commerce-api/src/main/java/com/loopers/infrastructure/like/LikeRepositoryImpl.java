package com.loopers.infrastructure.like;

import com.loopers.domain.like.Like;
import com.loopers.domain.like.LikeCountDto;
import com.loopers.domain.like.LikeRepository;
import com.loopers.domain.like.LikeType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class LikeRepositoryImpl implements LikeRepository {

    private final LikeJpaRepository likeJpaRepository;

    @Override
    public Optional<Like> findByUserIdAndTargetIdAndType(Long userId, Long targetId, LikeType likeType) {
        return likeJpaRepository.findByUserIdAndTargetIdAndType(userId, targetId, likeType);
    }

    @Override
    public void save(Like like) {
        likeJpaRepository.save(like);
    }

    @Override
    public void delete(Like like) {
        likeJpaRepository.delete(like);
    }

    @Override
    public List<Like> findByUserIdAndType(Long userId, LikeType likeType) {
        return likeJpaRepository.findByUserIdAndType(userId, likeType);
    }

    @Override
    public List<LikeCountDto> countByTargetIdIn(List<Long> targetIds, LikeType type) {
        return likeJpaRepository.countByTargetIdIn(targetIds, type);
    }

    @Override
    public Page<Long> findProductIdsOrderByLikesDesc(Long brandId, Pageable idPageable) {
        return likeJpaRepository.findProductIdsOrderByLikesDesc(brandId, idPageable);
    }

    @Override
    public long getLikeCount(Long targetId) {
        List<LikeCountDto> counts = likeJpaRepository.countByTargetIdIn(List.of(targetId), LikeType.PRODUCT);
        return counts.isEmpty() ? 0L : counts.get(0).count();
    }

    @Override
    public void deleteByUserIdAndTargetIdAndType(Long userId, Long targetId, LikeType likeType) {
        likeJpaRepository.deleteByUserIdAndTargetIdAndType(userId, targetId, likeType);
    }

}
