package com.jch.gulimall.product.service.impl;

import com.jch.gulimall.product.dao.BrandDao;
import com.jch.gulimall.product.dao.CategoryDao;
import com.jch.gulimall.product.entity.BrandEntity;
import com.jch.gulimall.product.entity.CategoryEntity;
import com.jch.gulimall.product.service.BrandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jch.common.utils.PageUtils;
import com.jch.common.utils.Query;

import com.jch.gulimall.product.dao.CategoryBrandRelationDao;
import com.jch.gulimall.product.entity.CategoryBrandRelationEntity;
import com.jch.gulimall.product.service.CategoryBrandRelationService;


@Service("categoryBrandRelationService")
public class CategoryBrandRelationServiceImpl extends ServiceImpl<CategoryBrandRelationDao, CategoryBrandRelationEntity> implements CategoryBrandRelationService {

    @Autowired
    private BrandDao brandDao;

    @Autowired
    private CategoryDao categoryDao;

    @Autowired
    private BrandService brandService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryBrandRelationEntity> page = this.page(
                new Query<CategoryBrandRelationEntity>().getPage(params),
                new QueryWrapper<CategoryBrandRelationEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 保存品牌与分类的详细信息
     * @param categoryBrandRelation
     */
    @Override
    public void saveDetail(CategoryBrandRelationEntity categoryBrandRelation) {
        Long brandId = categoryBrandRelation.getBrandId();
        Long catelogId = categoryBrandRelation.getCatelogId();
        // 查询分类名
        CategoryEntity categoryEntity = categoryDao.selectById(catelogId);
        // 查询品牌名
        BrandEntity brandEntity = brandDao.selectById(brandId);
        categoryBrandRelation.setCatelogName(categoryEntity.getName());
        categoryBrandRelation.setBrandName(brandEntity.getName());

        // 保存品牌与分类的详细信息
        this.save(categoryBrandRelation);
    }

    /**
     * 更新品牌名
     * @param brand
     */
    @Override
    public void updateBrand(BrandEntity brand) {
        CategoryBrandRelationEntity categoryBrandRelationEntity = new CategoryBrandRelationEntity();
        categoryBrandRelationEntity.setBrandId(brand.getBrandId());
        categoryBrandRelationEntity.setBrandName(brand.getName());
        this.update(categoryBrandRelationEntity,
                new QueryWrapper<CategoryBrandRelationEntity>()
                        .eq("brand_id", brand.getBrandId()));
    }

    /**
     * 更新分类名
     * @param category
     */
    @Override
    public void updateCategory(CategoryEntity category) {
        CategoryBrandRelationEntity categoryBrandRelationEntity = new CategoryBrandRelationEntity();
        categoryBrandRelationEntity.setCatelogId(category.getCatId());
        categoryBrandRelationEntity.setCatelogName(category.getName());
        this.update(categoryBrandRelationEntity,
                new QueryWrapper<CategoryBrandRelationEntity>()
                        .eq("catelog_id", category.getCatId()));
    }

    /**
     * 根据分类id获取该分类下的所有品牌
     * @param catId
     * @return
     */
    @Override
    public List<BrandEntity> getBrandListByCatId(Long catId) {
        List<CategoryBrandRelationEntity> categoryBrandRelationEntityList = this.baseMapper.selectList(
                new QueryWrapper<CategoryBrandRelationEntity>()
                        .eq("catelog_id", catId)
        );
        List<BrandEntity> brandEntityList = categoryBrandRelationEntityList.stream()
                .map((categoryBrandRelationEntity) -> {
                    return brandService.getById(categoryBrandRelationEntity.getBrandId());
                })
                .collect(Collectors.toList());
        return brandEntityList;
    }
}
