package com.atguigu.gmall.order.config;

import com.atguigu.gmall.common.constant.MqConst;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.CustomExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class OrderCancelMqConfig {

    // 声明一个交换机
    @Bean
    public CustomExchange delayExchange() {
        Map<String, Object> arguments = new HashMap<>();

        arguments.put("x-delayed-type", "direct");

        // 参数一：交换机名称 | 参数二：交换机类型key 参数三：是否持久化 | 参数四：是否自动删除 | 参数五：是否传入其他参数
        return new CustomExchange(MqConst.EXCHANGE_DIRECT_ORDER_CANCEL, "x-delayed-message", true, false, arguments);
    }

    // 声明一个队列
    @Bean
    public Queue delayQueue() {
        // 参数一：队列名字 | 参数二：是否支持持久化
        return new Queue(MqConst.QUEUE_ORDER_CANCEL, true);
    }

    // 设置 交换机 和 队列 的绑定关系
    @Bean
    public Binding delayBinding() {
        // 通过 MqConst.ROUTING_ORDER_CANCE 将 队列 与 交换机 绑定
        // 注意：调用 noargs() 转换为 Binding
        return BindingBuilder.bind(delayQueue()).to(delayExchange()).with(MqConst.ROUTING_ORDER_CANCEL).noargs();
    }
}
