package com.jch.gulimall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jch.common.utils.PageUtils;
import com.jch.gulimall.product.entity.CategoryEntity;

import java.util.List;
import java.util.Map;

/**
 * 商品三级分类
 *
 * @author jch
 * @email jch@gulimall.com
 * @date 2024-02-28 13:17:59
 */
public interface CategoryService extends IService<CategoryEntity> {

    PageUtils queryPage(Map<String, Object> params);

    /**
     * 查出所有分类以及子分类, 以树形结构组装
     * @return
     */
    List<CategoryEntity> listWithTree();

    /**
     * 删除（逻辑删除）
     * @param list
     */
    void removeMenuByIds(List<Long> list);

    /**
     * 根据分类id查询完整路径
     * @param catelogId 分类id
     * @return
     */
    Long[] findCatelogPath(Long catelogId);

    /**
     * 修改分类表和品牌分类关联表
     * @param category
     */
    void updateDetail(CategoryEntity category);
}

