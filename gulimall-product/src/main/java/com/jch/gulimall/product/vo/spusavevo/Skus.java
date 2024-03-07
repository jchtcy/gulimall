/**
  * Copyright 2020 bejson.com
  */
package com.jch.gulimall.product.vo.spusavevo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 *
 *
 * @author jch
 * @website
 */
@Data
public class Skus {

    private List<Attr> attr;
    private String skuName;
    private BigDecimal price;
    private String skuTitle;
    private String skuSubtitle;
    private List<Images> images;
    private List<String> descar;
    private int fullCount;
    private BigDecimal discount;
    private int countStatus;
    private BigDecimal fullPrice;
    private BigDecimal reducePrice;
    private int priceStatus;
    private List<MemberPrice> memberPrice;

}
