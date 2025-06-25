package com.example.demo.seckill;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.transaction.Transactional;

/**
 * Description:
 *
 * @author Zeti
 * @date 2025/6/25 16:07
 */
public interface SkuStockRepository extends JpaRepository<SkuStock, Long> {

    @Modifying
    @Transactional
    @Query("UPDATE SkuStock s SET s.stock = s.stock - 1 WHERE s.skuId = :skuId AND s.stock > 0")
    int deductStock(@Param("skuId") Long skuId);
}
