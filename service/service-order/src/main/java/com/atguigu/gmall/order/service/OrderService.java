package com.atguigu.gmall.order.service;

import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderInfo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

/**
 * IService<OrderInfo> 调用通用方法
 */
public interface OrderService extends IService<OrderInfo> {

    /**
     * 保存订单信息
     *
     * @param orderInfo
     * @return
     */
    Long saveOrderInfo(OrderInfo orderInfo);

    /**
     * 生成流水号【传递到页面和保存进缓存】
     *
     * @param userId 传递用户Id 用于将流水号存储到缓存
     * @return
     */
    String getTradeNo(String userId);

    /**
     * 比较流水号
     *
     * @param userId      获取缓存中的流水号
     * @param tradeCodeNo 页面传递过来的流水号
     * @return
     */
    boolean checkTradeCode(String userId, String tradeCodeNo);

    /**
     * 删除流水号
     *
     * @param userId 获取缓存中的流水号
     */
    void deleteTradeNo(String userId);

    /**
     * 验证库存
     * 库存系统接口：http://localhost:9001/hasStock/hasStock?skuId=10221&num=2
     *
     * @param skuId  商品skuId
     * @param skuNum 商品数量
     * @return
     */
    boolean checkStock(Long skuId, Integer skuNum);

    /**
     * 根据订单Id 处理过期订单
     *
     * @param orderId
     */
    void execExpiredOrder(Long orderId);

    /**
     * 根据订单Id 查询订单信息和订单明细信息
     *
     * @param orderId
     * @return
     */
    OrderInfo getOrderInfo(Long orderId);

    /**
     * 根据订单Id、进程状态对象修改订单状态
     *
     * @param orderId       订单号
     * @param processStatus 进程状态对象
     */
    void updateOrderStatus(Long orderId, ProcessStatus processStatus);

    /**
     * 通过订单Id获取订单明细 - 用于发送消息通知减库存
     *
     * @param orderId
     */
    void sendOrderStatus(Long orderId);

    /**
     * 将 orderInfo 部分数据转换为 Map
     *
     * @param orderInfo
     * @return
     */
    Map initWareOrder(OrderInfo orderInfo);

    /**
     * 拆单
     * @param orderId 订单Id
     * @param wareSkuMap 仓库与sku的对应关系
     * @return
     */
    List<OrderInfo> orderSplit(String orderId, String wareSkuMap);
}
