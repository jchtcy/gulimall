package com.jch.gulimall.product.service.impl;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jch.common.utils.PageUtils;
import com.jch.common.utils.Query;

import com.jch.gulimall.product.dao.ProductAttrValueDao;
import com.jch.gulimall.product.entity.ProductAttrValueEntity;
import com.jch.gulimall.product.service.ProductAttrValueService;
import org.springframework.transaction.annotation.Transactional;


@Service("productAttrValueService")
public class ProductAttrValueServiceImpl extends ServiceImpl<ProductAttrValueDao, ProductAttrValueEntity> implements ProductAttrValueService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<ProductAttrValueEntity> page = this.page(
                new Query<ProductAttrValueEntity>().getPage(params),
                new QueryWrapper<ProductAttrValueEntity>()
        );

        return new PageUtils(page);
    }

    /**
     *
     * @param spuId
     * @return
     */
    @Override
    public List<ProductAttrValueEntity> baseAttrlistForSpu(Long spuId) {
        List<ProductAttrValueEntity> productAttrValueEntityList = this.baseMapper.selectList(
                new QueryWrapper<ProductAttrValueEntity>().eq("spu_id", spuId)
        );
        return productAttrValueEntityList;
    }

    /**
     * 修改商品规格
     * @param spuId
     * @param entities
     */
    @Transactional
    @Override
    public void updateSpuAttr(Long spuId, List<ProductAttrValueEntity> entities) {
        //1、删除这个spuId对应的所有属性
        this.baseMapper.delete(
                new QueryWrapper<ProductAttrValueEntity>().eq("spu_id", spuId)
        );
        //2、新增回去
        for (ProductAttrValueEntity entity : entities) {
            entity.setSpuId(spuId);
        }
        this.saveBatch(entities);
    }
}
