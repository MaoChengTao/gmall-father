package com.atguigu.gmall.payment.service.impl;

import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.model.enums.PaymentStatus;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.payment.mapper.PaymentInfoMapper;
import com.atguigu.gmall.payment.service.PaymentService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;

@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    private PaymentInfoMapper paymentInfoMapper;

    @Autowired
    private RabbitService rabbitService;

    /**
     * 保存交易记录
     * 点击生成二维码 ===>>> 数据来自订单 OrderInfo
     * 订单页面：http://payment.gmall.com/pay.html?orderId=168
     * 支付页面：http://api.gmall.com/api/payment/alipay/submit/168
     *
     * @param orderInfo
     * @param paymentType 支付类型（1：微信 2：支付宝）
     */
    @Override
    public void savePaymentInfo(OrderInfo orderInfo, String paymentType) {
        // 细节：paymentInfo表中，订单Id和支付方式 不能重复{换言之，一个订单只能选择一种支付方式，并且只能支付一次}
        QueryWrapper<PaymentInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("order_id", orderInfo.getId());
        wrapper.eq("payment_type", paymentType);
        Integer count = paymentInfoMapper.selectCount(wrapper);
        // 判断 如果大于0 说明已经存在订单Id
        if (count > 0) {
            return; // 订单Id和支付方式不能重复 重复了直接返回
        }

        // 创建一个 PaymentInfo 对象
        PaymentInfo paymentInfo = new PaymentInfo();

        // 赋值 - 保存交易记录
        paymentInfo.setOutTradeNo(orderInfo.getOutTradeNo());
        paymentInfo.setOrderId(orderInfo.getId());
        paymentInfo.setTotalAmount(orderInfo.getTotalAmount());
        paymentInfo.setPaymentType(paymentType);
        paymentInfo.setPaymentStatus(PaymentStatus.UNPAID.name());
        paymentInfo.setSubject(orderInfo.getTradeBody());
        paymentInfo.setCreateTime(new Date());

        paymentInfoMapper.insert(paymentInfo);

    }

    /**
     * 获取交易记录信息
     *
     * @param outTradeNo  商户订单号
     * @param paymentType 支付类型名称
     * @return
     */
    @Override
    public PaymentInfo getPaymentInfo(String outTradeNo, String paymentType) {

        QueryWrapper<PaymentInfo> queryWrapper = new QueryWrapper<>();

        queryWrapper.eq("out_trade_no", outTradeNo);
        queryWrapper.eq("payment_type", paymentType);

        PaymentInfo paymentInfo = paymentInfoMapper.selectOne(queryWrapper);

        return paymentInfo;
    }

    /**
     * 支付成功
     *
     * @param outTradeNo  商户订单号
     * @param paymentType 支付类型名称
     * @param paramsMap   异步通知参数Map
     */
    @Override
    public void paySuccess(String outTradeNo, String paymentType, Map<String, String> paramsMap) {

        // 获取 PaymentInfo 以拿到 orderId 发送消息更新订单的状态
        PaymentInfo paymentInfoQuery = getPaymentInfo(outTradeNo, paymentType);

        if (paymentInfoQuery.getPaymentStatus() == PaymentStatus.PAID.name() ||
                paymentInfoQuery.getPaymentStatus() == PaymentStatus.ClOSED.name()) {
            return;
        }

        // 需要更新的字段：payment_status、callback_time、callback_content 通过 paramsMap 获取

        // 设置更新的内容
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setPaymentStatus(PaymentStatus.PAID.name());
        paymentInfo.setCallbackTime(new Date());
        paymentInfo.setCallbackContent(paramsMap.toString());
        paymentInfo.setTradeNo(paramsMap.get("trade_no")); // 获取异步通知参数中的支付宝交易号

        /*// 设置更新的条件
        QueryWrapper<PaymentInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("out_trade_no",outTradeNo);
        // 参数一：更新的内容 | 参数二：更新的条件
        paymentInfoMapper.update(paymentInfo,queryWrapper);*/

        // 调用方法更新交易记录状态
        updatePaymentInfo(outTradeNo, paymentInfo);

        // 支付成功后 - 发送消息 - 更新订单状态
        // 需要更新订单的状态 所以发送的消息为 订单Id
        rabbitService.sendMessage(
                MqConst.EXCHANGE_DIRECT_PAYMENT_PAY,
                MqConst.ROUTING_PAYMENT_PAY,
                paymentInfoQuery.getOrderId());
    }

    /**
     * 更新交易记录状态
     *
     * @param outTradeNo
     * @param paymentInfo
     */
    public void updatePaymentInfo(String outTradeNo, PaymentInfo paymentInfo) {
        // 设置更新的条件
        QueryWrapper<PaymentInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("out_trade_no", outTradeNo);
        // 参数一：更新的内容 | 参数二：更新的条件
        paymentInfoMapper.update(paymentInfo, queryWrapper);
    }

    /**
     * 关闭交易记录
     *
     * @param orderId
     */
    @Override
    public void closePayment(Long orderId) {
        // 先根据 orderId 查询是否有交易记录信息
        QueryWrapper<PaymentInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("order_id", orderId);
        Integer count = paymentInfoMapper.selectCount(queryWrapper);

        if (count == 0) {
            return;
        }

        // 更新状态为关闭
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setPaymentStatus(PaymentStatus.ClOSED.name());

        paymentInfoMapper.update(paymentInfo, queryWrapper);
    }
}
