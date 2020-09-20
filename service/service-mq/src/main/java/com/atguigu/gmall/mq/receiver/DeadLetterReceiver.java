package com.atguigu.gmall.mq.receiver;

import com.atguigu.gmall.mq.config.DeadLetterMqConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;

@Component
@Configuration
public class DeadLetterReceiver {

    // 由于配置类发送消息使用队列一且设置了过期时间 会变为死信 死信进入死信交换机
    // 死信交换机与队列二绑定 故监听队列二获取消息
    @RabbitListener(queues = DeadLetterMqConfig.queue_dead_2)
    public void receiveDeadLetter(String message) {

        System.out.println("接收到的消息 ===>>> " + message);

        // 定义时间格式
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        System.out.println("接收到消息的时间 ===>>> " + sdf.format(new Date()));
    }
}
