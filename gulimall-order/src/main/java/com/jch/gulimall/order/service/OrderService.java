package com.jch.gulimall.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jch.common.to.mq.SeckillOrderTo;
import com.jch.common.utils.PageUtils;
import com.jch.common.vo.order.PayAsyncVO;
import com.jch.common.vo.order.PayVO;
import com.jch.gulimall.order.entity.OrderEntity;
import com.jch.gulimall.order.vo.OrderConfirmVo;
import com.jch.gulimall.order.vo.OrderSubmitVo;
import com.jch.gulimall.order.vo.SubmitOrderResponseVo;

import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * 订单
 *
 * @author jch
 * @email jch@gulimall.com
 * @date 2024-02-28 16:15:49
 */
public interface OrderService extends IService<OrderEntity> {

    PageUtils queryPage(Map<String, Object> params);

    /**
     * 订单确认页
     * @return
     */
    OrderConfirmVo orderConfirm() throws ExecutionException, InterruptedException;

    /**
     * 下单
     * @param vo
     * @return
     */
    SubmitOrderResponseVo submitOrder(OrderSubmitVo vo)  throws Exception ;

    /**
     * 根据订单号获取订单状态
     * @param orderSn
     * @return
     */
    OrderEntity getOrderStatus(String orderSn);

    /**
     * 关闭订单
     * @param orderEntity
     */
    void closeOrder(OrderEntity orderEntity);

    /**
     * 根据订单编号获取支付所需要的信息
     * @param orderSn
     * @return
     */
    PayVO getOrderPayInfo(String orderSn);

    /**
     * 分页查询当前登录用户的所有订单信息
     * @param params
     * @return
     */
    PageUtils queryPageWithItem(Map<String, Object> params);

    /**
     * 处理支付宝的支付结果
     * @param vo
     * @return
     */
    String handlePayResult(PayAsyncVO vo);

    /**
     * 创建秒杀单
     * @param seckillOrderTo
     */
    void createSeckillOrder(SeckillOrderTo seckillOrderTo);
}

