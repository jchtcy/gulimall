package com.jch.gulimall.order.vo;

import lombok.Getter;
import lombok.Setter;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class OrderConfirmVo {
    /**
     * 用户收获地址列表
     */
    @Setter @Getter
    List<MemberAddressVo> memberAddressVos;

    /**
     * 所有选中的购物项
     */
    @Setter @Getter
    List<OrderItemVo> items;

    /**
     * 会员积分
     */
    @Setter @Getter
    Integer integration;

    /**
     * 库存
     * 有货/无货，不放在item里面
     */
    @Getter
    @Setter
    Map<Long, Boolean> stocks;

    /**
     * 订单防重令牌
     */
    @Setter @Getter
    String uniqueToken;

    /**
     * 订单总额
     */
    //BigDecimal total;
    public BigDecimal getTotal() {
        BigDecimal sum = BigDecimal.ZERO;
        if (items != null && items.size() > 0) {
            for (OrderItemVo item : items) {
                sum = sum.add(item.getTotalPrice());
            }
        }
        return sum;
    }

    /**
     * 应付价格
     */
    //BigDecimal payPrice;
    public BigDecimal getPayPrice() {
        return getTotal();
    }

    /**
     * 商品总数
     */
    public Integer getCount() {
        Integer count = 0;
        if (!CollectionUtils.isEmpty(items)) {
            for (OrderItemVo item : items) {
                count += item.getCount();
            }
        }
        return count;
    }
}
