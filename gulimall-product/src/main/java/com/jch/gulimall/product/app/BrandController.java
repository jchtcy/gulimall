package com.jch.gulimall.product.app;

import java.util.Arrays;
import java.util.Map;

import com.jch.common.vallid.AddGroup;
import com.jch.common.vallid.UpdateGroup;
import com.jch.common.vallid.UpdateStatusGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jch.gulimall.product.entity.BrandEntity;
import com.jch.gulimall.product.service.BrandService;
import com.jch.common.utils.PageUtils;
import com.jch.common.utils.R;


/**
 * 品牌
 *
 * @author jch
 * @email jch@gulimall.com
 * @date 2024-02-28 13:17:59
 */
@RestController
@RequestMapping("product/brand")
public class BrandController {
    @Autowired
    private BrandService brandService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = brandService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{brandId}")
    public R info(@PathVariable("brandId") Long brandId){
		BrandEntity brand = brandService.getById(brandId);

        return R.ok().put("brand", brand);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    //@RequiresPermissions("product:brand:save")
    public R save(@Validated({AddGroup.class}) @RequestBody BrandEntity brand){
//    public R save(@Valid @RequestBody BrandEntity brand, BindingResult result){
//        if (result.hasErrors()){
//            Map<String, String> map = new HashMap<>();
//            //1、获取校验的结果
//            result.getFieldErrors().forEach((item)->{
//                //获取到错误提示
//                String message = item.getDefaultMessage();
//                //获取到错误属性的名字
//                String field = item.getField();
//                map.put(field, message);
//            });
//            return R.error().put("data", map);
//        }else{
//            brandService.save(brand);
//        }
        brandService.save(brand);
        return R.ok();
    }


    /**
     * 修改
     */
    @RequestMapping("/update")
    public R update(@Validated({UpdateGroup.class}) @RequestBody BrandEntity brand){
		brandService.updateDetail(brand);

        return R.ok();
    }

    /**
     * 修改状态
     */
    @RequestMapping("/update/status")
    public R updateStatus(@Validated({UpdateStatusGroup.class}) @RequestBody BrandEntity brand){
        brandService.updateById(brand);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R delete(@RequestBody Long[] brandIds){
		brandService.removeByIds(Arrays.asList(brandIds));

        return R.ok();
    }

}
