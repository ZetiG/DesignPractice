package com.example.demo.seckill;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Description:
 *
 * @author Zeti
 * @date 2025/6/25 10:35
 */
@Component
public class SeckillListConsumer {

    @Resource
    private RedisTemplate<String, String> redisTemplate;
    @Resource
    private OrderRepository orderRepo;

    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${seckill.list.name:seckill:orders}")
    private String listName;

    private final ExecutorService threadPool = Executors.newFixedThreadPool(5); // 可配置线程数

    @PostConstruct
    public void start() {
        Thread dispatcher = new Thread(this::dispatchTasks);
        dispatcher.setDaemon(true);
        dispatcher.start();
    }

    /**
     * 轮询 Redis 消息并分发给线程池
     */
    private void dispatchTasks() {
        final String listKey = listName;

        while (true) {
            try {
                String msgJson = redisTemplate.opsForList().rightPop(listKey, 10, TimeUnit.SECONDS);
                if (msgJson == null || msgJson.isEmpty()) {
                    continue;
                }

                // 分发任务给线程池异步处理
                threadPool.submit(() -> consumeOne(msgJson));

            } catch (Exception e) {
                System.err.println("【调度线程异常】：" + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * 处理单个订单消息
     */
    private void consumeOne(String msgJson) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = mapper.readValue(msgJson, Map.class);

            String userId  = String.valueOf(map.get("userId"));
            String orderId = String.valueOf(map.get("orderId"));
            String skuId   = String.valueOf(map.get("skuId"));

            System.out.println(Thread.currentThread().getName() +
                    " → 正在处理订单：" + orderId + " 用户：" + userId + " 商品：" + skuId);

            Order order = new Order();
            order.setOrderId(orderId);
            order.setSkuId(Long.valueOf(skuId));
            order.setUserId(Long.valueOf(userId));
            order.setTimestamp(Instant.ofEpochMilli(Long.parseLong(map.get("timestamp").toString())));

            if (!orderRepo.existsById(order.getOrderId())) {
                orderRepo.save(order);
                System.out.println("✅ 订单落库成功：" + orderId);
            } else {
                System.out.println("⚠️ 已存在订单，跳过：" + orderId);
            }

        } catch (Exception ex) {
            System.err.println("❌ 订单消费异常，推入死信队列：" + ex.getMessage());
            ex.printStackTrace();
            // 推入死信队列以便后续人工处理
            redisTemplate.opsForList().leftPush("order:dead", msgJson);
        }
    }
}
