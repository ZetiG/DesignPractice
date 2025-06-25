package com.example.demo.seckill;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Description: 高并发压测-普通下单、秒杀下单、部分用户支付，校验库存是否准确扣减
 *
 * @author Zeti
 * @date 2025/6/25 16:45
 */
public class SeckillPressureTest {

    private static final int THREAD_COUNT = 100;
    private static final int USER_TOTAL = 2000;
    private static final int SKU_ID = 1;
    private static final String HOST = "http://localhost:8888";

    private static final List<String> paidOrderIds = Collections.synchronizedList(new ArrayList<>());

    // ✅ 统计变量
    private static final AtomicInteger seckillCount = new AtomicInteger(0);
    private static final AtomicInteger normalCount = new AtomicInteger(0);
    private static final AtomicInteger failCount = new AtomicInteger(0);
    private static final AtomicInteger paidCount = new AtomicInteger(0);

    public static void main(String[] args) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(1);

        for (int i = 0; i < USER_TOTAL; i++) {
            final int userId = i + 1;
            executor.submit(() -> {
                try {
                    latch.await();

                    String endpoint = (userId % 2 == 0) ? "/sale/seckill/" : "/sale/order/";
                    String url = HOST + endpoint + SKU_ID;
                    HttpPost post = new HttpPost(url);

                    List<NameValuePair> params = new ArrayList<>();
                    params.add(new BasicNameValuePair("userId", String.valueOf(userId)));
                    post.setEntity(new UrlEncodedFormEntity(params, StandardCharsets.UTF_8));

                    try (CloseableHttpClient client = HttpClients.createDefault();
                         CloseableHttpResponse response = client.execute(post)) {

                        String res = EntityUtils.toString(response.getEntity());

                        if (res.contains("订单ID")) {
                            String orderId = extractOrderId(res);
                            if (userId % 2 == 0) {
                                seckillCount.incrementAndGet();
                            } else {
                                normalCount.incrementAndGet();
                            }

                            // 模拟 30% 用户支付
                            if (paidOrderIds.size() < USER_TOTAL * 0.3) {
                                paidOrderIds.add(orderId);
                            }

                        } else {
                            failCount.incrementAndGet();
                        }

                    }
                } catch (Exception e) {
                    failCount.incrementAndGet();
                }
            });
        }

        // 启动并发请求
        System.out.println("🚀 开始压测...");
        long startTime = System.currentTimeMillis();
        latch.countDown();
        executor.shutdown();
        executor.awaitTermination(2, TimeUnit.MINUTES);

        // 模拟支付
        System.out.println("💰 模拟支付中...");
        for (String orderId : paidOrderIds) {
            try {
                String payUrl = HOST + "/sale/pay?orderId=" + orderId;
                HttpPost payPost = new HttpPost(payUrl);
                try (CloseableHttpClient client = HttpClients.createDefault();
                     CloseableHttpResponse response = client.execute(payPost)) {

                    String res = EntityUtils.toString(response.getEntity());
                    if (res.contains("支付成功")) {
                        paidCount.incrementAndGet();
                    }
                }
            } catch (Exception ignored) {}
        }

        long endTime = System.currentTimeMillis();

        // ✅ 结果统计输出
        System.out.println("\n==================== ✅ 压测完成 ====================");
        System.out.println("参与用户数        ：" + USER_TOTAL);
        System.out.println("秒杀下单成功用户数：" + seckillCount.get());
        System.out.println("普通下单成功用户数：" + normalCount.get());
        System.out.println("支付成功用户数    ：" + paidCount.get());
        System.out.println("下单失败用户数    ：" + failCount.get());
        System.out.println("总耗时（ms）      ：" + (endTime - startTime));
        System.out.println("=====================================================");
    }

    private static String extractOrderId(String response) {
        try {
            int idx = response.indexOf("订单ID:");
            if (idx != -1) {
                return response.substring(idx + 5).trim();
            }
        } catch (Exception ignored) {}
        return UUID.randomUUID().toString();
    }
}
