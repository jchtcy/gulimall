package com.jch.authserver.feign;

import com.jch.authserver.vo.UserLoginVo;
import com.jch.authserver.vo.UserRegistVo;
import com.jch.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient("gulimall-member")
public interface MemberFeignService {

    /**
     * 注册进会员
     * @param vo
     * @return
     */
    @PostMapping("/member/member/register")
    public R regist(@RequestBody UserRegistVo vo);

    /**
     * 会员登录
     * @param vo
     * @return
     */
    @PostMapping("/member/member/login")
    public R login(@RequestBody UserLoginVo vo);
}
