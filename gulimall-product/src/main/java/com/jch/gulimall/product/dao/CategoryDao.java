package com.jch.gulimall.product.dao;

import com.jch.gulimall.product.entity.CategoryEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品三级分类
 * 
 * @author jch
 * @email jch@gulimall.com
 * @date 2024-02-28 13:17:59
 */
@Mapper
public interface CategoryDao extends BaseMapper<CategoryEntity> {
	
}
