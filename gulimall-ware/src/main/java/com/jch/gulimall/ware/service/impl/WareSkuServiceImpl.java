package com.jch.gulimall.ware.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.jch.common.enume.OrderStatusConstant;
import com.jch.common.to.mq.OrderTo;
import com.jch.common.to.mq.StockDetailTo;
import com.jch.common.to.mq.StockLockedTo;
import com.jch.common.utils.R;
import com.jch.common.exception.NoStockException;
import com.jch.gulimall.ware.entity.WareOrderTaskDetailEntity;
import com.jch.gulimall.ware.entity.WareOrderTaskEntity;
import com.jch.gulimall.ware.feign.OrderFeignService;
import com.jch.gulimall.ware.feign.ProductFeignService;
import com.jch.gulimall.ware.service.WareOrderTaskDetailService;
import com.jch.gulimall.ware.service.WareOrderTaskService;
import com.jch.gulimall.ware.vo.OrderItemVo;
import com.jch.gulimall.ware.vo.OrderVo;
import com.jch.gulimall.ware.vo.SkuHasStockVo;
import com.jch.gulimall.ware.vo.WareSkuLockVo;
import com.rabbitmq.client.Channel;
import lombok.Data;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jch.common.utils.PageUtils;
import com.jch.common.utils.Query;

import com.jch.gulimall.ware.dao.WareSkuDao;
import com.jch.gulimall.ware.entity.WareSkuEntity;
import com.jch.gulimall.ware.service.WareSkuService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuDao, WareSkuEntity> implements WareSkuService {

    @Autowired
    ProductFeignService productFeignService;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    WareOrderTaskService wareOrderTaskService;

    @Autowired
    WareOrderTaskDetailService wareOrderTaskDetailService;

    @Autowired
    OrderFeignService orderFeignService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        QueryWrapper<WareSkuEntity> queryWrapper = new QueryWrapper<>();
        String wareId = (String) params.get("wareId");
        if (!StringUtils.isEmpty(wareId)) {
            queryWrapper.eq("ware_id", wareId);
        }
        String skuId = (String) params.get("skuId");
        if (!StringUtils.isEmpty(skuId)) {
            queryWrapper.eq("sku_id", skuId);
        }
        IPage<WareSkuEntity> page = this.page(
                new Query<WareSkuEntity>().getPage(params),
                queryWrapper
        );

        return new PageUtils(page);
    }

    /**
     * 入库
     *
     * @param skuId
     * @param wareId
     * @param skuNum
     */
    @Override
    public void addStock(Long skuId, Long wareId, Integer skuNum) {
        WareSkuEntity wareSkuEntity = this.baseMapper.selectOne(
                new QueryWrapper<WareSkuEntity>().eq("sku_id", skuId).eq("ware_id", wareId)
        );

        if (wareSkuEntity == null) {
            // 新增
            wareSkuEntity = new WareSkuEntity();
            wareSkuEntity.setStock(skuNum);
            wareSkuEntity.setStockLocked(0);
            // 远程查询sku的名字, 失败无需回滚
            try {
                R info = productFeignService.info(skuId);
                if (info.getCode() == 0) {
                    Map<String, Object> map = (Map<String, Object>) info.get("skuInfo");
                    wareSkuEntity.setSkuName((String) map.get("skuName"));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            wareSkuEntity.setStock(wareSkuEntity.getStock() + skuNum);
        }

        wareSkuEntity.setSkuId(skuId);
        wareSkuEntity.setWareId(wareId);
        this.saveOrUpdate(wareSkuEntity);
    }

    /**
     * 查询sku是否有库存
     *
     * @param skuIds
     * @return
     */
    @Override
    public List<SkuHasStockVo> getSkusHasStock(List<Long> skuIds) {
        List<SkuHasStockVo> voList = skuIds.stream().map(skuId -> {
            SkuHasStockVo vo = new SkuHasStockVo();
            Long count = baseMapper.getSkuStock(skuId);

            vo.setSkuId(skuId);
            vo.setHasStock(count == null ? false : count > 0);
            return vo;
        }).collect(Collectors.toList());
        return voList;
    }

    /**
     * 锁定库存
     *
     * @param wareSkuLockVo
     * @return
     */
    @Transactional
    @Override
    public boolean orderLockStock(WareSkuLockVo wareSkuLockVo) {
        /**
         * 保存库存工作单详情
         * 追溯
         */
        WareOrderTaskEntity taskEntity = new WareOrderTaskEntity();
        taskEntity.setOrderSn(wareSkuLockVo.getOrderSn());
        wareOrderTaskService.save(taskEntity);

        // 1、找到每个商品在哪个仓库都有库存
        List<OrderItemVo> locks = wareSkuLockVo.getLocks();
        List<SkuWareHasStock> skuWareHasStocks = locks.stream().map(item -> {
            SkuWareHasStock stock = new SkuWareHasStock();
            Long skuId = item.getSkuId();
            stock.setSkuId(skuId);
            stock.setNum(item.getCount());
            // 查询这个商品在哪里有库存
            List<Long> wareId = this.baseMapper.selectWareIdHasSkuStock(skuId);
            stock.setWareId(wareId);
            return stock;
        }).collect(Collectors.toList());
        // 2、锁定库存
        for (SkuWareHasStock hasStock : skuWareHasStocks) {
            Boolean skuLock = false;
            Long skuId = hasStock.getSkuId();
            List<Long> wareIds = hasStock.getWareId();
            if (wareIds == null && wareIds.size() == 0) {
                // 没有足够的库存
                throw new NoStockException(skuId);
            }

            /**
             * 1、如果每一个商品都锁定成功, 将当前商品锁定了几件的工作单记录发给MQ
             * 2、锁定失败。前面保存的工作单信息就回滚了, 即使要解锁记录, 由于去数据库查询不到id, 所以不用解锁。
             */
            for (Long wareId : wareIds) {
                Long count = this.baseMapper.lockSkuStock(skuId, wareId, hasStock.getNum());
                if (count == 1) {
                    skuLock = true;

                    // 保存库存工作单详情
                    WareOrderTaskDetailEntity taskDetailEntity = new WareOrderTaskDetailEntity(
                            null, skuId, "", hasStock.getNum(), taskEntity.getId(), wareId, 1
                    );
                    wareOrderTaskDetailService.save(taskDetailEntity);
                    // 发消息给MQ锁定成功
                    StockLockedTo lockedTo = new StockLockedTo();
                    lockedTo.setId(taskEntity.getId());
                    StockDetailTo stockDetailTo = new StockDetailTo();
                    BeanUtils.copyProperties(taskDetailEntity, stockDetailTo);
                    lockedTo.setDetail(stockDetailTo);
                    rabbitTemplate.convertAndSend("stock-event-exchange", "stock.locked", lockedTo);
                    break;
                } else {
                    // 当前仓库锁定失败
                }
            }
            if (skuLock == false) {
                // 当前商品所有仓库都没有所住
                throw new NoStockException(skuId);
            }
        }
        // 3、全部锁定成功
        return true;
    }

    @Data
    private class SkuWareHasStock {
        private Long skuId;
        private Integer num;
        private List<Long> wareId;
    }

    /**
     * 解锁库存
     *
     * @param stockLockedTo
     */
    @Override
    public void unlockStock(StockLockedTo stockLockedTo) {
        // 库存工作单id
        StockDetailTo detailTo = stockLockedTo.getDetail();
        Long detailId = detailTo.getId();
        /**
         * 解锁
         * 查询数据库关于这个订单的锁定库存信息
         *      有: 库存锁定成功
         *          如果没有这个订单, 说明订单回滚了, 解锁库存
         *          如果有这个订单
         *              如果订单状态 已取消 解锁库存
         *              如果订单状态 未取消 不能解锁
         *      没有: 库存锁定失败, 库存回滚。无需解锁
         */
        // 查询是否锁定了库存
        WareOrderTaskDetailEntity detail = wareOrderTaskDetailService.getById(detailId);
        if (detail != null) {
            // 锁定库存成功
            // 查询订单状态
            Long id = stockLockedTo.getId();
            WareOrderTaskEntity taskEntity = wareOrderTaskService.getById(id);
            String orderSn = taskEntity.getOrderSn();
            R orderR = orderFeignService.getOrderStatus(orderSn);
            if (orderR.getCode() == 0) {
                // 订单数据返回成功
                OrderVo orderVo = orderR.getData("order", new TypeReference<OrderVo>() {
                });
                if ((orderVo == null || orderVo.getStatus() == OrderStatusConstant.OrderStatusEnum.CANCLED.getCode()) && detail.getLockStatus() == 1) {
                    // 订单回滚或订单取消或已被回滚, 解锁库存
                    unLockStock(detail.getSkuId(), detail.getWareId(), detail.getSkuNum(), detailId);
                }
            } else {
                throw new RuntimeException("远程调用失败");
            }
        }
    }

    /**
     * 解锁库存的sql
     *
     * @param skuId
     * @param wareId
     * @param num
     * @param taskDetailId
     */
    private void unLockStock(Long skuId, Long wareId, Integer num, Long taskDetailId) {
        // 库存解锁
        this.baseMapper.unLockStock(skuId, wareId, num);
        // 更新库存工作单状态
        WareOrderTaskDetailEntity detail = new WareOrderTaskDetailEntity();
        detail.setId(taskDetailId);
        detail.setLockStatus(2);
        wareOrderTaskDetailService.updateById(detail);
    }

    /**
     * 订单服务卡顿时, 导致状态未修改, 库存解锁不了
     * @param orderTo
     */
    @Transactional
    @Override
    public void unlockStock(OrderTo orderTo) {
        String orderSn = orderTo.getOrderSn();
        // 查询库存工作单
        WareOrderTaskEntity wareOrderTaskEntity = wareOrderTaskService.getOrderTaskByOrderSn(orderSn);
        Long taskId = wareOrderTaskEntity.getId();
        // 查询未被解锁的库存工作单
        List<WareOrderTaskDetailEntity> taskDetailEntityList = wareOrderTaskDetailService.list(new QueryWrapper<WareOrderTaskDetailEntity>()
                .eq("task_id", taskId)
                .eq("lock_status", 1));
        for (WareOrderTaskDetailEntity detail : taskDetailEntityList) {
            unLockStock(detail.getSkuId(), detail.getWareId(), detail.getSkuNum(), detail.getId());
        }
    }
}
