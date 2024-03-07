package com.jch.gulimall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jch.common.utils.PageUtils;
import com.jch.gulimall.product.entity.SpuInfoEntity;
import com.jch.gulimall.product.vo.spusavevo.SpuSaveVO;

import java.util.Map;

/**
 * spu信息
 *
 * @author jch
 * @email jch@gulimall.com
 * @date 2024-02-28 13:17:59
 */
public interface SpuInfoService extends IService<SpuInfoEntity> {

    PageUtils queryPage(Map<String, Object> params);

    /**
     * 新增商品
     * @param spuSaveVO
     */
    void saveSpuInfo(SpuSaveVO spuSaveVO);

    /**
     * 保存保存spu基本信息
     * @param spuInfoEntity
     */
    void saveBaseSpuInfo(SpuInfoEntity spuInfoEntity);

    /**
     * spu检索
     * @param params
     * @return
     */
    PageUtils queryPageByCondition(Map<String, Object> params);
}

