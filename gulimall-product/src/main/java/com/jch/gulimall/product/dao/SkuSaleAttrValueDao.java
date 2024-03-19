package com.jch.gulimall.product.dao;

import com.jch.gulimall.product.entity.SkuSaleAttrValueEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jch.gulimall.product.vo.SkuItemVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * sku销售属性&值
 *
 * @author jch
 * @email jch@gulimall.com
 * @date 2024-02-28 13:17:59
 */
@Mapper
public interface SkuSaleAttrValueDao extends BaseMapper<SkuSaleAttrValueEntity> {

    /**
     * 获取spu的销售属性组合
     * @param spuId
     * @return
     */
    List<SkuItemVo.SkuItemSaleAttrVo> getSaleAttrsBySpuId(@Param("spuId") Long spuId);

    /**
     * 查询sku的组合信息
     * @param skuId
     * @return
     */
    List<String> getSkuSaleAttrValuesAsStringList(@Param("skuId") Long skuId);
}
