package com.example.demo.seckill;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.Instant;

/**
 * Description:
 *
 * @author Zeti
 * @date 2025/6/25 10:55
 */
@Entity
@Table(name = "orders")
public class Order {
    @Id
    private String orderId;
    private Long userId;
    private Long skuId;
    private Instant timestamp;
    @Column(nullable = false)
    private String status = "PENDING";

    // getters & setters
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getSkuId() { return skuId; }
    public void setSkuId(Long skuId) { this.skuId = skuId; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
