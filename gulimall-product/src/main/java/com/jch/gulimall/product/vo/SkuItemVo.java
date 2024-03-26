package com.jch.gulimall.product.vo;

import com.jch.common.to.seckill.SeckillSkuRedisTo;
import com.jch.gulimall.product.entity.SkuImagesEntity;
import com.jch.gulimall.product.entity.SkuInfoEntity;
import com.jch.gulimall.product.entity.SpuInfoDescEntity;
import lombok.Data;

import java.util.List;

@Data
public class SkuItemVo {

    // 1、sku基本信息获取 pms_sku_info
    SkuInfoEntity info;
    // 2、sku的图片信息
    List<SkuImagesEntity> images;
    // 3、获取spu的销售属性组合
    List<SkuItemSaleAttrVo> saleAttr;
    // 4、获取spu的介绍
    SpuInfoDescEntity desc;
    // 5、获取spu规格参数信息
    List<SpuItemGroupAttrVo> groupAttrs;


    private boolean hasStock = true;// 是否有货
    // 6、秒杀
    private SeckillSkuRedisTo seckillSku;

    @Data
    public static class SkuItemSaleAttrVo{
        private Long attrId;
        private String attrName;
        private List<AttrValueWithSkuIdVo> attrValues;
    }

    @Data
    public static class AttrValueWithSkuIdVo {

        private String attrValue;
        private String skuIds;
    }

    @Data
    public static class SpuItemGroupAttrVo{
        private String groupName;
        private List<SpuBaseAttrVo> attrs;
    }

    @Data
    public static class SpuBaseAttrVo{
        private String attrName;
        private String attrValue;
    }
}
