package com.jch.gulimall.order.feign;

import com.jch.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient("gulimall-member")
public interface MemberFeignService {

    /**
     * 获取会员收获地址列表
     * @param memberId
     * @return
     */
    @GetMapping("/member/memberreceiveaddress/{memberId}/address")
    R getAddress(@PathVariable("memberId") Long memberId);
}
