package com.jch.gulimall.order.feign;

import com.jch.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient("gulimall-product")
public interface ProductFeignService {

    /**
     * 根据skuId获取spu信息
     * @param skuId
     * @return
     */
    @GetMapping("/product/spuinfo/{skuId}/up")
    public R getSpuInfoBySkuId(@PathVariable("skuId") Long skuId);
}
