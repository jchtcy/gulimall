package com.jch.gulimall.order.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 购物车里的购物项
 */
@Data
public class OrderItemVo {

    /**
     * 商品id
     */
    private Long skuId;
    /**
     * 标题
     */
    private String title;
    /**
     * 图片
     */
    private String image;
    /**
     * 商品属性
     */
    private List<String> skuAttrValues;
    /**
     * 价格
     */
    private BigDecimal price;
    /**
     * 数量
     */
    private Integer count;
    /**
     * 总价
     */
    private BigDecimal totalPrice;
    /**
     * 商品重量
     */
    private BigDecimal weight = new BigDecimal("0.085");
}
