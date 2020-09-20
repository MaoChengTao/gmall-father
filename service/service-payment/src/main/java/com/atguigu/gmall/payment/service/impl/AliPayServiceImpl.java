package com.atguigu.gmall.payment.service.impl;

import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradeCloseRequest;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeCloseResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.atguigu.gmall.model.enums.PaymentStatus;
import com.atguigu.gmall.model.enums.PaymentType;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.order.client.OrderFeignClient;
import com.atguigu.gmall.payment.config.AlipayConfig;
import com.atguigu.gmall.payment.service.AliPayService;
import com.atguigu.gmall.payment.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AliPayServiceImpl implements AliPayService {

    @Autowired
    private AlipayClient alipayClient;

    @Autowired
    private OrderFeignClient orderFeignClient;

    @Autowired
    private PaymentService paymentService;

    /**
     * 生成支付二维码
     * http://api.gmall.com/api/payment/alipay/submit/172 支付页面显示二维码 显示支付金额
     *
     * @param orderId 订单Id
     * @return
     * @throws Exception
     */
    @Override
    public String createAliPay(Long orderId) throws Exception {
        // 根据 orderId 获取订单信息
        OrderInfo orderInfo = orderFeignClient.getOrderInfo(orderId);

        // 点击生成二维码的时候 调用方法【保存交易记录】
        paymentService.savePaymentInfo(orderInfo, PaymentType.ALIPAY.name());

        //AlipayClient alipayClient =  new DefaultAlipayClient( "https://openapi.alipay.com/gateway.do" , APP_ID, APP_PRIVATE_KEY, FORMAT, CHARSET, ALIPAY_PUBLIC_KEY, SIGN_TYPE);  //获得初始化的AlipayClient
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest(); //创建API对应的request

        // 设置同步回调
        //alipayRequest.setReturnUrl("http://domain.com/CallBack/return_url.jsp");
        // http://api.gmall.com/api/payment/alipay/callback/return
        alipayRequest.setReturnUrl(AlipayConfig.return_payment_url);

        // 设置异步回调
        //alipayRequest.setNotifyUrl("http://domain.com/CallBack/notify_url.jsp"); //在公共参数中设置回跳和通知地址
        alipayRequest.setNotifyUrl(AlipayConfig.notify_payment_url); //在公共参数中设置回跳和通知地址

        // 创建 Map 封装参数
        Map<String, Object> map = new HashMap<>();
        map.put("out_trade_no", orderInfo.getOutTradeNo());
        map.put("product_code", "FAST_INSTANT_TRADE_PAY");
        map.put("total_amount", orderInfo.getTotalAmount());
        map.put("subject", orderInfo.getTradeBody());

        // 设置参数 将 map 转换为 json 字符串
        alipayRequest.setBizContent(JSON.toJSONString(map));
        /*alipayRequest.setBizContent( "{"  +
                "    \"out_trade_no\":\"20150320010101001\","  +
                "    \"product_code\":\"FAST_INSTANT_TRADE_PAY\","  +
                "    \"total_amount\":88.88,"  +
                "    \"subject\":\"Iphone6 16G\","  +
                "    \"body\":\"Iphone6 16G\","  +
                "    \"passback_params\":\"merchantBizType%3d3C%26merchantBizNo%3d2016010101111\","  +
                "    \"extend_params\":{"  +
                "    \"sys_service_provider_id\":\"2088511833207846\""  +
                "    }" +
                "  }" ); //填充业务参数 */

        return alipayClient.pageExecute(alipayRequest).getBody();  //调用SDK生成表单
    }

    /**
     * 退款
     *
     * @param orderId
     * @return
     */
    @Override
    public boolean refund(Long orderId) {
        // 创建退款请求
        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();

        // 获取 orderInfo
        OrderInfo orderInfo = orderFeignClient.getOrderInfo(orderId);

        // 创建 Map 封装参数，退款需要的参数 out_trade_no || trade_no && refund_amount
        Map<String, Object> map = new HashMap<>();
        map.put("out_trade_no", orderInfo.getOutTradeNo());
        map.put("refund_amount", orderInfo.getTotalAmount());
        map.put("refund_reason", "正常退款");

        // 设置退款请求
        request.setBizContent(JSON.toJSONString(map));

        // 执行退款请求 获取响应
        AlipayTradeRefundResponse response = null;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }

        if (response.isSuccess()) {
            System.out.println("调用成功");

            // 退款成功 ===>>> 关闭支付宝交易状态 和 更新交易记录：关闭
            PaymentInfo paymentInfo = new PaymentInfo();

            paymentInfo.setPaymentStatus(PaymentStatus.ClOSED.name());

            paymentService.updatePaymentInfo(orderInfo.getOutTradeNo(), paymentInfo);

            return true;

        } else {
            System.out.println("调用失败");

            return false;
        }
    }

    /**
     * 关闭交易记录
     *
     * @param orderId
     * @return
     */
    @Override
    public Boolean closePay(Long orderId) {
        AlipayTradeCloseRequest request = new AlipayTradeCloseRequest();

        OrderInfo orderInfo = orderFeignClient.getOrderInfo(orderId);
        String outTradeNo = orderInfo.getOutTradeNo();

        Map<String, Object> map = new HashMap<>();
        map.put("out_trade_no", outTradeNo);

        request.setBizContent(JSON.toJSONString(map));

        AlipayTradeCloseResponse response = null;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }

        if (response.isSuccess()) {
            System.out.println("调用成功");
            return true;
        } else {
            System.out.println("调用失败");
            return false;
        }
    }
}
