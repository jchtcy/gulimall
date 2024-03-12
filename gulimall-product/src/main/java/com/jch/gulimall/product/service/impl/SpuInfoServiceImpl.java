package com.jch.gulimall.product.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.jch.common.constant.product.SpuConstant;
import com.jch.common.to.SkuHasStockVo;
import com.jch.common.to.SkuReductionTo;
import com.jch.common.to.SpuBoundTo;
import com.jch.common.to.es.SkuEsModel;
import com.jch.common.utils.R;
import com.jch.gulimall.product.entity.*;
import com.jch.gulimall.product.feign.CouponFeignService;
import com.jch.gulimall.product.feign.SearchFeignService;
import com.jch.gulimall.product.feign.WareFeignService;
import com.jch.gulimall.product.service.*;
import com.jch.gulimall.product.vo.spusavevo.*;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jch.common.utils.PageUtils;
import com.jch.common.utils.Query;

import com.jch.gulimall.product.dao.SpuInfoDao;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


@Service("spuInfoService")
public class SpuInfoServiceImpl extends ServiceImpl<SpuInfoDao, SpuInfoEntity> implements SpuInfoService {

    @Autowired
    SpuInfoDescService spuInfoDescService;

    @Autowired
    SpuImagesService spuImagesService;

    @Autowired
    AttrService attrService;

    @Autowired
    ProductAttrValueService productAttrValueService;

    @Autowired
    SkuInfoService skuInfoService;

    @Autowired
    SkuImagesService skuImagesService;

    @Autowired
    SkuSaleAttrValueService skuSaleAttrValueService;

    @Autowired
    CouponFeignService couponFeignService;

    @Autowired
    BrandService brandService;

    @Autowired
    CategoryService categoryService;

    @Autowired
    WareFeignService wareFeignService;

    @Autowired
    SearchFeignService searchFeignService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(params),
                new QueryWrapper<SpuInfoEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 新增商品
     *
     * @param spuSaveVO
     */
    @Transactional
    @Override
    public void saveSpuInfo(SpuSaveVO spuSaveVO) {
        // 1、保存spu基本信息pms_spu_info
        SpuInfoEntity spuInfoEntity = new SpuInfoEntity();
        BeanUtils.copyProperties(spuSaveVO, spuInfoEntity);
        spuInfoEntity.setCreateTime(new Date());
        spuInfoEntity.setUpdateTime(new Date());
        this.saveBaseSpuInfo(spuInfoEntity);
        // 2、保存spu的描述图片pms_spu_info_desc
        List<String> decriptList = spuSaveVO.getDecript();
        SpuInfoDescEntity spuInfoDescEntity = new SpuInfoDescEntity();
        spuInfoDescEntity.setSpuId(spuInfoEntity.getId());
        spuInfoDescEntity.setDecript(String.join(",", decriptList));
        spuInfoDescService.save(spuInfoDescEntity);
        // 3、保存spu的图片集pms_spu_images
        List<String> images = spuSaveVO.getImages();
        if (images != null && images.size() > 0) {
            List<SpuImagesEntity> spuImagesEntityList = images.stream().map(image -> {
                SpuImagesEntity spuImagesEntity = new SpuImagesEntity();
                spuImagesEntity.setSpuId(spuInfoEntity.getId());
                spuImagesEntity.setImgUrl(image);
                return spuImagesEntity;
            }).collect(Collectors.toList());
            spuImagesService.saveBatch(spuImagesEntityList);
        }
        // 4、保存spu的规格参数pms_product_attr_value
        List<BaseAttrs> baseAttrsList = spuSaveVO.getBaseAttrs();
        List<ProductAttrValueEntity> productAttrValueEntityList = baseAttrsList.stream().map(baseAttr -> {
            ProductAttrValueEntity productAttrValueEntity = new ProductAttrValueEntity();

            productAttrValueEntity.setAttrId(baseAttr.getAttrId());

            AttrEntity attrEntity = attrService.getById(baseAttr.getAttrId());
            productAttrValueEntity.setAttrName(attrEntity.getAttrName());

            productAttrValueEntity.setAttrValue(baseAttr.getAttrValues());
            productAttrValueEntity.setQuickShow(baseAttr.getShowDesc());

            productAttrValueEntity.setSpuId(spuInfoEntity.getId());
            return productAttrValueEntity;

        }).collect(Collectors.toList());
        productAttrValueService.saveBatch(productAttrValueEntityList);
        // 5、保存spu的积分信息gulimall_sms->sms_spu_bounds
        Bounds bounds = spuSaveVO.getBounds();
        SpuBoundTo spuBoundTo = new SpuBoundTo();
        BeanUtils.copyProperties(bounds, spuBoundTo);
        spuBoundTo.setSpuId(spuInfoEntity.getId());
        R r = couponFeignService.saveSpuBounds(spuBoundTo);
        if (r.getCode() != 0) {
            log.error("远程保存spu积分信息失败");
        }
        // 6、保存spu对应所有sku信息
        /**
         * sku的基本信息pms_sku_info
         * sku的图片信息pms_sku_images
         * sku的销售属性信息pms_sku_sale_attr_value
         * sku的优惠、满减等信息gulimall_sms->sms_sku_ladder/sms_sku_full_reduction/sms_member_price
         */
        List<Skus> skus = spuSaveVO.getSkus();
        if (skus != null && skus.size() > 0) {
            skus.forEach(sku -> {
                // 1、sku的基本信息pms_sku_info
                // 寻找默认图片
                String deaultImg = "";
                for (Images image : sku.getImages()) {
                    if (image.getDefaultImg() == 1) {
                        deaultImg = image.getImgUrl();
                    }
                }

                SkuInfoEntity skuInfoEntity = new SkuInfoEntity();
                BeanUtils.copyProperties(sku, skuInfoEntity);
                skuInfoEntity.setBrandId(spuInfoEntity.getBrandId());
                skuInfoEntity.setCatalogId(spuInfoEntity.getCatelogId());
                skuInfoEntity.setSaleCount(0L);
                skuInfoEntity.setSpuId(spuInfoEntity.getId());
                skuInfoEntity.setSkuDefaultImg(deaultImg);
                skuInfoService.save(skuInfoEntity);

                // 2、sku的图片信息pms_sku_images
                Long skuId = skuInfoEntity.getSkuId();
                List<SkuImagesEntity> skuImagesEntityList = sku.getImages().stream().map(img -> {
                            SkuImagesEntity skuImagesEntity = new SkuImagesEntity();
                            skuImagesEntity.setSkuId(skuId);
                            skuImagesEntity.setImgUrl(img.getImgUrl());
                            skuImagesEntity.setDefaultImg(img.getDefaultImg());
                            return skuImagesEntity;
                        }).filter(img -> !StringUtils.isEmpty(img.getImgUrl()))
                        .collect(Collectors.toList());
                skuImagesService.saveBatch(skuImagesEntityList);

                // 3、sku的销售属性信息pms_sku_sale_attr_value
                List<Attr> attrList = sku.getAttr();
                List<SkuSaleAttrValueEntity> skuSaleAttrValueEntityList = attrList.stream().map(attr -> {
                    SkuSaleAttrValueEntity skuSaleAttrValueEntity = new SkuSaleAttrValueEntity();
                    BeanUtils.copyProperties(attr, skuSaleAttrValueEntity);
                    skuSaleAttrValueEntity.setSkuId(skuId);
                    return skuSaleAttrValueEntity;
                }).collect(Collectors.toList());
                skuSaleAttrValueService.saveBatch(skuSaleAttrValueEntityList);
                // 4、sku的优惠、满减等信息gulimall_sms->sms_sku_ladder/sms_sku_full_reduction/sms_member_price
                SkuReductionTo skuReductionTo = new SkuReductionTo();
                BeanUtils.copyProperties(sku, skuReductionTo);
                skuReductionTo.setSkuId(skuId);
                if (skuReductionTo.getFullCount() <= 0 || skuReductionTo.getFullPrice().compareTo(new BigDecimal("0")) > 0) {
                    R r2 = couponFeignService.saveSkuReduction(skuReductionTo);
                    if (r2.getCode() != 0) {
                        log.error("远程保存sku优惠信息失败");
                    }
                }
            });
        }

    }

    /**
     * 保存保存spu基本信息
     *
     * @param spuInfoEntity
     */
    @Override
    public void saveBaseSpuInfo(SpuInfoEntity spuInfoEntity) {
        this.baseMapper.insert(spuInfoEntity);
    }

    /**
     * spu检索
     *
     * @param params
     * @return
     */
    @Override
    public PageUtils queryPageByCondition(Map<String, Object> params) {
        QueryWrapper<SpuInfoEntity> queryWrapper = new QueryWrapper<>();

        String key = (String) params.get("key");
        if (!StringUtils.isEmpty(key)) {
            queryWrapper.and(w -> {
                w.eq("id", key).or().like("spu_name", key);
            });
        }

        String status = (String) params.get("status");
        if (!StringUtils.isEmpty(status)) {
            queryWrapper.eq("publish_status", status);
        }

        String brandId = (String) params.get("brandId");
        if (!StringUtils.isEmpty(brandId) && !"0".equalsIgnoreCase(brandId)) {
            queryWrapper.eq("brand_id", brandId);
        }

        String catelogId = (String) params.get("catelogId");
        if (!StringUtils.isEmpty(catelogId) && !"0".equalsIgnoreCase(catelogId)) {
            queryWrapper.eq("catelog_id", catelogId);
        }
        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(params),
                queryWrapper
        );
        return new PageUtils(page);
    }

    /**
     * 商品上架
     *
     * @param spuId
     */
    @Override
    public void up(Long spuId) {
        //查出当前spuid对应所有sku信息, 品牌名字
        List<SkuInfoEntity> skuInfoEntityList = skuInfoService.getSkusBySpuId(spuId);
        List<Long> skuIds = skuInfoEntityList.stream().map(SkuInfoEntity::getSkuId).collect(Collectors.toList());
        // 查询当前sku的所有可以被检索的规格属性
        List<ProductAttrValueEntity> baseAttrs = productAttrValueService.baseAttrlistForSpu(spuId);
        List<Long> attrIds = baseAttrs.stream().map(attr -> {
            return attr.getAttrId();
        }).collect(Collectors.toList());
        List<Long> searchAttrIds = attrService.selectSearchAttrIds(attrIds);
        Set<Long> idSet = new HashSet<>(searchAttrIds);
        List<SkuEsModel.Attrs> attrs = baseAttrs.stream()
                .filter(attr -> {
                    return idSet.contains(attr.getAttrId());
                })
                .map(item -> {
                    SkuEsModel.Attrs attr = new SkuEsModel.Attrs();
                    BeanUtils.copyProperties(item, attr);
                    return attr;
                })
                .collect(Collectors.toList());

        // 远程调用查询是否有库存
        Map<Long, Boolean> stockMap = null;
        try {
            R r = wareFeignService.getSkusHasStock(skuIds);

            TypeReference<List<SkuHasStockVo>> typeReference = new TypeReference<List<SkuHasStockVo>>() {
            };

            stockMap = r.getData(typeReference).stream().collect(Collectors.toMap(
                    SkuHasStockVo::getSkuId,
                    SkuHasStockVo::getHasStock
            ));

        } catch (Exception e) {
            log.error("库存服务查询异常: 原有{}", e);
        }

        // 封装每个sku的信息
        Map<Long, Boolean> finalStockMap = stockMap;
        List<SkuEsModel> skuEsModelList = skuInfoEntityList.stream().map(sku -> {
            // 组装需要的数据
            SkuEsModel skuEsModel = new SkuEsModel();
            BeanUtils.copyProperties(sku, skuEsModel);
            skuEsModel.setSkuPrice(sku.getPrice());
            skuEsModel.setSkuImg(sku.getSkuDefaultImg());

            // 从远程调用中获取库存
            if (finalStockMap == null) {
                skuEsModel.setHasStock(true);
            } else {
                skuEsModel.setHasStock(finalStockMap.get(sku.getSkuId()));
            }
            skuEsModel.setHotScore(0L);
            // 查询品牌的名字和图片
            BrandEntity brandEntity = brandService.getById(sku.getBrandId());
            skuEsModel.setBrandName(brandEntity.getName());
            skuEsModel.setBrandImg(brandEntity.getLogo());
            // 查询分类名
            CategoryEntity categoryEntity = categoryService.getById(sku.getCatalogId());
            skuEsModel.setCatalogName(categoryEntity.getName());
            // 属性
            skuEsModel.setAttrs(attrs);
            return skuEsModel;
        }).collect(Collectors.toList());

        // 将数据发送给es保存
        R r = searchFeignService.productStatusUp(skuEsModelList);
        if (r.getCode() == 0) {// 远程调用成功
            // 修改spu状态
            this.baseMapper.updateSpuStatus(spuId, SpuConstant.PublishStatusEnum.SPU_UP.getCode());
        } else {// 远程调用失败
            // 重复调用 接口幂等性
        }
    }
}
