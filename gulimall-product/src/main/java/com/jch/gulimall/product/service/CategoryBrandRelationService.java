package com.jch.gulimall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jch.common.utils.PageUtils;
import com.jch.gulimall.product.entity.BrandEntity;
import com.jch.gulimall.product.entity.CategoryBrandRelationEntity;
import com.jch.gulimall.product.entity.CategoryEntity;

import java.util.List;
import java.util.Map;

/**
 * 品牌分类关联
 *
 * @author jch
 * @email jch@gulimall.com
 * @date 2024-02-28 13:17:59
 */
public interface CategoryBrandRelationService extends IService<CategoryBrandRelationEntity> {

    PageUtils queryPage(Map<String, Object> params);

    /**
     * 保存品牌与分类的详细信息
     * @param categoryBrandRelation
     */
    void saveDetail(CategoryBrandRelationEntity categoryBrandRelation);

    /**
     * 更新品牌名
     * @param brand
     */
    void updateBrand(BrandEntity brand);

    /**
     * 更新分类名
     * @param category
     */
    void updateCategory(CategoryEntity category);

    /**
     * 根据分类id获取该分类下的所有品牌
     * @param catId
     * @return
     */
    List<BrandEntity> getBrandListByCatId(Long catId);
}

