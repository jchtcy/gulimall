package com.jch.gulimall.product.service.impl;

import com.jch.gulimall.product.dao.AttrAttrgroupRelationDao;
import com.jch.gulimall.product.entity.AttrAttrgroupRelationEntity;
import com.jch.gulimall.product.entity.AttrEntity;
import com.jch.gulimall.product.service.AttrService;
import com.jch.gulimall.product.vo.AttrGroupWithAttrsVo;
import com.jch.gulimall.product.vo.SkuItemVo;
import org.springframework.beans.BeanUtils;
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

import com.jch.gulimall.product.dao.AttrGroupDao;
import com.jch.gulimall.product.entity.AttrGroupEntity;
import com.jch.gulimall.product.service.AttrGroupService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


@Service("attrGroupService")
public class AttrGroupServiceImpl extends ServiceImpl<AttrGroupDao, AttrGroupEntity> implements AttrGroupService {

    @Autowired
    private AttrAttrgroupRelationDao attrAttrgroupRelationDao;

    @Autowired
    private AttrService attrService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<AttrGroupEntity> page = this.page(
                new Query<AttrGroupEntity>().getPage(params),
                new QueryWrapper<AttrGroupEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 根据三级分类id获取分类属性分组
     * @param params
     * @param catelogId 如果不传默认为0查所有
     * @return
     */
    @Override
    public PageUtils queryPage(Map<String, Object> params, Long catelogId) {
        String key = (String) params.get("key");
        QueryWrapper<AttrGroupEntity> wrapper = new QueryWrapper<AttrGroupEntity>();
        if (!StringUtils.isEmpty(key)) {
            wrapper.and((obj) -> {
                obj.eq("attr_group_id", key).or().like("attr_group_name", key);
            });
        }

        if (catelogId == 0) {
            IPage<AttrGroupEntity> page = this.page(new Query<AttrGroupEntity>().getPage(params),
                    wrapper);
            return new PageUtils(page);
        } else {
            wrapper.eq("catelog_id" , catelogId);
            IPage<AttrGroupEntity> page = this.page(new Query<AttrGroupEntity>().getPage(params),
                    wrapper);
            return new PageUtils(page);
        }
    }

    /**
     * 删除属性分类表时, 删除属性分类和属性关联表
     * @param attrGroupList
     */
    @Transactional
    @Override
    public void removeDetailsByIds(List<Long> attrGroupList) {
        // 删除属性分类表
        this.baseMapper.deleteBatchIds(attrGroupList);

        // 删除属性和属性分类关联表
        attrAttrgroupRelationDao.delete(
                new QueryWrapper<AttrAttrgroupRelationEntity>()
                        .in("attr_group_id", attrGroupList)
        );
    }

    /**
     * 获取分类下所有分组&关联属性
     * @param catelogId
     * @return
     */
    @Override
    public List<AttrGroupWithAttrsVo> getAttrGroupWithAttrListByCatelogId(Long catelogId) {
        //查出当前分类下的所有属性分组
        List<AttrGroupEntity> attrGroupEntityList = this.baseMapper.selectList(
                new QueryWrapper<AttrGroupEntity>()
                        .eq("catelog_id", catelogId)
        );
        List<AttrGroupWithAttrsVo> attrGroupWithAttrsVoList = attrGroupEntityList.stream()
                .map(attrGroupEntity -> {
                    AttrGroupWithAttrsVo attrGroupWithAttrsVo = new AttrGroupWithAttrsVo();
                    BeanUtils.copyProperties(attrGroupEntity, attrGroupWithAttrsVo);
                    //查出该属性分组的所有属性
                    List<AttrEntity> attrs = attrService.getRelationAttr(attrGroupEntity.getAttrGroupId());
                    attrGroupWithAttrsVo.setAttrs(attrs);
                    return attrGroupWithAttrsVo;
                })
                .collect(Collectors.toList());

        return attrGroupWithAttrsVoList;
    }

    /**
     * 获取spu规格参数信息
     *
     * @param spuId
     * @param catalogId
     * @return
     */
    @Override
    public List<SkuItemVo.SpuItemGroupAttrVo> getAttrGroupWithSpuId(Long spuId, Long catalogId) {
        // 1、查询当前spu对应的所有属性的分组信息, 以及当前分组下的所有属性对应的值
        return this.baseMapper.getAttrGroupWithSpuId(spuId, catalogId);
    }
}
