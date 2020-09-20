package com.atguigu.gmall.mq.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.mq.config.DeadLetterMqConfig;
import com.atguigu.gmall.mq.config.DelayedMqConfig;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 消息发送端
 */
@RestController
@RequestMapping("/mq")
public class MqController {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 消息发送端
     * http://localhost:8282/mq/sendConfirm
     *
     * @return
     */
    @GetMapping("sendConfirm")
    public Result sendConfirm() {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        rabbitTemplate.convertAndSend(
                "exchange.confirm",
                "routing.confirm",
                sdf.format(new Date()));

        return Result.ok();
    }

    /**
     * 发送消息 - 基于死信实现延迟消息
     * http://localhost:8282/mq/sendDeadLettle
     *
     * @return
     */
    @GetMapping("sendDeadLetter")
    public Result sendDeadLetter() {
        // 定义时间格式
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        // 发送消息
        rabbitTemplate.convertAndSend(
                DeadLetterMqConfig.exchange_dead,
                DeadLetterMqConfig.routing_dead_1, "我是消息Ok");

        System.out.println("发送消息的时间 ===>>> " + sdf.format(new Date()));

        return Result.ok();
    }

    /**
     * 发送消息 - 基于插件实现延迟消息
     * http://localhost:8282/mq/sendDelay
     *
     * @return
     */
    @GetMapping("sendDelay")
    public Result sendDelay() {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        rabbitTemplate.convertAndSend(
                DelayedMqConfig.exchange_delay,
                DelayedMqConfig.routing_delay,
                "我是788",
                new MessagePostProcessor() {
                    @Override
                    public Message postProcessMessage(Message message) throws AmqpException {
                        // 自定义过期时间
                        message.getMessageProperties().setDelay(10 * 1000);

                        System.out.println("发送消息的时间 ===>>> " + sdf.format(new Date()));

                        return message;
                    }
                });
        return Result.ok();
    }
}
