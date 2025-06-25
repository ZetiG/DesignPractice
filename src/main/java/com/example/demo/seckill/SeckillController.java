package com.example.demo.seckill;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.shaded.guava18.com.google.common.util.concurrent.RateLimiter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Description:
 *
 * @author Zeti
 * @date 2025/6/25 10:24
 */
@RestController
@RequestMapping("/sale")
public class SeckillController {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private SkuStockRepository skuStockRepo;


    private final ObjectMapper mapper = new ObjectMapper();
    private RateLimiter rateLimiter;

    /** 用于推单的 List 名称，可在 application.properties 覆盖 */
    @Value("${seckill.list.name:seckill:orders}")
    private String listName;

    @PostConstruct
    public void init() {
        // 限流：每秒最多 500 个请求
        rateLimiter = RateLimiter.create(500.0);
    }

    /**
     * 秒杀下单接口：限流 → 防重 → 占库存 → 写入异步队列 → 锁定库存（order:lock）
     */
    @PostMapping("/seckill/{skuId}")
    public ResponseEntity<String> kill(@PathVariable Long skuId,
                                       @RequestParam Long userId) throws Exception {
        // 1. 限流
        if (!rateLimiter.tryAcquire()) {
            return ResponseEntity.status(429).body("请求过于频繁");
        }

        // 2. 防重复（防止用户重复下单）
        String userKey = "seckill:users:" + skuId + ":" + userId;
        Boolean first = redisTemplate.opsForValue().setIfAbsent(userKey, "1");
        if (!Boolean.TRUE.equals(first)) {
            return ResponseEntity.badRequest().body("请勿重复秒杀，下单后未支付请等待系统取消后重试");
        }

        // 3. 占库存（仅占用，未真正扣减）
        String stockKey = "stock:" + skuId;
        Long stockLeft = redisTemplate.opsForValue().decrement(stockKey);
        if (stockLeft == null || stockLeft < 0) {
            redisTemplate.opsForValue().increment(stockKey); // 回滚占用
            redisTemplate.delete(userKey); // 恢复防重标记
            return ResponseEntity.ok("库存不足");
        }

        // 4. 构建订单信息
        String orderId = userId + "-" + System.currentTimeMillis() + "-" + ThreadLocalRandom.current().nextInt(100000, 999999);

        Map<String, Object> order = new HashMap<>();
        order.put("orderId", orderId);
        order.put("skuId", skuId);
        order.put("userId", userId);
        order.put("type", "seckill");
        order.put("timestamp", System.currentTimeMillis());
        order.put("expireAt", System.currentTimeMillis() + 5 * 60 * 1000);

        String json = mapper.writeValueAsString(order);

        // 5. 写入异步队列（订单持久化）
        redisTemplate.opsForList().leftPush(listName, json);

        // 6. 写入 Redis 锁定订单，不设置过期时间（由 rollback 任务控制）
        redisTemplate.opsForValue().set("order:lock:" + orderId, json);

        // ✅ 防重标记保留 5分钟，系统或用户支付后手动删除
        redisTemplate.expire(userKey, 5, TimeUnit.MINUTES);

        return ResponseEntity.ok("下单成功，请尽快支付，订单ID: " + orderId);
    }


    /**
     * 普通售卖下单：不扣库存，仅生成订单，等支付成功再扣库存
     */
    @PostMapping("/order/{skuId}")
    public ResponseEntity<String> placeOrder(@PathVariable Long skuId, @RequestParam Long userId) throws JsonProcessingException {
        // 1. 尝试占用库存（decrement）
        String stockKey = "stock:" + skuId;
        Long stockLeft = redisTemplate.opsForValue().decrement(stockKey);
        if (stockLeft == null || stockLeft < 0) {
            // 回滚占用（即库存为负了，说明已卖完）
            redisTemplate.opsForValue().increment(stockKey);
            return ResponseEntity.ok("库存不足");
        }

        // 2. 构建订单信息
        String orderId = userId + "-" + System.currentTimeMillis() + "-" + ThreadLocalRandom.current().nextInt(100000, 999999);

        String lockKey = "order:lock:" + orderId;

        Map<String, Object> msg = new HashMap<>();
        msg.put("orderId",   orderId);
        msg.put("skuId",     skuId);
        msg.put("userId",    userId);
        msg.put("type",      "normal");
        msg.put("timestamp", String.valueOf(System.currentTimeMillis()));
        msg.put("expireAt", System.currentTimeMillis() + 5 * 60 * 1000); // 5分钟后过期

        // 3. 将订单信息写入 Redis 锁定区，5分钟内必须支付
        String json = mapper.writeValueAsString(msg);
        redisTemplate.opsForValue().set(lockKey, json); // 不设置过期时间

        // 4. 可选：加入异步下单队列（与秒杀保持一致）
        redisTemplate.opsForList().leftPush(listName, json);

        return ResponseEntity.ok("下单成功，请尽快支付，订单ID: " + orderId);
    }


    /**
     * 支付接口-支付成功后才扣库存
     * @param orderId
     * @return
     */
    @PostMapping("/pay")
    public ResponseEntity<String> pay(@RequestParam String orderId) {
        String lockKey = "order:lock:" + orderId;
        Object obj = redisTemplate.opsForValue().get(lockKey);
        if (obj == null) {
            return ResponseEntity.badRequest().body("订单不存在或已处理");
        }

        try {
            Map<String, Object> map = mapper.readValue(String.valueOf(obj), Map.class);
            Long skuId = Long.valueOf(map.get("skuId").toString());

            // 判断是否已支付
            if (Boolean.TRUE.equals(redisTemplate.hasKey("order:paid:" + orderId))) {
                return ResponseEntity.ok("订单已支付");
            }

            // ✅ 1. 扣减数据库库存（注意并发场景会返回0）
            int rows = skuStockRepo.deductStock(skuId);
            if (rows == 0) {
                return ResponseEntity.status(500).body("库存扣减失败，请联系客服");
            }

            // ✅ 2. 删除 Redis 锁定库存
            redisTemplate.delete(lockKey);

            // ✅ 3. 标记支付成功
            redisTemplate.opsForValue().set("order:paid:" + orderId, "1", Duration.ofHours(1));

            return ResponseEntity.ok("支付成功，库存已扣减（含数据库）");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("支付处理失败");
        }
    }


    /**
     * 商品当前剩余库存
     * @param skuId
     * @return
     */
    @GetMapping("/stock/{skuId}")
    public ResponseEntity<String> getStock(@PathVariable Long skuId) {
        String key = "stock:" + skuId;
        Object obj = redisTemplate.opsForValue().get(key);
        if (obj == null) {
            return ResponseEntity.badRequest().body("商品ID不存在或已售罄");
        }
        return ResponseEntity.ok("商品ID：" + skuId + " 剩余库存：" + String.valueOf(obj));
    }


    @Scheduled(fixedDelay = 5000)
    public void rollbackUnpaidOrders() {
        Set<String> keys = redisTemplate.keys("order:lock:*");
        for (String key : keys) {
            try {
                Object obj = redisTemplate.opsForValue().get(key);
                if (obj != null) {
                    Map<String, Object> map = mapper.readValue(String.valueOf(obj), Map.class);
                    Long expireAt = Long.valueOf(map.get("expireAt").toString());

                    if (System.currentTimeMillis() > expireAt) {
                        Long skuId = Long.valueOf(map.get("skuId").toString());

                        // 幂等判断（是否已支付）
                        Boolean paid = redisTemplate.hasKey("order:paid:" + map.get("orderId"));
                        if (Boolean.TRUE.equals(paid)) {
                            continue;
                        }

                        redisTemplate.opsForValue().increment("stock:" + skuId); // 回滚库存
                        redisTemplate.delete(key); // 删除订单锁定记录

                        System.out.println("订单超时释放库存：" + key);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


}