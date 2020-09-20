package com.atguigu.gmall.payment.service;

public interface AliPayService {

    /**
     * 生成支付二维码
     * http://api.gmall.com/api/payment/alipay/submit/172 支付页面显示二维码 显示支付金额
     * @param orderId 订单Id
     * @return
     * @throws Exception
     */
    String createAliPay(Long orderId) throws Exception;

    /**
     * 退款
     * @param orderId
     * @return
     */
    boolean refund(Long orderId);

    /***
     * 关闭交易
     * @param orderId
     * @return
     */
    Boolean closePay(Long orderId);
}
