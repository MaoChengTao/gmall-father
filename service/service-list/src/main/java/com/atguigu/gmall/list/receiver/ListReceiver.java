package com.atguigu.gmall.list.receiver;

import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.list.service.SearchService;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 接收后台系统传递过来的消息队列
 */
@Component
public class ListReceiver {

    @Autowired
    private SearchService searchService;

    /**
     * 处理商品上架
     *
     * @param skuId
     * @param message
     * @param channel
     */
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_GOODS_UPPER, autoDelete = "false", durable = "true"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_GOODS, autoDelete = "false"),
            key = {MqConst.ROUTING_GOODS_UPPER}
    ))
    public void upperGoods(Long skuId, Message message, Channel channel) {
        System.out.println("接收到消息：" + new String(message.getBody()));
        System.out.println("需要上架的商品skuId：" + skuId);
        if (null != skuId) {
            // 调用方法上架商品
            searchService.upperGoods(skuId);
        }
        // 手动确定ack
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }

    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_GOODS_LOWER, autoDelete = "false", durable = "true"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_GOODS, autoDelete = "false"),
            key = {MqConst.ROUTING_GOODS_LOWER}
    ))
    public void lowerGoods(Long skuId, Message message, Channel channel) {
        System.out.println("接收到消息：" + new String(message.getBody()));
        System.out.println("需要下架的商品skuId：" + skuId);
        if (null != skuId) {
            // 调用方法上架商品
            searchService.lowerGoods(skuId);
        }
        // 手动确定ack
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }
}
