package com.jch.gulimall.coupon.dao;

import com.jch.gulimall.coupon.entity.CouponEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 优惠券信息
 * 
 * @author jch
 * @email jch@gulimall.com
 * @date 2024-02-28 15:45:54
 */
@Mapper
public interface CouponDao extends BaseMapper<CouponEntity> {
	
}
