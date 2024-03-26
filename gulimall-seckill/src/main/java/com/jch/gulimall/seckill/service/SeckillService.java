package com.jch.gulimall.seckill.service;

import com.jch.common.to.seckill.SeckillSkuRedisTo;

import java.util.List;

public interface SeckillService {
    /**
     * 上架最新三天的商品
     */
    void uploadSeckillSkuLatest3Days();

    /**
     * 返回当前时间可以参与秒杀的商品信息
     * @return
     */
    List<SeckillSkuRedisTo> getCurrentSeckillSkus();

    /**
     * 商品详情查询秒杀信息
     * @param skuId
     * @return
     */
    SeckillSkuRedisTo getSkuSeckilInfo(Long skuId);

    /**
     * 秒杀方法
     * @param killId
     * @param key
     * @param num
     * @return
     */
    String kill(String killId, String key, Integer num);
}
