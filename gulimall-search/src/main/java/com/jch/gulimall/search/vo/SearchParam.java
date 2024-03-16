package com.jch.gulimall.search.vo;

import lombok.Data;

import java.util.List;

/**
 * 封装页面所有可能传递过来的查询条件
 */
@Data
public class SearchParam {

    /**
     * 页面传递过来的全文匹配关键字
     */
    private String keyword;

    /**
     * 三级分类id
     */
    private Long catalog3Id;

    /**
     * 排序条件
     * sort=saleCount_asc/desc
     * sort=skuPrice_asc/desc
     * sort=hotScore_asc/desc
     */
    private String sort;

    // 下面是过滤条件
    /**
     * hasStock(是否有货)=0/1
     */
    private Integer hasStock = 1;
    /**
     * skuPrice(价格区间)=1_500/_500/500_
     */
    private String skuPrice;
    /**
     * 品牌
     */
    private List<Long> brandId;

    /**
     * 按照属性进行筛选
     */
    private List<String> attrs;

    /**
     * 页码
     */
    private Integer pageNum = 1;

    private String _queryString;// 原生的所有查询条件（来自url的请求参数），用于构建面包屑
}
