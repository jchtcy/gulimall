package com.jch.gulimall.order.dao;

import com.jch.gulimall.order.entity.OrderEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 订单
 *
 * @author jch
 * @email jch@gulimall.com
 * @date 2024-02-28 16:15:49
 */
@Mapper
public interface OrderDao extends BaseMapper<OrderEntity> {

    /**
     * 支付成功修改订单状态
     * @param orderSn
     * @param status
     */
    void updateOrderStatus(@Param("orderSn") String orderSn, @Param("status") Integer status);
}
