package com.atguigu.gmall.order.controller;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.cart.client.CartFeignClient;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.util.AuthContextHolder;
import com.atguigu.gmall.model.cart.CartInfo;
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.user.UserAddress;
import com.atguigu.gmall.order.service.OrderService;
import com.atguigu.gmall.product.client.ProductFeignClient;
import com.atguigu.gmall.user.client.UserFeignClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

@RestController
@RequestMapping("api/order")
public class OrderApiController {

    @Autowired
    private UserFeignClient userFeignClient;

    @Autowired
    private CartFeignClient cartFeignClient;

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    // 网关拦截验证：if (antPathMatcher.match("/api/**/auth/**", path)) {...}

    /**
     * 确认订单
     * 1 查看订单 --->>> 必须登录 --->>> 符合网关拦截认证要求即需要登录 故映射地址为： @GetMapping("auth/trade")
     * 2 trade.html页面需要：userAddressList、${detailArrayList}、totalAmount、totalNum 等数据渲染页面
     * 3 仿照 ItemServiceImpl 使用 Map 封装数据返回页面
     *
     * @param request
     * @return
     */
    @GetMapping("auth/trade")
    public Result<Map<String, Object>> trade(HttpServletRequest request) {
        /*
        思路：
            1 通过 userFeignClient 获取用户收货地址列表
            2 通过 cartFeignClient 获取购买清单列表
            3 计算总金额
            4 计算商品总件数
         */

        // 获取 userId
        String userId = AuthContextHolder.getUserId(request);

        // 远程调用 - 获取用户收货地址列表
        List<UserAddress> userAddressList = userFeignClient.findUserAddressListByUserId(userId);

        // 远程调用 - 获取购买清单列表
        List<CartInfo> cartInfoCheckedList = cartFeignClient.getCartCheckedList(userId);

        // 声明一个集合来存储订单明细
        ArrayList<OrderDetail> orderDetailList = new ArrayList<>();

        // 声明变量存储 商品总件数
        int totalNum = 0;

        if (!CollectionUtils.isEmpty(cartInfoCheckedList)) {

            for (CartInfo cartInfo : cartInfoCheckedList) {
                // 声明一个订单明细对象
                OrderDetail orderDetail = new OrderDetail();

                orderDetail.setSkuId(cartInfo.getSkuId());
                orderDetail.setSkuName(cartInfo.getSkuName());
                orderDetail.setImgUrl(cartInfo.getImgUrl());
                orderDetail.setSkuNum(cartInfo.getSkuNum());
                orderDetail.setOrderPrice(cartInfo.getSkuPrice());

                // 方式二：计算商品总件数
                totalNum += cartInfo.getSkuNum();

                // 添加到集合
                orderDetailList.add(orderDetail);
            }
        }

        // 计算总金额 OrderInfo.sumTotalAmount()
        // 计算总结金额必须先为 orderDetailList 赋值 才能调用 sumTotalAmount() 方法计算
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderDetailList(orderDetailList);
        // 计算总金额
        orderInfo.sumTotalAmount();// sumTotalAmount()计算完后将结果赋值为 OrderInfo.totalAmount

        // 方式一：计算商品总件数
        /*
         方式一：计算有几个spu中的sku ===>>> orderDetailList.size()
         方式二：计算有几个spu中的skuId的个数 ===>>> 遍历集合时就为totalNum赋值 totalNum += cartInfo.getSkuNum()
         */
        //int totalNum = orderDetailList.size();

        // 获取流水号
        String tradeNo = orderService.getTradeNo(userId);

        // 声明 Map 封装页面所需要的数据
        Map<String, Object> map = new HashMap<>();

        // 保存数据
        // 保存流水号 页面需要tradeNo: [[${tradeNo}]]
        map.put("tradeNo", tradeNo);

        map.put("userAddressList", userAddressList);
        map.put("detailArrayList", orderDetailList);
        // 保存总金额
        map.put("totalAmount", orderInfo.getTotalAmount());
        // 保存商品总件数
        map.put("totalNum", totalNum);

        return Result.ok(map);
    }

    /**
     * 提交订单
     * 提交订单的控制器：'/api/order/auth/submitOrder?tradeNo= '+ tradeNo
     *
     * @param orderInfo
     * @param request
     * @return
     */
    @PostMapping("auth/submitOrder")
    public Result submitOrder(@RequestBody OrderInfo orderInfo, HttpServletRequest request) {
        // 获取到用户Id 并 设置用户Id
        String userId = AuthContextHolder.getUserId(request);
        orderInfo.setUserId(Long.parseLong(userId));

        // 判断是否是无刷新回退重复提交订单

        // 获取页面提交订单时传递的流水号
        String tradeCodeNo = request.getParameter("tradeNo");

        // 调用服务层的比较方法
        boolean flag = orderService.checkTradeCode(userId, tradeCodeNo);

        // 判断比较结果
        if (!flag) { // 比较结果不一致，流水号不匹配
            // 比较失败
            return Result.fail().message("不能重复提交订单！");
        }

        // 流水号比较一致 删除缓存中的流水号
        orderService.deleteTradeNo(userId);

        // 验证库存和价格 需要验证每个商品的库存和最新价格

        // 获取商品明细集合
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();

        // 定义集合保存库存不足的信息
        List<String> errorList = new ArrayList<>();

        // 定义异步编排集合
        List<CompletableFuture> completableFutureList = new ArrayList<>();

        if (!CollectionUtils.isEmpty(orderDetailList)) {

            // 循环遍历验证每个商品
            for (OrderDetail orderDetail : orderDetailList) {

                /*// 调用方法验证库存
                boolean checkStockResult = orderService.checkStock(orderDetail.getSkuId(), orderDetail.getSkuNum());

                if (!checkStockResult) {// 验证没通过
                    return Result.fail().message(orderDetail.getSkuName() + "【" + "库存不足" + "】");
                }*/
                CompletableFuture<Void> checkStockCompletableFuture = CompletableFuture.runAsync(() -> {
                    boolean checkStockResult = orderService.checkStock(orderDetail.getSkuId(), orderDetail.getSkuNum());

                    if (!checkStockResult) {// 验证不通过
                        errorList.add(orderDetail.getSkuName() + "【" + "库存不足" + "】");
                    }

                }, threadPoolExecutor);

                // 保存异步编排到集合
                completableFutureList.add(checkStockCompletableFuture);

                /*// 获取商品的最新价格
                BigDecimal skuPrice = productFeignClient.getSkuPrice(orderDetail.getSkuId());

                // 验证商品的价格{订单里的价格与数据库中的价格比较}
                if (orderDetail.getOrderPrice().compareTo(skuPrice) != 0) {

                    // 如果价格有变动 需要获取最新价格将购物车中的数据变为最新的
                    // 查询数据库最新价格 ===>>> 放入缓存 ===>>> 刷新购物车价格 ===>>> 更新订单价格
                    // 通过 userId 查询数据库中购物车数据并放入缓存
                    cartFeignClient.loadCartCache(userId);

                    return Result.fail().message(orderDetail.getSkuName() + "【" + "价格有变动" + "】");
                }*/

                CompletableFuture<Void> checkPriceCompletableFuture = CompletableFuture.runAsync(() -> {
                    BigDecimal skuPrice = productFeignClient.getSkuPrice(orderDetail.getSkuId());

                    if (orderDetail.getOrderPrice().compareTo(skuPrice) != 0) {

                        cartFeignClient.loadCartCache(userId);

                        errorList.add(orderDetail.getSkuName() + "【" + "价格有变动" + "】");
                    }
                }, threadPoolExecutor);

                // 保存异步编排到集合
                completableFutureList.add(checkPriceCompletableFuture);
            }
            // 合并异步编排
            CompletableFuture.allOf(
                    completableFutureList.toArray(new CompletableFuture[completableFutureList.size()])).join();

            // 判断是否有验证库存不通过的信息
            if (errorList.size() > 0) {
                return Result.fail().message(StringUtils.join(errorList, "---"));
            }
        }

        // 调用服务层
        Long orderId = orderService.saveOrderInfo(orderInfo);

        // 返回订单Id
        return Result.ok(orderId);
    }

    /**
     * 根据订单号获取订单信息
     *
     * @param orderId
     * @return
     */
    @GetMapping("inner/getOrderInfo/{orderId}")
    public OrderInfo getOrderInfo(@PathVariable Long orderId) {
        return orderService.getOrderInfo(orderId);
    }

    /**
     * 拆单
     * 拆单接口：http://localhost:8204/api/order/orderSplit?orderId=xxx&wareSkuMap=xxx
     *
     * @param request
     * @return
     */
    @RequestMapping("orderSplit")
    public String orderSplit(HttpServletRequest request) {
        // 获取拆单需要的数据
        String orderId = request.getParameter("orderId");
        String wareSkuMap = request.getParameter("wareSkuMap");

        //调用服务层进行拆单
        List<OrderInfo> subOrderInfoList = orderService.orderSplit(orderId, wareSkuMap);

        // 定义一个集合保存map
        List<Map> mapList = new ArrayList<>();

        if (!CollectionUtils.isEmpty(subOrderInfoList)) {

            for (OrderInfo orderInfo : subOrderInfoList) {
                // 将 orderInfo 转换为 map
                Map map = orderService.initWareOrder(orderInfo);

                mapList.add(map);
            }
        }
        return JSONObject.toJSONString(mapList);
    }
}
