package com.jch.gulimall.seckill.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.jch.common.constant.seckill.SeckillConstant;
import com.jch.common.to.mq.SeckillOrderTo;
import com.jch.common.utils.R;
import com.jch.common.vo.auth.MemberResponseVO;
import com.jch.common.vo.coupon.SeckillSessionsSkus;
import com.jch.common.vo.coupon.SeckillSkuRelationEntity;
import com.jch.gulimall.seckill.feign.CouponFeignService;
import com.jch.gulimall.seckill.feign.ProductFeignService;
import com.jch.gulimall.seckill.interceptor.LoginUserInterceptor;
import com.jch.gulimall.seckill.service.SeckillService;
import com.jch.common.to.seckill.SeckillSkuRedisTo;
import com.jch.common.vo.seckill.SkuInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SeckillServiceImpl implements SeckillService {

    @Autowired
    CouponFeignService couponFeignService;

    @Autowired
    ProductFeignService productFeignService;

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    RedissonClient redissonClient;

    @Autowired
    RabbitTemplate rabbitTemplate;

    /**
     * 上架最新三天的商品
     */
    @Override
    public void uploadSeckillSkuLatest3Days() {
        // 1、得到最近三天需要参与秒杀的活动
        R seckillSessionsSkusR = couponFeignService.getSeckillSkuLatest3Days();
        if (seckillSessionsSkusR.getCode() == 0) {
            List<SeckillSessionsSkus> seckillSessionsSkus = seckillSessionsSkusR.getData(new TypeReference<List<SeckillSessionsSkus>>() {
            });
            if (!CollectionUtils.isEmpty(seckillSessionsSkus)) {
                // 缓存到redis
                saveSessionInfos(seckillSessionsSkus);
                saveSessionSkuInfos(seckillSessionsSkus);
            }
        }
    }

    /**
     * 保存活动信息
     *
     * @param seckillSessionsSkus
     */
    private void saveSessionInfos(List<SeckillSessionsSkus> seckillSessionsSkus) {
        seckillSessionsSkus.forEach(session -> {
            Long startTime = session.getStartTime().getTime();
            Long endTime = session.getEndTime().getTime();
            String key = SeckillConstant.SESSIONS_CACHE_PREFIX + startTime + "_" + endTime;

            List<SeckillSkuRelationEntity> skus = session.getRelationSkus();
            Boolean hasKey = redisTemplate.hasKey(key);
            if (!hasKey && !CollectionUtils.isEmpty(skus)) {
                // 缓存活动信息
                List<String> skuIds = skus.stream()
                        .map(item -> item.getPromotionSessionId() + "_" + item.getSkuId().toString())
                        .collect(Collectors.toList());
                redisTemplate.opsForList().leftPushAll(key, skuIds);
            }
        });
    }

    /**
     * 保存商品信息
     *
     * @param seckillSessionsSkus
     */
    private void saveSessionSkuInfos(List<SeckillSessionsSkus> seckillSessionsSkus) {
        seckillSessionsSkus.forEach(session -> {
            BoundHashOperations<String, Object, Object> ops = redisTemplate.boundHashOps(SeckillConstant.SKUKILL_CACHE_PREFIX);
            List<SeckillSkuRelationEntity> skus = session.getRelationSkus();
            if (!CollectionUtils.isEmpty(skus)) {
                skus.forEach(sku -> {
                    // 商品随机码
                    String token = UUID.randomUUID().toString().replace("-", "");

                    String skuKey = sku.getPromotionSessionId() + "_" + sku.getSkuId().toString();
                    if (!ops.hasKey(skuKey)) {
                        // 缓存商品
                        SeckillSkuRedisTo redisTo = new SeckillSkuRedisTo();
                        // 1、sku的基本信息
                        R skuInfoR = productFeignService.getSkuInfo(sku.getSkuId());
                        if (skuInfoR.getCode() == 0) {
                            SkuInfoVo skuInfo = skuInfoR.getData("skuInfo", new TypeReference<SkuInfoVo>() {
                            });
                            redisTo.setSkuInfo(skuInfo);
                        }
                        // 2、sku的秒杀信息
                        BeanUtils.copyProperties(sku, redisTo);
                        // 3、设置当前商品的秒杀信息
                        redisTo.setStartTime(session.getStartTime().getTime());
                        redisTo.setEndTime(session.getEndTime().getTime());
                        // 4、商品的随机码
                        redisTo.setRandomCode(token);

                        String jsonString = JSON.toJSONString(redisTo);
                        ops.put(skuKey, jsonString);

                        // 5、商品可以秒杀的数量作为信号量
                        String semaphoreKey = SeckillConstant.SKU_STOCK_SEMAPHORE + token;
                        RSemaphore semaphore = redissonClient.getSemaphore(semaphoreKey);
                        semaphore.trySetPermits(sku.getSeckillCount());
                    }
                });
            }
        });
    }

    /**
     * 返回当前时间可以参与秒杀的商品信息
     *
     * @return
     */
    @Override
    public List<SeckillSkuRedisTo> getCurrentSeckillSkus() {
        // 获取当前时间属于哪个秒杀场次
        Long time = new Date().getTime();
        Set<String> keys = redisTemplate.keys(SeckillConstant.SESSIONS_CACHE_PREFIX + "*");
        if (!CollectionUtils.isEmpty(keys)) {
            for (String key : keys) {
                String timeStr = key.split(":")[2];
                String[] times = timeStr.split("_");
                Long startTime = Long.parseLong(times[0]);
                Long endTime = Long.parseLong(times[1]);

                if (time >= startTime && time <= endTime) {
                    // 获取商品信息
                    List<String> range = redisTemplate.opsForList().range(key, -100, 100);
                    BoundHashOperations<String, String, String> ops = redisTemplate.boundHashOps(SeckillConstant.SKUKILL_CACHE_PREFIX);
                    List<String> list = ops.multiGet(range);
                    if (list != null) {
                        List<SeckillSkuRedisTo> result = list.stream().map(item -> {
                            SeckillSkuRedisTo redisTo = JSON.parseObject(item, SeckillSkuRedisTo.class);
                            redisTo.setRandomCode(null);
                            return redisTo;
                        }).collect(Collectors.toList());
                        return result;
                    }
                    break;
                }
            }
        }
        return null;
    }

    /**
     * 商品详情查询秒杀信息
     * @param skuId
     * @return
     */
    @Override
    public SeckillSkuRedisTo getSkuSeckilInfo(Long skuId) {
        // 找到所有需要参与秒杀的商品key
        BoundHashOperations<String, String, String> ops = redisTemplate.boundHashOps(SeckillConstant.SKUKILL_CACHE_PREFIX);

        Set<String> keys = ops.keys();
        if (!CollectionUtils.isEmpty(keys)) {
            String regx = "\\d_" + skuId;
            for (String key : keys) {
                if (Pattern.matches(regx, key)) {
                    String json = ops.get(key);
                    SeckillSkuRedisTo redisTo = JSON.parseObject(json, SeckillSkuRedisTo.class);

                    // 随机码
                    Long now = new Date().getTime();
                    Long startTime = redisTo.getStartTime();
                    Long endTime = redisTo.getEndTime();
                    if (!(now >= startTime && now <= endTime)) {
                        redisTo.setRandomCode(null);
                    }
                    return redisTo;
                }
            }
        }
        return null;
    }

    /**
     * 秒杀方法
     * @param killId
     * @param key
     * @param num
     * @return
     */
    @Override
    public String kill(String killId, String key, Integer num) {
        MemberResponseVO responseVO = LoginUserInterceptor.loginUser.get();

        // 获取当前秒杀商品的详细信息
        BoundHashOperations<String, String, String> ops = redisTemplate.boundHashOps(SeckillConstant.SKUKILL_CACHE_PREFIX);

        String json = ops.get(killId);
        if (!StringUtils.isEmpty(json)) {
            SeckillSkuRedisTo redisTo = JSON.parseObject(json, SeckillSkuRedisTo.class);
            // 校验时间合法性
            Long startTime = redisTo.getStartTime();
            Long endTime = redisTo.getEndTime();
            long now = System.currentTimeMillis();
            Long ttl = endTime - now;
            if (now >= startTime && now <= endTime) {
                // 2、校验随机码和商品id
                String randomCode = redisTo.getRandomCode();
                String skuId = redisTo.getPromotionSessionId() + "_" + redisTo.getSkuId();
                if (randomCode.equals(key) && killId.equals(skuId)) {
                    // 3、验证购物数量是否合理
                    if (num <= redisTo.getSeckillLimit()) {
                        // 4、验证这个人是否已经购买过, 幂等性; 如果秒杀成功, 就去占位
                        String redisKey = responseVO.getId() + "_" + skuId;
                        Boolean absent = redisTemplate.opsForValue().setIfAbsent(redisKey, num.toString(), ttl, TimeUnit.MILLISECONDS);
                        if (absent) {
                            // 占位成功 没买过
                            RSemaphore semaphore = redissonClient.getSemaphore(SeckillConstant.SKU_STOCK_SEMAPHORE + randomCode);
                            boolean acquire = semaphore.tryAcquire(num);
                            //保证Redis中还有商品库存
                            if (acquire) {
                                // 秒杀成功, 给MQ发送消息
                                String timeId = IdWorker.getTimeId();
                                SeckillOrderTo orderTo = new SeckillOrderTo();
                                orderTo.setOrderSn(timeId);
                                orderTo.setMemberId(responseVO.getId());
                                orderTo.setNum(num);
                                orderTo.setPromotionSessionId(redisTo.getPromotionSessionId());
                                orderTo.setSkuId(redisTo.getSkuId());
                                orderTo.setSeckillPrice(redisTo.getSeckillPrice());

                                rabbitTemplate.convertAndSend("order-event-exchange", "order.seckill.order", orderTo);
                                return timeId;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }
}
