package com.jch.gulimall.order.vo;

import com.jch.gulimall.order.entity.OrderEntity;
import lombok.Data;

@Data
public class SubmitOrderResponseVo {

    private OrderEntity order;

    private Integer code;
}
