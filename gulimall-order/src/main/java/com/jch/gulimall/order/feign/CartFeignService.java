package com.jch.gulimall.order.feign;

import com.jch.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient("gulimall-cart")
public interface CartFeignService {

    /**
     * 获取当前登录用户所有购物车里的数据
     * @return
     */
    @GetMapping("/currentUserCartItems")
    R getCurrentUserCartItems();
}
