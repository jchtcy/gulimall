package com.jch.gulimall.order.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class FareVO {
    private MemberAddressVo address;
    private BigDecimal fare;
}

