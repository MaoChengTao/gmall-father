package com.atguigu.gmall.cart.service;

import com.atguigu.gmall.model.cart.CartInfo;

import java.util.List;

public interface CartService {
    /**
     * 加入购物车
     * 加入购物车的请求：'http://cart.gmall.com/addCart.html?skuId=' + this.skuId + '&skuNum=' + this.skuNum
     * 加入购物车需要参数： 商品Id、用户Id(隐含参数)、商品数量
     * 此数据接口供 feign 远程调用 将数据发送到 web-all 中的 addCart.html 控制器
     */
    void addToCart(Long skuId, String userId, Integer skuNum);

    /**
     * 通过用户Id 查询购物车列表
     * @param userId
     * @param userTempId
     * @return
     */
    List<CartInfo> getCartList(String userId, String userTempId);

    /**
     * 选中状态变更
     * @param userId
     * @param isChecked
     * @param skuId
     */
    void checkCart(String userId, Integer isChecked, Long skuId);

    /**
     * 删除购物车数据
     * @param userId
     * @param skuId
     */
    void deleteCartInfo(String userId, Long skuId);

    /**
     * 根据 用户Id 查询购物车中选择的商品列表
     * @param userId
     * @return
     */
    List<CartInfo> getCartCheckedList(String userId);

    /**
     * 通过 userId 查询数据库中购物车数据并放入缓存
     *
     * @param userId
     * @return
     */
    List<CartInfo> loadCartCache(String userId);
}
