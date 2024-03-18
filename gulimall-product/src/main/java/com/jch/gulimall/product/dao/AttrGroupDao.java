package com.jch.gulimall.product.dao;

import com.jch.gulimall.product.entity.AttrGroupEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jch.gulimall.product.vo.SkuItemVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 属性分组
 *
 * @author jch
 * @email jch@gulimall.com
 * @date 2024-02-28 13:17:59
 */
@Mapper
public interface AttrGroupDao extends BaseMapper<AttrGroupEntity> {

    /**
     * 获取spu规格参数信息
     * @param spuId
     * @param catalogId
     * @return
     */
    List<SkuItemVo.SpuItemGroupAttrVo> getAttrGroupWithSpuId(@Param("spuId") Long spuId, @Param("catalogId") Long catalogId);
}
