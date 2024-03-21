package com.jch.gulimall.order.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 封装订单提交数据
 */
@Data
public class OrderSubmitVo {
    /**
     * 收货地址id
     */
    private Long addrId;
    /**
     * 支付方式
     */
    private Integer payType;
    /**
     * 防重令牌
     */
    private String uniqueToken;
    /**
     * 应付价格
     */
    private BigDecimal payPrice;
    /**
     * 订单备注
     */
    private String remarks;
}
