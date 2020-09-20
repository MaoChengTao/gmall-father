package com.atguigu.gmall.order.receiver;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.order.service.OrderService;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * 接收延迟消息 - 处理过期订单(本质是更新订单的状态)
 */
@Component
public class OrderReceiver {

    @Autowired
    private OrderService orderService;

    /**
     * 设置监听消息
     * 消息发送端发送的消息是 orderId
     * 已经在配置类设置了其他配置 故监听队列即可
     */
    @SneakyThrows
    @RabbitListener(queues = MqConst.QUEUE_ORDER_CANCEL)
    public void orderCancel(Long orderId, Message message, Channel channel) {
        // 接收消息并判断
        if (null != orderId) {
            // 根据 orderId 查询是否有该订单信息
            OrderInfo orderInfo = orderService.getById(orderId);

            // 判断有订单信息，并且 订单状态 和 进程状态 都是未支付状态下 才能关闭过期订单
            // ProcessStatus.UNPAID.getOrderStatus().name())：订单的进程中能获取到订单的状态
            if (orderInfo != null && orderInfo.getOrderStatus().equals(ProcessStatus.UNPAID.name())) {
                // 处理过期订单 - 更改订单状态
                orderService.execExpiredOrder(orderInfo.getId());
            }
        }

        // 手动ACK
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }

    /**
     * 支付成功后 - 更改订单状态与通知扣减库存
     *
     * @param orderId
     * @param message
     * @param channel
     */
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_PAYMENT_PAY, durable = "true"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_PAYMENT_PAY),
            key = {MqConst.ROUTING_PAYMENT_PAY}
    ))
    public void paySuccess(Long orderId, Message message, Channel channel) {
        // 接收消息并判断
        if (orderId != null) {
            // 根据 orderId 查询是否有该订单信息
            OrderInfo orderInfo = orderService.getOrderInfo(orderId);

            // 判断有订单信息
            // ProcessStatus.UNPAID.getOrderStatus().name())：订单的进程中能获取到订单的状态
            if (orderInfo != null && orderInfo.getOrderStatus().equals(ProcessStatus.UNPAID.getOrderStatus().name())) {

                // 更新订单状态 - 已支付
                orderService.updateOrderStatus(orderId, ProcessStatus.PAID);

                // 发送消息 - 通知仓库系统减库存{减库存接口所需要的参数都能在orderInfo获取到}
                orderService.sendOrderStatus(orderId);
            }

        }

        // 手动ACK确认消息
        // 参数一：判断订单是否被处理过 | 参数二：是否批量处理
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }

    /**
     * 接收库存系统减库存的结果 - 根据结果更新订单状态
     * 商品减库返回的结果消息
     * orderId：订单系统的订单ID
     * status:
     * 状态： ‘DEDUCTED’ (已减库存)
     * 状态： ‘OUT_OF_STOCK’ (库存超卖)
     */
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_WARE_ORDER, durable = "true"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_WARE_ORDER),
            key = {MqConst.ROUTING_WARE_ORDER}
    ))
    public void updateOrderStatus(String msgJson, Message message, Channel channel) {
        // 接收消息并判断
        if (!StringUtils.isEmpty(msgJson)) {
            // 将字符串转换为 map
            Map map = JSON.parseObject(msgJson, Map.class);

            // 获取对应的数据
            String orderId = (String) map.get("orderId");
            String status = (String) map.get("status");

            // 判断减库存的结果
            if ("DEDUCTED".equals(status)) {// 减库存成功

                orderService.updateOrderStatus(Long.parseLong(orderId), ProcessStatus.WAITING_DELEVER);

            } else {// 减|库存失败

                orderService.updateOrderStatus(Long.parseLong(orderId), ProcessStatus.STOCK_EXCEPTION);
            }
        }
        // 手动ACK确认消息
        // 参数一：判断订单是否被处理过 | 参数二：是否批量处理
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }
}
