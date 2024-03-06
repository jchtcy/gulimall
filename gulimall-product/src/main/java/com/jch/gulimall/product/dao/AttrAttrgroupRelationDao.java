package com.jch.gulimall.product.dao;

import com.jch.gulimall.product.entity.AttrAttrgroupRelationEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 属性&属性分组关联
 *
 * @author jch
 * @email jch@gulimall.com
 * @date 2024-02-28 13:17:59
 */
@Mapper
public interface AttrAttrgroupRelationDao extends BaseMapper<AttrAttrgroupRelationEntity> {

    // 根据attrId，attrGroupId批量删除关联关系
    void deleteBatchRelation(@Param("attrAttrgroupRelationEntityList") List<AttrAttrgroupRelationEntity> attrAttrgroupRelationEntityList);
}
