package com.jch.gulimall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jch.common.utils.PageUtils;
import com.jch.gulimall.product.entity.AttrEntity;
import com.jch.gulimall.product.vo.AttrGroupRelationVo;
import com.jch.gulimall.product.vo.AttrRespVo;
import com.jch.gulimall.product.vo.AttrVo;

import java.util.List;
import java.util.Map;

/**
 * 商品属性
 *
 * @author jch
 * @email jch@gulimall.com
 * @date 2024-02-28 13:17:59
 */
public interface AttrService extends IService<AttrEntity> {

    PageUtils queryPage(Map<String, Object> params);

    /**
     * 保存属性和属性分组关联表
     * @param attr
     */
    void saveAttr(AttrVo attr);

    /**
     * 获取分类规格参数
     *
     * @param params
     * @param catelogId
     * @param attrType
     * @return
     */
    PageUtils queryBaseAttrPage(Map<String, Object> params, Long catelogId, String attrType);

    /**
     * 查询属性详情
     * @param attrId
     * @return
     */
    AttrRespVo getAttrInfo(Long attrId);

    /**
     * 修改属性
     * @param attr
     */
    void updateAttr(AttrVo attr);

    /**
     * 获取属性分组的关联的所有属性
     * @param attrgroupId
     * @return
     */
    List<AttrEntity> getRelationAttr(Long attrgroupId);

    /**
     * 删除属性与分组的关联关系
     * @param attrGroupRelationVos
     */
    void deleteAttrRelation(AttrGroupRelationVo[] attrGroupRelationVos);

    /**
     * 获取属性分组没有关联的其他属性
     * @param params
     * @param attrgroupId
     * @return
     */
    PageUtils getNoRelationAttr(Map<String, Object> params, Long attrgroupId);
}

