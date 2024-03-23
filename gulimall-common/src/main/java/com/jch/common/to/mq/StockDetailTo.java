package com.jch.common.to.mq;

import lombok.Data;

/**
 * Data time:2022/4/14 20:21
 * StudentID:2019112118
 * Author:hgw
 * Description: 详情单
 */
@Data
public class StockDetailTo {
    private Long id;
    /**
     * sku_id
     */
    private Long skuId;
    /**
     * sku_name
     */
    private String skuName;
    /**
     * 购买个数
     */
    private Integer skuNum;
    /**
     * 工作单id
     */
    private Long taskId;
    /**
     * 仓库id
     */
    private Long wareId;
    /**
     * 锁定状态，1-已锁定  2-已解锁  3-扣减
     */
    private Integer lockStatus;
}

