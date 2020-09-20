package com.atguigu.gmall.payment.service;

import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;

import java.util.Map;

public interface PaymentService {
    /**
     * 保存交易记录
     * 点击生成二维码 ===>>> 数据来自订单 OrderInfo
     * 订单页面：http://payment.gmall.com/pay.html?orderId=168
     * 支付页面：http://api.gmall.com/api/payment/alipay/submit/168
     *
     * @param orderInfo
     * @param paymentType 支付类型（1：微信 2：支付宝）
     */
    void savePaymentInfo(OrderInfo orderInfo, String paymentType);

    /**
     * 获取交易记录数据
     *
     * @param outTradeNo  商户订单号
     * @param paymentType 支付类型名称
     * @return
     */
    PaymentInfo getPaymentInfo(String outTradeNo, String paymentType);

    /**
     * 支付成功
     *
     * @param outTradeNo  商户订单号
     * @param paymentType 支付类型名称
     * @param paramsMap   异步通知参数Map
     */
    void paySuccess(String outTradeNo, String paymentType, Map<String, String> paramsMap);

    /**
     * 更新交易记录状态
     *
     * @param outTradeNo
     * @param paymentInfo
     */
    void updatePaymentInfo(String outTradeNo, PaymentInfo paymentInfo);

    /**
     * 关闭 paymentInfo
     * @param orderId
     */
    void closePayment(Long orderId);
}
