package com.atguigu.gmall.mq.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * 基于死信实现延迟消息
 */
@Configuration
public class DeadLetterMqConfig {

    /*
    基于死信实现延迟消息 需要
        1个交换机 2个路由键 2个队列
     */
    public static final String exchange_dead = "exchange.dead";
    public static final String routing_dead_1 = "routing.dead.1";
    public static final String routing_dead_2 = "routing.dead.2";
    public static final String queue_dead_1 = "queue.dead.1";
    public static final String queue_dead_2 = "queue.dead.2";

    // 声明一个交换机
    @Bean
    public DirectExchange exchange() {
        // 参数一：交换机名称 | 参数二：是否持久化 | 参数三：是否自动删除 | 参数四：是否传入其他参数
        return new DirectExchange(exchange_dead, true, false, null);
    }

    // 声明第一个队列 并设置消息的存活时间
    @Bean
    public Queue queue1() {
        // 声明一个 Map
        Map<String, Object> arguments = new HashMap<>();

        // key=固定值 value=可能会变为死信交换机的交换机(满足三个条件之一就转变)
        arguments.put("x-dead-letter-exchange", exchange_dead);

        // key=死信交换机的路由键 value=转变为死信交换机后绑定的新队列
        arguments.put("x-dead-letter-routing-key", routing_dead_2);

        // 统一延迟时间 - 消息存活时间10秒钟
        arguments.put("x-message-ttl", 10 * 1000);

        // 参数一：队列名称 | 参数二：是否持久化 | 参数三：是否是独享、排外的{true 只允许第一次绑定交换机的连接访问队列} |
        // 参数四：是否自动删除 |参数五：是否有其他参数传入
        return new Queue(queue_dead_1, true, false, false, arguments);
    }

    // 设置 交换机 和 队列一 的绑定关系
    @Bean
    public Binding binding() {
        // 通过 routing_dead_1路由键 将 queue1() 与 exchange() 绑定
        return BindingBuilder.bind(queue1()).to(exchange()).with(routing_dead_1);
    }

    // 声明第二个队列 {普通队列 不设置消息的过期时间}
    @Bean
    public Queue queue2() {
        return new Queue(queue_dead_2, true, false, false, null);
    }

    // 设置 交换机 和 队列二 的绑定关系
    // 若队列一的消息过期 则消息变为死信 并且 交换机变为死信交换机
    // 为死信交换机绑定新的路由键
    @Bean
    public Binding deadBinding() {
        return BindingBuilder.bind(queue2()).to(exchange()).with(routing_dead_2);
    }
}
