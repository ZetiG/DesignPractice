package com.example.demo.seckill;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Description:
 *
 * @author Zeti
 * @date 2025/6/25 16:06
 */
@Entity
@Table(name = "sku_stock")
public class SkuStock {
    @Id
    private Long skuId;

    private Integer stock;

    // getters / setters
    public Long getSkuId() { return skuId; }
    public void setSkuId(Long skuId) { this.skuId = skuId; }

    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
}
