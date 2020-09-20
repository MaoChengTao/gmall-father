package com.atguigu.gmall.mq.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.CustomExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;


/**
 * 基于插件实现延迟消息
 */
@Configuration
public class DelayedMqConfig {

    // 基于插件实现延迟消息 只需要一个交换机、一个队列、一个路由键
    public static final String exchange_delay = "exchange.delay";
    public static final String routing_delay = "routing.delay";
    public static final String queue_delay_1 = "queue.delay.1";

    // 声明一个交换机
    @Bean
    public CustomExchange delayExchange() {
        Map<String, Object> arguments = new HashMap<>();

        arguments.put("x-delayed-type", "direct");

        // 参数一：交换机名称 | 参数二：交换机类型key 参数三：是否持久化 | 参数四：是否自动删除 | 参数五：是否传入其他参数
        return new CustomExchange(exchange_delay, "x-delayed-message", true, false, arguments);
    }

    // 声明一个队列
    @Bean
    public Queue delayQeue1() {
        // 参数一：队列名字 | 参数二：是否支持持久化
        return new Queue(queue_delay_1, true);
    }

    // 设置 交换机 和 队列 的绑定关系
    @Bean
    public Binding delayBinding1() {
        // 通过 routing_delay 将 队列 与 交换机 绑定
        // 注意：调用 noargs() 转换为 Binding
        return BindingBuilder.bind(delayQeue1()).to(delayExchange()).with(routing_delay).noargs();
    }
}
