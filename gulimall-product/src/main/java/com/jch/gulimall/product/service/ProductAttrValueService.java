package com.jch.gulimall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jch.common.utils.PageUtils;
import com.jch.gulimall.product.entity.ProductAttrValueEntity;

import java.util.List;
import java.util.Map;

/**
 * spu属性值
 *
 * @author jch
 * @email jch@gulimall.com
 * @date 2024-02-28 13:17:59
 */
public interface ProductAttrValueService extends IService<ProductAttrValueEntity> {

    PageUtils queryPage(Map<String, Object> params);

    /**
     *
     * @param spuId
     * @return
     */
    List<ProductAttrValueEntity> baseAttrlistForSpu(Long spuId);

    /**
     * 修改商品规格
     * @param spuId
     * @param entities
     */
    void updateSpuAttr(Long spuId, List<ProductAttrValueEntity> entities);
}

