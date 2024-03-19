package com.jch.gulimall.cart.service;

import com.jch.gulimall.cart.vo.Cart;
import com.jch.gulimall.cart.vo.CartItem;

import java.util.concurrent.ExecutionException;

public interface CartService {
    /**
     * 添加商品到购物车
     * @param skuId
     * @param num
     * @return
     */
    CartItem addToCart(Long skuId, Integer num) throws ExecutionException, InterruptedException;

    /**
     * 根据商品id查询购物项数据
     * @param skuId
     * @return
     */
    CartItem getCartItem(Long skuId);

    /**
     * 查询购物车数据
     * @return
     */
    Cart getCart() throws ExecutionException, InterruptedException;

    /**
     * 清空购物车
     * @param cartKey
     */
    public void clearCart(String cartKey);

    /**
     * 勾选购物项
     * @param skuId
     * @param check
     */
    void checkItem(Long skuId, Integer check);

    /**
     * 改变商品数量
     * @param skuId
     * @param num
     */
    void changeItemCount(Long skuId, Integer num);

    /**
     * 删除购物项
     * @param skuId
     */
    void deleteItem(Long skuId);
}
