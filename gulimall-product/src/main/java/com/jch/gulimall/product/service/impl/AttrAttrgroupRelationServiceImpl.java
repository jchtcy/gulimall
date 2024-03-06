package com.jch.gulimall.product.service.impl;

import com.jch.gulimall.product.vo.AttrGroupRelationVo;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jch.common.utils.PageUtils;
import com.jch.common.utils.Query;

import com.jch.gulimall.product.dao.AttrAttrgroupRelationDao;
import com.jch.gulimall.product.entity.AttrAttrgroupRelationEntity;
import com.jch.gulimall.product.service.AttrAttrgroupRelationService;


@Service("attrAttrgroupRelationService")
public class AttrAttrgroupRelationServiceImpl extends ServiceImpl<AttrAttrgroupRelationDao, AttrAttrgroupRelationEntity> implements AttrAttrgroupRelationService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<AttrAttrgroupRelationEntity> page = this.page(
                new Query<AttrAttrgroupRelationEntity>().getPage(params),
                new QueryWrapper<AttrAttrgroupRelationEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 添加属性分组与属性关联关系
     * @param attrGroupRelationVoList
     */
    @Override
    public void saveAttrGroupRelationVoBatch(List<AttrGroupRelationVo> attrGroupRelationVoList) {
        List<AttrAttrgroupRelationEntity> attrAttrgroupRelationEntityList = attrGroupRelationVoList.stream()
                .map((attrGroupRelationVo -> {
                    AttrAttrgroupRelationEntity attrAttrgroupRelationEntity = new AttrAttrgroupRelationEntity();
                    BeanUtils.copyProperties(attrGroupRelationVo, attrAttrgroupRelationEntity);
                    return attrAttrgroupRelationEntity;
                })).collect(Collectors.toList());
        this.saveBatch(attrAttrgroupRelationEntityList);
    }
}
