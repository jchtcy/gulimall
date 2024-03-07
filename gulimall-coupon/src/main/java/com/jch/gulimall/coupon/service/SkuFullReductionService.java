package com.jch.gulimall.coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jch.common.to.SkuReductionTo;
import com.jch.common.utils.PageUtils;
import com.jch.gulimall.coupon.entity.SkuFullReductionEntity;

import java.util.Map;

/**
 * 商品满减信息
 *
 * @author jch
 * @email jch@gulimall.com
 * @date 2024-02-28 15:45:54
 */
public interface SkuFullReductionService extends IService<SkuFullReductionEntity> {

    PageUtils queryPage(Map<String, Object> params);

    /**
     * 保存 sku的优惠、满减等信息
     * @param skuReductionTo
     * @return
     */
    void saveSkuReduction(SkuReductionTo skuReductionTo);
}

