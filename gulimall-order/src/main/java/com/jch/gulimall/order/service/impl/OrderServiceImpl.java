package com.jch.gulimall.order.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.jch.common.constant.order.OrderConstant;
import com.jch.common.utils.R;
import com.jch.common.vo.auth.MemberResponseVO;
import com.jch.gulimall.order.feign.CartFeignService;
import com.jch.gulimall.order.feign.MemberFeignService;
import com.jch.gulimall.order.feign.WareFeignService;
import com.jch.gulimall.order.interceptor.LoginUserInterceptor;
import com.jch.gulimall.order.vo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
                }, executor)
                .thenRunAsync(() -> {
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
     * @param vo
     * @return
     */
    @Override
    public SubmitOrderResponseVo submitOrder(OrderSubmitVo vo) {
        SubmitOrderResponseVo responseVo = new SubmitOrderResponseVo();
        // 获取当前登录用户
        MemberResponseVO memberResponseVO = LoginUserInterceptor.loginUser.get();
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
            return responseVo;
        }
    }
}
