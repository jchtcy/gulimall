package com.jch.gulimall.product.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.jch.gulimall.product.entity.ProductAttrValueEntity;
import com.jch.gulimall.product.service.ProductAttrValueService;
import com.jch.gulimall.product.vo.AttrRespVo;
import com.jch.gulimall.product.vo.AttrVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.jch.gulimall.product.entity.AttrEntity;
import com.jch.gulimall.product.service.AttrService;
import com.jch.common.utils.PageUtils;
import com.jch.common.utils.R;



/**
 * 商品属性
 *
 * @author jch
 * @email jch@gulimall.com
 * @date 2024-02-28 13:17:59
 */
@RestController
@RequestMapping("product/attr")
public class AttrController {
    @Autowired
    private AttrService attrService;

    @Autowired
    ProductAttrValueService productAttrValueService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = attrService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{attrId}")
    public R info(@PathVariable("attrId") Long attrId){
		AttrRespVo attrRespVo = attrService.getAttrInfo(attrId);

        return R.ok().put("attr", attrRespVo);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R save(@RequestBody AttrVo attr){
		attrService.saveAttr(attr);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R update(@RequestBody AttrVo attr){
		attrService.updateAttr(attr);

        return R.ok();
    }

    /**
     * 删除属性
     */
    @RequestMapping("/delete")
    public R delete(@RequestBody Long[] attrIds){
		attrService.removeDetailsByIds(Arrays.asList(attrIds));

        return R.ok();
    }

    /**
     * 获取分类规格参数
     */
    //@RequestMapping("/base/list/{catelogId}")
    @RequestMapping("/{attrType}/list/{catelogId}")
    public R list(@RequestParam Map<String, Object> params, @PathVariable("catelogId") Long catelogId,
                  @PathVariable("attrType") String attrType){
        PageUtils page = attrService.queryBaseAttrPage(params, catelogId, attrType);

        return R.ok().put("page", page);
    }

    /**
     * 获取spu规格
     * @param spuId
     * @return
     */
    @GetMapping("/base/listforspu/{spuId}")
    public R baseListforspu(@PathVariable("spuId") Long spuId){
        List<ProductAttrValueEntity> entityList = productAttrValueService.baseAttrlistForSpu(spuId);

        return R.ok().put("data", entityList);
    }

    /**
     * 修改商品规格
     */
    @PostMapping("/update/{spuId}")
    public R updateSpuAttr(@PathVariable("spuId") Long spuId, @RequestBody List<ProductAttrValueEntity> entities){
        productAttrValueService.updateSpuAttr(spuId, entities);

        return R.ok();
    }


}
