package com.loopers.infrastructure.product;


import com.loopers.domain.product.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;


public interface ProductJpaRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Page<Product> findByIdIn(List<Long> productIds, Pageable pageable);

    @Query(value = "SELECT p FROM Product p WHERE p.status = 'ACTIVE' AND (:brandId IS NULL OR p.brandId = :brandId)",
            countQuery = "SELECT count(p) FROM Product p WHERE p.status = 'ACTIVE' AND (:brandId IS NULL OR p.brandId = :brandId)")
    Page<Product> findActiveProductsOrderByLikesDesc(@Param("brandId") Long brandId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id in :productIds")
    List<Product> findAllByIdWithLock(@Param("productIds") List<Long> productIds);

}
