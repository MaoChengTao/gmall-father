package com.atguigu.gmall.common.service;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 负责消息发送
 */
@Service
public class RabbitService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * @param exchange   交换机
     * @param routingKey 路由键
     * @param message    消息
     * @return
     */
    public boolean sendMessage(String exchange, String routingKey, Object message) {

        rabbitTemplate.convertAndSend(exchange, routingKey, message);

        return true;
    }

    /**
     * 基于插件 - 发送延迟消息
     *
     * @param exchange   交换机
     * @param routingKey 路由键
     * @param message    消息
     * @param delayTime  延迟时间 / 秒
     * @return
     */
    public boolean sendDelayMessage(String exchange, String routingKey, Object message, int delayTime) {
        rabbitTemplate.convertAndSend(exchange, routingKey, message, new MessagePostProcessor() {
            @Override
            public Message postProcessMessage(Message message) throws AmqpException {
                // 设置消息的过期时间
                message.getMessageProperties().setDelay(delayTime * 1000);
                // 返回消息
                return message;
            }
        });
        return true;
    }
}
