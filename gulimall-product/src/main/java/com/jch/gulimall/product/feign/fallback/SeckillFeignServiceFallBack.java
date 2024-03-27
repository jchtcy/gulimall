package com.jch.gulimall.product.feign.fallback;

import com.jch.common.exception.BizCodeEnum;
import com.jch.common.utils.R;
import com.jch.gulimall.product.feign.SeckillFeignService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SeckillFeignServiceFallBack implements SeckillFeignService {

    @Override
    public R getSkuSeckilInfo(Long skuId) {
        log.error("降级方法 getSkuSeckilInfo");
        return R.error(BizCodeEnum.SMS_CODE_EXCEPTION.getCode(), BizCodeEnum.SMS_CODE_EXCEPTION.getMsg());
    }
}
