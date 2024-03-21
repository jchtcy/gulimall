package com.jch.gulimall.cart.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.jch.common.constant.auth.AuthConstant;
import com.jch.common.constant.cart.CartConstant;
import com.jch.common.utils.R;
import com.jch.gulimall.cart.feign.ProductFeignService;
import com.jch.gulimall.cart.interceptor.CartInterceptor;
import com.jch.gulimall.cart.service.CartService;
import com.jch.gulimall.cart.vo.Cart;
import com.jch.gulimall.cart.vo.CartItem;
import com.jch.gulimall.cart.vo.SkuInfoVo;
import com.jch.gulimall.cart.vo.UserInfoTo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CartServiceImpl implements CartService {

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    ProductFeignService productFeignService;

    @Autowired
    ThreadPoolExecutor executor;

    /**
     * 添加商品到购物车
     * @param skuId
     * @param num
     * @return
     */
    @Override
    public CartItem addToCart(Long skuId, Integer num) throws ExecutionException, InterruptedException {

        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        String res = (String) cartOps.get(skuId.toString());
        if (StringUtils.isEmpty(res)) { // 添加新商品到购物车
            CartItem cartItem = new CartItem();
            // 1、远程查询商品的信息
            CompletableFuture<Void> getSkuInfoTask = CompletableFuture.runAsync(() -> {
                R r = productFeignService.info(skuId);
                SkuInfoVo skuInfo = r.getData("skuInfo", new TypeReference<SkuInfoVo>() {
                });
                cartItem.setCheck(true);
                cartItem.setCount(num);
                cartItem.setImage(skuInfo.getSkuDefaultImg());
                cartItem.setTitle(skuInfo.getSkuTitle());
                cartItem.setSkuId(skuId);
                cartItem.setPrice(skuInfo.getPrice());
            }, executor);
            // 2、远程查询sku的组合信息
            CompletableFuture<Void> getSkuSaleAttrValues = CompletableFuture.runAsync(() -> {
                R r = productFeignService.getSkuSaleAttrValues(skuId);
                List<String> skuAttr = r.getData("skuSaleAttrValues", new TypeReference<List<String>>() {
                });
                cartItem.setSkuAttrValues(skuAttr);
            }, executor);
            // 3、商品添加到购物车
            CompletableFuture.allOf(getSkuInfoTask, getSkuSaleAttrValues).get();
            cartOps.put(skuId.toString(), JSON.toJSONString(cartItem));
            return cartItem;
        } else {// 购物车已有该商品, 修改数量
            CartItem cartItem = JSON.parseObject(res, CartItem.class);
            cartItem.setCount(cartItem.getCount() + num);
            cartOps.put(skuId.toString(), JSON.toJSONString(cartItem));
            return cartItem;
        }
    }

    /**
     * 获取到要操作的购物车
     * @return
     */
    private BoundHashOperations<String, Object, Object> getCartOps() {
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();

        String cartKey = "";
        if (userInfoTo.getUserId() != null) {
            cartKey = CartConstant.CART_PREFIX + userInfoTo.getUserId();
        } else {
            cartKey = CartConstant.CART_PREFIX + userInfoTo.getUserKey();
        }

        return redisTemplate.boundHashOps(cartKey);
    }

    /**
     * 根据商品id查询购物项数据
     * @param skuId
     * @return
     */
    @Override
    public CartItem getCartItem(Long skuId) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        String res = (String) cartOps.get(skuId.toString());
        return JSON.parseObject(res, CartItem.class);
    }

    /**
     * 查询购物车数据
     * @return
     */
    @Override
    public Cart getCart() throws ExecutionException, InterruptedException {
        Cart cart = new Cart();
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        if (userInfoTo.getUserId() != null) {// 登录
            // 如果临时购物车的数据还没合并
            String tempCartKey = CartConstant.CART_PREFIX + userInfoTo.getUserKey();
            List<CartItem> tempCartItems = getCartItems(tempCartKey);
            if (tempCartItems != null) {
                // 临时购物车又数据, 需要合并
                for (CartItem item : tempCartItems) {
                    addToCart(item.getSkuId(), item.getCount());
                }
                // 清除临时购物车数据
                clearCart(tempCartKey);
            }
            // 获取登录后的购物车数据
            String cartKey = CartConstant.CART_PREFIX + userInfoTo.getUserId();
            List<CartItem> cartItems = getCartItems(cartKey);
            cart.setItems(cartItems);
        } else {// 没登录
            String cartKey = CartConstant.CART_PREFIX + userInfoTo.getUserKey();
            List<CartItem> cartItems = getCartItems(cartKey);
            cart.setItems(cartItems);
        }
        return cart;
    }

    private List<CartItem> getCartItems(String cartKey) {
        BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(cartKey);
        List<Object> values = hashOps.values();
        if (values != null && values.size() > 0){
            List<CartItem> cartItems = values.stream().map(obj -> {
                String str = (String) obj;
                return JSON.parseObject(str, CartItem.class);
            }).collect(Collectors.toList());
            return cartItems;
        }
        return null;
    }

    /**
     * 清空购物车
     * @param cartKey
     */
    @Override
    public void clearCart(String cartKey) {
        redisTemplate.delete(cartKey);
    }

    /**
     * 勾选购物项
     * @param skuId
     * @param check
     */
    @Override
    public void checkItem(Long skuId, Integer check) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        CartItem cartItem = getCartItem(skuId);
        cartItem.setCheck(check == 1 ? true : false);
        cartOps.put(skuId.toString(), JSON.toJSONString(cartItem));
    }

    /**
     * 改变商品数量
     * @param skuId
     * @param num
     */
    @Override
    public void changeItemCount(Long skuId, Integer num) {
        CartItem cartItem = getCartItem(skuId);
        cartItem.setCount(num);

        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        cartOps.put(skuId.toString(), JSON.toJSONString(cartItem));
    }

    /**
     * 删除购物项
     * @param skuId
     */
    @Override
    public void deleteItem(Long skuId) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        cartOps.delete(skuId.toString());
    }

    /**
     * 获取当前登录用户所有购物车里的数据
     * @return
     */
    @Override
    public List<CartItem> getCurrentUserCartItems() {
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        if (userInfoTo.getUserId() == null) {
            return null;
        } else {
            List<CartItem> cartItems = getCartItems(CartConstant.CART_PREFIX + userInfoTo.getUserId());
            // 获取所有被选中的购物项
            List<CartItem> result = cartItems.stream()
                    .filter(item -> item.getCheck())
                    .map(item -> {
                        // 更新为最新价格
                        R priceR = productFeignService.getPrice(item.getSkuId());
                        String price = priceR.getData("price", new TypeReference<String>() {});
                        item.setPrice(new BigDecimal(price));
                        return item;
                    })
                    .collect(Collectors.toList());
            return result;
        }
    }
}
