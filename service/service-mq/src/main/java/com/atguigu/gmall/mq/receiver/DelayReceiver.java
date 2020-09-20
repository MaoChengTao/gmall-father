package com.atguigu.gmall.mq.receiver;

import com.atguigu.gmall.mq.config.DelayedMqConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;

@Component
@Configuration
public class DelayReceiver {

    // 设置监听的队列 其余配置已经在 DelayedMqConfig配置类声明
    @RabbitListener(queues = DelayedMqConfig.queue_delay_1)
    public void receiveDelay(String message) {

        System.out.println("接收到的消息：" + message);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        System.out.println("接收到消息的时间 ===>>> " + sdf.format(new Date()));
    }
}
