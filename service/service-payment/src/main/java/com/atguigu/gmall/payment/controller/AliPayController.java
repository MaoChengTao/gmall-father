package com.atguigu.gmall.payment.controller;

import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.enums.PaymentStatus;
import com.atguigu.gmall.model.enums.PaymentType;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.payment.config.AlipayConfig;
import com.atguigu.gmall.payment.service.AliPayService;
import com.atguigu.gmall.payment.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

// 使用 @Controller：是因为不是所有的控制器都要返回 json数据。所以不是使用 @RestController。
@Controller
@RequestMapping("/api/payment/alipay")
public class AliPayController {

    @Autowired
    private AliPayService aliPayService;

    @Autowired
    private PaymentService paymentService;


    /**
     * 生成二维码功能
     * 生成二维码的页面：http://api.gmall.com/api/payment/alipay/submit/168
     *
     * @param orderId 订单号
     * @return
     * @ResponseBody注解作用：将数据转换json格式、直接数据返回页面
     */
    @RequestMapping("submit/{orderId}")
    @ResponseBody
    public String submit(@PathVariable Long orderId) {
        String from = "";

        try {

            from = aliPayService.createAliPay(orderId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 结合 @ResponseBody 直接将数据返回页面
        return from;
    }

    /**
     * 支付后 - 同步回调
     * http://api.gmall.com/api/payment/alipay/callback/return
     */
    @RequestMapping("callback/return")
    public String returnCallback() {
        // 当用户获取到同步回调之后
        // 重定向 跳转到支付成功页面 http://payment.gmall.com/pay/success.html 展示支付成功页面
        return "redirect:" + AlipayConfig.return_order_url;
    }

    /**
     * 支付后 - 异步回调{内网穿透}
     * 回调地址：http://vtx78f.natappfree.cc/api/payment/alipay/callback/notify
     * 异步验签参考官方文档：https://opendocs.alipay.com/open/270/105902
     * 在支付宝的业务通知中，只有交易通知状态为 TRADE_SUCCESS 或 TRADE_FINISHED 时，支付宝才会认定为买家付款成功。
     */
    @RequestMapping("callback/notify")
    public String notifyCallback(@RequestParam Map<String, String> paramsMap) {

        System.out.println("异步回调...");

        //将异步通知中收到的所有参数都存放到map中
        //Map<String, String> paramsMap = ...

        // 获取异步通知中的 out_trade_no
        String outTradeNo = paramsMap.get("out_trade_no");

        // 获取异步通知中的 trade_status
        String trade_status = paramsMap.get("trade_status");

        //调用SDK验证签名
        boolean signVerified = false;
        try {
            signVerified = AlipaySignature.rsaCheckV1(paramsMap, AlipayConfig.alipay_public_key, AlipayConfig.charset, AlipayConfig.sign_type);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }

        if (signVerified) {
            // TODO 验签成功后，按照支付结果异步通知中的描述，对支付结果中的业务内容进行二次校验，
            //  校验成功后在response中返回success并继续商户自身业务处理，校验失败返回failure

            // 在支付宝的业务通知中，只有交易通知状态为 TRADE_SUCCESS 或 TRADE_FINISHED 时，支付宝才会认定为买家付款成功。
            if ("TRADE_SUCCESS".equals(trade_status) || "TRADE_FINISHED".equals(trade_status)) {

                /*
                细节：
                    如果交易记录表中的支付状态为 PAID 或者 CLOSE，此时不能继续支付！！！
                    通过 outTradeNo 来查询数据 获取交易记录中的支付状态 判断支付状态！！！
                    异步回调的参数中有参数：out_trade_no 是唯一的！！！可以通过 out_trade_no 来查询订单记录获取支付状态！！！
                */
                // 通过 outTradeNo 和 支付类型 查询 PaymentInfo
                PaymentInfo paymentInfo = paymentService.getPaymentInfo(outTradeNo, PaymentType.ALIPAY.name());

                // 判断交易记录中的支付状态
                if (paymentInfo.getPaymentStatus().equals(PaymentStatus.PAID.name()) || paymentInfo.getPaymentStatus().equals(PaymentStatus.ClOSED.name())) {
                    return "failure";
                }

                // 正常的支付成功 ===>>> 更新交易记录状态
                // 根据 outTradeNo 和 支付类型 更新交易状态
                // 需要更新的字段：payment_status、callback_time、callback_content 通过 paramsMap 获取
                paymentService.paySuccess(outTradeNo, PaymentType.ALIPAY.name(), paramsMap);

                return "success";
            }
        } else {

            // TODO 验签失败则记录异常日志，并在response中返回failure.
            return "failure";
        }
        return "failure";
    }

    /**
     * 发起退款
     * http://localhost:8205/api/payment/alipay/refund/20
     * @param orderId
     * @return
     */
    @ResponseBody
    @RequestMapping("refund/{orderId}")
    public Result refund(@PathVariable Long orderId) {

        boolean flag = aliPayService.refund(orderId);

        return Result.ok(flag);
    }
}
