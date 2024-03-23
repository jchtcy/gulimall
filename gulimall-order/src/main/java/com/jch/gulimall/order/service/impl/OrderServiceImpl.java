package com.jch.gulimall.order.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.jch.common.constant.order.OrderConstant;
import com.jch.common.exception.NoStockException;
import com.jch.common.to.mq.OrderTo;
import com.jch.common.utils.R;
import com.jch.common.vo.auth.MemberResponseVO;
import com.jch.gulimall.order.entity.OrderItemEntity;
import com.jch.common.enume.OrderStatusConstant;
import com.jch.gulimall.order.feign.CartFeignService;
import com.jch.gulimall.order.feign.MemberFeignService;
import com.jch.gulimall.order.feign.ProductFeignService;
import com.jch.gulimall.order.feign.WareFeignService;
import com.jch.gulimall.order.interceptor.LoginUserInterceptor;
import com.jch.gulimall.order.service.OrderItemService;
import com.jch.gulimall.order.vo.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jch.common.utils.PageUtils;
import com.jch.common.utils.Query;
import com.jch.gulimall.order.dao.OrderDao;
import com.jch.gulimall.order.entity.OrderEntity;
import com.jch.gulimall.order.service.OrderService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {

    @Autowired
    private MemberFeignService memberFeignService;

    @Autowired
    private CartFeignService cartFeignService;

    @Autowired
    ThreadPoolExecutor executor;

    @Autowired
    WareFeignService wareFeignService;

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    ProductFeignService productFeignService;

    @Autowired
    OrderItemService orderItemService;

    @Autowired
    RabbitTemplate rabbitTemplate;

    private ThreadLocal<OrderSubmitVo> confirmVoThreadLocal = new ThreadLocal<>();

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 订单确认页
     *
     * @return
     */
    @Override
    public OrderConfirmVo orderConfirm() throws ExecutionException, InterruptedException {
        OrderConfirmVo confirmVo = new OrderConfirmVo();
        // 获取当前登录用户
        MemberResponseVO memberResponseVO = LoginUserInterceptor.loginUser.get();
        // 获得主线程请求域
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        // 1、远程查询收货地址列表
        CompletableFuture<Void> getAddressFuture = CompletableFuture.runAsync(() -> {
            // 将主线程请求域 放在当前线程请求域中
            RequestContextHolder.setRequestAttributes(requestAttributes);
            R addressR = memberFeignService.getAddress(memberResponseVO.getId());
            List<MemberAddressVo> address = addressR.getData("address", new TypeReference<List<MemberAddressVo>>() {
            });
            confirmVo.setMemberAddressVos(address);
        }, executor);
        // 2、远程查询购物车所有所中的购物项
        CompletableFuture<Void> cartFuture = CompletableFuture.runAsync(() -> {
            // 将主线程请求域 放在当前线程请求域中
            RequestContextHolder.setRequestAttributes(requestAttributes);
            R cartItemsR = cartFeignService.getCurrentUserCartItems();
            List<OrderItemVo> items = cartItemsR.getData("cartItems", new TypeReference<List<OrderItemVo>>() {
            });
            confirmVo.setItems(items);
        }, executor).thenRunAsync(() -> {
            List<OrderItemVo> items = confirmVo.getItems();
            List<Long> skuIds = items.stream().map(OrderItemVo::getSkuId).collect(Collectors.toList());
            R r = wareFeignService.getSkusHasStock(skuIds);
            List<SkuStockVo> data = r.getData(new TypeReference<List<SkuStockVo>>() {
            });
            if (data != null) {
                Map<Long, Boolean> map = data.stream().collect(Collectors.toMap(
                        SkuStockVo::getSkuId, SkuStockVo::getHasStock
                ));
                confirmVo.setStocks(map);
            }
        }, executor);
        // 3、查询用户积分
        Integer integration = memberResponseVO.getIntegration();
        confirmVo.setIntegration(integration);
        // 4、其他数据自动计算
        // 5、防重令牌
        String token = UUID.randomUUID().toString().replace("_", "");
        redisTemplate.opsForValue().set(OrderConstant.USER_ORDER_PREFIX + memberResponseVO.getId(), token, 30, TimeUnit.MINUTES);
        confirmVo.setUniqueToken(token);

        CompletableFuture.allOf(getAddressFuture, cartFuture).get();
        return confirmVo;
    }

    /**
     * 下单
     *
     * @param vo
     * @return
     */
    //@GlobalTransactional// 全局事务, seata分布式事务，不适合高并发场景（默认基于AT实现）
    @Transactional
    @Override
    public SubmitOrderResponseVo submitOrder(OrderSubmitVo vo)  throws Exception {
        SubmitOrderResponseVo responseVo = new SubmitOrderResponseVo();
        responseVo.setCode(0);
        // 获取当前登录用户
        MemberResponseVO memberResponseVO = LoginUserInterceptor.loginUser.get();
        confirmVoThreadLocal.set(vo);
        // 验证令牌【令牌的对比和删除必须保证原子性】,通过使用脚本来完成(0：令牌校验失败; 1: 删除成功)
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        String uniqueToken = vo.getUniqueToken();
        Long result = redisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class),
                Arrays.asList(OrderConstant.USER_ORDER_PREFIX + memberResponseVO.getId()), uniqueToken);

        if (result == 0L) {
            // 令牌验证不通过
            responseVo.setCode(1);
            return responseVo;
        } else {
            // 令牌验证通过
            // 1、创建订单
            OrderCreateTo orderCreateTo = orderCreate();
            // 2、验价
            BigDecimal back = orderCreateTo.getPayPrice();
            BigDecimal front = vo.getPayPrice();

            if (Math.abs(back.subtract(front).doubleValue()) < 0.01) {
                // 3、对比成功, 保存订单
                saveOrder(orderCreateTo);
                // 4、库存锁定, 只要有异常回滚订单数据
                OrderEntity order = orderCreateTo.getOrder();
                WareSkuLockVo wareSkuLockVo = new WareSkuLockVo();
                wareSkuLockVo.setOrderSn(order.getOrderSn());
                List<OrderItemVo> locks = orderCreateTo.getItems().stream().map(item -> {
                    OrderItemVo orderItemVo = new OrderItemVo();
                    orderItemVo.setSkuId(item.getSkuId());
                    orderItemVo.setCount(item.getSkuQuantity());
                    orderItemVo.setTitle(item.getSpuName());
                    return orderItemVo;
                }).collect(Collectors.toList());
                wareSkuLockVo.setLocks(locks);
                // 远程锁库存
                R r = wareFeignService.orderLockStock(wareSkuLockVo);
                /**
                 * 为了保证高并发。库存服务自己回滚。可以发消息给库存服务
                 * 库存服务也可以使用自动解锁
                 */
                if (r.getCode() == 0) {
                    // 锁成功了
                    responseVo.setOrder(order);

                    // 模拟异常, 测试分布式事务, 和自动解锁库存
                    //System.out.println(10 / 0);

                    // 订单创建成功时向MQ中 延时队列发送消息
                    rabbitTemplate.convertAndSend("order-event-exchange", "order.create.order", orderCreateTo.getOrder());
                    return responseVo;
                } else {
                    // 锁失败了
                    responseVo.setCode(3);
                    String msg = (String) r.get("msg");
                    throw new NoStockException(msg);
                }
            } else {
                // 金额对比失败
                responseVo.setCode(2);
                return responseVo;
            }
        }
    }

    /**
     * 封装订单实体类对象
     *
     * @return
     */
    private OrderCreateTo orderCreate() {
        OrderCreateTo orderCreateTo = new OrderCreateTo();
        // 1、生成订单号
        String orderSn = IdWorker.getTimeId();
        // 2、构建订单
        OrderEntity orderEntity = buildOrder(orderSn);
        // 3、构建所有订单项
        List<OrderItemEntity> orderItems = buildOrderItems(orderSn);
        // 4、订单价格、积分等信息
        computePrice(orderEntity, orderItems);
        // 5、封装To返回
        orderCreateTo.setOrder(orderEntity);
        orderCreateTo.setItems(orderItems);
        orderCreateTo.setFare(orderEntity.getFreightAmount());
        orderCreateTo.setPayPrice(orderEntity.getPayAmount());
        return orderCreateTo;
    }

    /**
     * 构建订单
     *
     * @param orderSn
     * @return
     */
    private OrderEntity buildOrder(String orderSn) {
        MemberResponseVO memberResponseVO = LoginUserInterceptor.loginUser.get();
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setOrderSn(orderSn);
        orderEntity.setMemberId(memberResponseVO.getId());
        // 2、获取收获地址信息
        OrderSubmitVo orderSubmitVo = confirmVoThreadLocal.get();
        R fareR = wareFeignService.getFare(orderSubmitVo.getAddrId());
        FareVO fareVO = fareR.getData(new TypeReference<FareVO>() {
        });
        // 运费
        orderEntity.setFreightAmount(fareVO.getFare());
        // 收货人信息
        orderEntity.setReceiverCity(fareVO.getAddress().getCity());
        orderEntity.setReceiverDetailAddress(fareVO.getAddress().getDetailAddress());
        orderEntity.setReceiverName(fareVO.getAddress().getName());
        orderEntity.setReceiverPhone(fareVO.getAddress().getPhone());
        orderEntity.setReceiverPostCode(fareVO.getAddress().getPostCode());
        orderEntity.setReceiverProvince(fareVO.getAddress().getProvince());
        orderEntity.setReceiverRegion(fareVO.getAddress().getRegion());
        // 设置订单状态
        orderEntity.setStatus(OrderStatusConstant.OrderStatusEnum.CREATE_NEW.getCode());
        orderEntity.setAutoConfirmDay(OrderStatusConstant.autoConfirmDay);
        // 默认未删除
        orderEntity.setDeleteStatus(0);
        return orderEntity;
    }

    /**
     * 构建所有订单项数据
     *
     * @param
     * @param orderSn
     * @return
     */
    private List<OrderItemEntity> buildOrderItems(String orderSn) {
        R itemsR = cartFeignService.getCurrentUserCartItems();
        List<OrderItemVo> orderItemVos = itemsR.getData("cartItems", new TypeReference<List<OrderItemVo>>() {
        });
        List<OrderItemEntity> itemEntities = new ArrayList<>();
        if (orderItemVos != null && orderItemVos.size() > 0) {
            itemEntities = orderItemVos.stream().map(item -> {
                OrderItemEntity orderItemEntity = buildOrderItem(item);
                orderItemEntity.setOrderSn(orderSn);
                return orderItemEntity;
            }).collect(Collectors.toList());
        }
        return itemEntities;
    }

    /**
     * 构建单个订单项数据
     *
     * @param
     * @return
     */
    private OrderItemEntity buildOrderItem(OrderItemVo orderItemVo) {
        OrderItemEntity orderItemEntity = new OrderItemEntity();
        // 1、spu信息
        Long skuId = orderItemVo.getSkuId();
        R spuInfoR = productFeignService.getSpuInfoBySkuId(skuId);
        SpuInfoVo spuInfoVo = spuInfoR.getData(new TypeReference<SpuInfoVo>() {
        });
        orderItemEntity.setSpuId(spuInfoVo.getId());
        orderItemEntity.setSpuBrand(spuInfoVo.getBrandId().toString());
        orderItemEntity.setSpuName(spuInfoVo.getSpuName());
        orderItemEntity.setCategoryId(spuInfoVo.getCatelogId());
        // 2、sku信息
        orderItemEntity.setSkuId(orderItemVo.getSkuId());
        orderItemEntity.setSkuName(orderItemVo.getTitle());
        orderItemEntity.setSkuPic(orderItemVo.getImage());
        orderItemEntity.setSkuPrice(orderItemVo.getPrice());
        String skuAttrsVals = StringUtils.collectionToDelimitedString(orderItemVo.getSkuAttrValues(), ";");
        orderItemEntity.setSkuAttrsVals(skuAttrsVals);
        orderItemEntity.setSkuQuantity(orderItemVo.getCount());
        // 3、优惠信息忽略
        // 4、积分信息
        orderItemEntity.setGiftGrowth(orderItemVo.getPrice().multiply(new BigDecimal(orderItemVo.getCount().toString())).intValue());
        orderItemEntity.setGiftIntegration(orderItemVo.getPrice().multiply(new BigDecimal(orderItemVo.getCount().toString())).intValue());
        // 6、订单项的价格信息
        orderItemEntity.setPromotionAmount(BigDecimal.ZERO);
        orderItemEntity.setCouponAmount(BigDecimal.ZERO);
        orderItemEntity.setIntegrationAmount(BigDecimal.ZERO);
        // 当前订单项的实际金额
        BigDecimal orign = orderItemEntity.getSkuPrice().multiply(new BigDecimal(orderItemEntity.getSkuQuantity().toString()));
        BigDecimal real = orign.subtract(orderItemEntity.getCouponAmount()).subtract(orderItemEntity.getCouponAmount()).subtract(orderItemEntity.getIntegrationAmount());
        orderItemEntity.setRealAmount(real);
        return orderItemEntity;
    }

    /**
     * 计算订单总额
     *
     * @param orderEntity
     * @param orderItems
     */
    private void computePrice(OrderEntity orderEntity, List<OrderItemEntity> orderItems) {
        BigDecimal total = new BigDecimal("0.0");
        BigDecimal coupon = new BigDecimal("0.0");
        BigDecimal integration = new BigDecimal("0.0");
        BigDecimal promotionAmount = new BigDecimal("0.0");
        Integer gift = 0;
        Integer growth = 0;
        // 1、订单价格相关
        for (OrderItemEntity orderItem : orderItems) {
            total = total.add(orderItem.getRealAmount());
            coupon = coupon.add(orderItem.getCouponAmount());
            integration = integration.add(orderItem.getIntegrationAmount());
            promotionAmount = promotionAmount.add(orderItem.getPromotionAmount());
            gift += orderItem.getGiftIntegration();
            growth += orderItem.getGiftGrowth();
        }
        // 订单总额
        orderEntity.setTotalAmount(total);
        // 应付总额
        orderEntity.setPayAmount(total.add(orderEntity.getFreightAmount()));
        // 优惠金额
        orderEntity.setCouponAmount(coupon);
        orderEntity.setIntegrationAmount(integration);
        orderEntity.setPromotionAmount(promotionAmount);
        // 积分
        orderEntity.setIntegration(gift);
        orderEntity.setGrowth(growth);
    }

    /**
     * 保存订单数据
     *
     * @param orderCreateTo
     */
    private void saveOrder(OrderCreateTo orderCreateTo) {
        OrderEntity order = orderCreateTo.getOrder();
        order.setModifyTime(new Date());
        this.save(order);

        List<OrderItemEntity> items = orderCreateTo.getItems();
        orderItemService.saveBatch(items);
    }

    /**
     * 根据订单号获取订单状态
     * @param orderSn
     * @return
     */
    @Override
    public OrderEntity getOrderStatus(String orderSn) {
        return this.getOne(new QueryWrapper<OrderEntity>().eq("order_sn", orderSn));
    }

    /**
     * 关闭订单
     * @param orderEntity
     */
    @Override
    public void closeOrder(OrderEntity orderEntity) {
        // 查询当前订单的最新状态
        OrderEntity order = this.getById(orderEntity.getId());
        if (order.getStatus() == OrderStatusConstant.OrderStatusEnum.CREATE_NEW.getCode()) {
            order.setStatus(OrderStatusConstant.OrderStatusEnum.CANCLED.getCode());
            this.updateById(order);

            // 给MQ发消息释放库存
            OrderTo orderTo = new OrderTo();
            BeanUtils.copyProperties(order, orderTo);
            rabbitTemplate.convertAndSend("order-event-exchange", "order.release.other", orderTo);
        }
    }
}
