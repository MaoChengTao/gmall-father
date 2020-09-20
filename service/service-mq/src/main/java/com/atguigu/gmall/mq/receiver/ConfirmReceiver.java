package com.atguigu.gmall.mq.receiver;

import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/**
 * 消息接收端
 */
@Component
@Configuration
public class ConfirmReceiver {

    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "queue.confirm", autoDelete = "false"),
            exchange = @Exchange(value = "exchange.confirm", autoDelete = "false"),
            key = {"routing.confirm"}
    ))
    public void receiveMessage(Message message, Channel channel) {
        // 获取消息
        System.out.println("获取消息：" + new String(message.getBody()));

        try {
            // 模拟业务出错
            int i = 1 / 0;

            // 手动确认消息 | ack：表示消息成功处理 | false 确认一个消息 | true 批量确认
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);

        } catch (Exception e) {
            System.out.println("出现异常...判断消息是否被处理过");

            if (message.getMessageProperties().getRedelivered()) {

                System.out.println("消息已经被处理过...拒绝再次处理");

                channel.basicReject(message.getMessageProperties().getDeliveryTag(), false);

            } else {

                System.out.println("消息再次回到队列...");

                channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
            }
        }
    }
}
