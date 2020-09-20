package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.order.client.OrderFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;

@Controller
public class PaymentController {

    @Autowired
    private OrderFeignClient orderFeignClient;

    /**
     * 支付页面
     * 请求的地址：http://payment.gmall.com/pay.html?orderId=172
     *
     * @param request
     * @return
     */
    @GetMapping("pay.html")
    public String payIndex(HttpServletRequest request, Model model) {
        // 获取请求参数中的 orderId
        String orderId = request.getParameter("orderId");

        // 远程调用 - 获取订单信息
        OrderInfo orderInfo = orderFeignClient.getOrderInfo(Long.parseLong(orderId));

        // 页面需要 orderInfo
        model.addAttribute("orderInfo", orderInfo);

        return "payment/pay";
    }

    /**
     * 支付成功页面
     * 支付成功页面 http://payment.gmall.com/pay/success.html
     *
     * @return
     */
    @GetMapping("pay/success.html")
    public String paySuccess() {
        return "payment/success";
    }
}
