package com.jch.gulimall.cart.feign;

import com.jch.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@FeignClient("gulimall-product")
public interface ProductFeignService {

    /**
     * 信息
     */
    @RequestMapping("/product/skuinfo/info/{skuId}")
    R info(@PathVariable("skuId") Long skuId);

    /**
     * 查询sku的组合信息
     * @param skuId
     * @return
     */
    @GetMapping("/product/skusaleattrvalue/stringlist/{skuId}")
    public R getSkuSaleAttrValues(@PathVariable("skuId") Long skuId);

    /**
     * 获取指定商品的价格
     */
    @GetMapping("/product/skuinfo/{skuId}/price")
    public R getPrice(@PathVariable("skuId") Long skuId);
}
