package com.jch.gulimall.ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jch.common.to.mq.OrderTo;
import com.jch.common.to.mq.StockLockedTo;
import com.jch.common.utils.PageUtils;
import com.jch.gulimall.ware.entity.WareSkuEntity;
import com.jch.gulimall.ware.vo.SkuHasStockVo;
import com.jch.gulimall.ware.vo.WareSkuLockVo;

import java.util.List;
import java.util.Map;

/**
 * 商品库存
 *
 * @author jch
 * @email jch@gulimall.com
 * @date 2024-02-28 16:22:11
 */
public interface WareSkuService extends IService<WareSkuEntity> {

    PageUtils queryPage(Map<String, Object> params);

    /**
     * 入库
     * @param skuId
     * @param wareId
     * @param skuNum
     */
    void addStock(Long skuId, Long wareId, Integer skuNum);

    /**
     * 查询sku是否有库存
     * @param skuIds
     * @return
     */
    List<SkuHasStockVo> getSkusHasStock(List<Long> skuIds);

    /**
     * 锁定库存
     *
     * @param wareSkuLockVo
     * @return
     */
    boolean orderLockStock(WareSkuLockVo wareSkuLockVo);

    /**
     * 解锁库存
     * @param stockLockedTo
     */
    void unlockStock(StockLockedTo stockLockedTo);

    /**
     * 订单服务卡顿时, 导致状态未修改, 库存解锁不了
     * @param orderTo
     */
    void unlockStock(OrderTo orderTo);
}

