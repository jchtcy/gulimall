package com.jch.gulimall.seckill.feign;

import com.jch.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient("gulimall-coupon")
public interface CouponFeignService {
    /**
     * 获取最近三天的活动
     * @return
     */
    @GetMapping("/coupon/seckillsession/lates3DaySession")
    public R getSeckillSkuLatest3Days();
}
