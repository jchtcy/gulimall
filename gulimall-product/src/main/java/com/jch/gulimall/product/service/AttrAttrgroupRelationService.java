package com.jch.gulimall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jch.common.utils.PageUtils;
import com.jch.gulimall.product.entity.AttrAttrgroupRelationEntity;
import com.jch.gulimall.product.vo.AttrGroupRelationVo;

import java.util.List;
import java.util.Map;

/**
 * 属性&属性分组关联
 *
 * @author jch
 * @email jch@gulimall.com
 * @date 2024-02-28 13:17:59
 */
public interface AttrAttrgroupRelationService extends IService<AttrAttrgroupRelationEntity> {

    PageUtils queryPage(Map<String, Object> params);

    /**
     * 添加属性分组与属性关联关系
     * @param attrGroupRelationVoList
     */
    void saveAttrGroupRelationVoBatch(List<AttrGroupRelationVo> attrGroupRelationVoList);
}

