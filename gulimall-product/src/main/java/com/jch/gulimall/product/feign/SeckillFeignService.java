package com.jch.gulimall.product.feign;

import com.jch.common.utils.R;
import com.jch.gulimall.product.feign.fallback.SeckillFeignServiceFallBack;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

@FeignClient(value = "gulimall-seckill", fallback = SeckillFeignServiceFallBack.class)
public interface SeckillFeignService {

    /**
     * 商品详情查询秒杀信息
     * @param skuId
     * @return
     */
    @GetMapping(value = "/sku/seckill/{skuId}")
    @ResponseBody
    public R getSkuSeckilInfo(@PathVariable("skuId") Long skuId);
}
