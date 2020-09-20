package com.atguigu.gmall.cart.controller;

import com.atguigu.gmall.cart.service.CartService;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.util.AuthContextHolder;
import com.atguigu.gmall.model.cart.CartInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("api/cart")
public class CartApiController {

    @Autowired
    private CartService cartService;

    /**
     * 加入购物车
     * 加入购物车的请求：'http://cart.gmall.com/addCart.html?skuId=' + this.skuId + '&skuNum=' + this.skuNum
     * 加入购物车需要参数： 商品Id、用户Id(隐含参数)、商品数量
     * 此数据接口供 feign 远程调用 将数据发送到 web-all 中的 addCart.html 控制器
     */
    @PostMapping("addToCart/{skuId}/{skuNum}")
    public Result addToCart(@PathVariable Long skuId,
                            @PathVariable Integer skuNum,
                            HttpServletRequest request) {
        // 网关模块将 用户Id和 临时用户Id 保存到 header 从网关获取用户Id和临时用户Id

        // 获取 userId [已登录]
        String userId = AuthContextHolder.getUserId(request);

        // 获取 userTempId [未登录]
        if (StringUtils.isEmpty(userId)) {// userId 为空说明未登录
            userId = AuthContextHolder.getUserTempId(request);
        }

        cartService.addToCart(skuId, userId, skuNum);

        return Result.ok();
    }

    /**
     * 查询购物车
     *
     * @param request
     * @return
     */
    @GetMapping("cartList")
    public Result cartList(HttpServletRequest request) {
        // 网关模块将 用户Id和 临时用户Id 保存到 header 从网关获取用户Id和临时用户Id

        // 获取 userId
        String userId = AuthContextHolder.getUserId(request);

        // 获取 userTempId
        String userTempId = AuthContextHolder.getUserTempId(request);

        List<CartInfo> cartList = cartService.getCartList(userId, userTempId);

        return Result.ok(cartList);
    }

    /**
     * 更新选中状态
     *
     * @param skuId
     * @param isChecked
     * @param request
     * @return
     */
    @GetMapping("checkCart/{skuId}/{isChecked}")
    public Result checkCart(@PathVariable("skuId") Long skuId,
                            @PathVariable("isChecked") Integer isChecked,
                            HttpServletRequest request) {

        // 获取 userId [已登录]
        String userId = AuthContextHolder.getUserId(request);

        // 获取 userTempId [未登录]
        if (StringUtils.isEmpty(userId)) {
            userId = AuthContextHolder.getUserTempId(request);
        }

        // 调用更新选择状态的方法
        cartService.checkCart(userId, isChecked, skuId);

        return Result.ok();
    }


    /**
     * 删除购物车
     *
     * @param skuId
     * @param request
     * @return
     */
    @DeleteMapping("deleteCart/{skuId}")
    public Result deleteCart(@PathVariable("skuId") Long skuId, HttpServletRequest request) {
        // 获取 userId [已登录]
        String userId = AuthContextHolder.getUserId(request);

        // 获取 userTempId [未登录]
        if (StringUtils.isEmpty(userId)) {
            userId = AuthContextHolder.getUserTempId(request);
        }

        cartService.deleteCartInfo(userId, skuId);

        return Result.ok();
    }

    /**
     * 根据 用户Id 查询购物车中选择的商品列表
     *
     * @param userId
     * @return
     */
    @GetMapping("getCartCheckedList/{userId}")
    public List<CartInfo> getCartCheckedList(@PathVariable String userId) {
        return cartService.getCartCheckedList(userId);
    }

    /**
     * 通过 userId 查询数据库中购物车数据并放入缓存
     *
     * @param userId
     * @return
     */
    @GetMapping("loadCartCache/{userId}")
    public Result loadCartCache(@PathVariable String userId) {
        cartService.loadCartCache(userId);
        return Result.ok();
    }
}
