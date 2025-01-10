package com.atguigu.tingshu.order.config;

import com.atguigu.tingshu.common.rabbit.constant.MqConst;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * @author fzx
 * @ClassName CanelOrderMqConfig
 * @description: TODO
 * @date 2024年12月07日
 * @version: 1.0
 */
@Component
public class CanelOrderMqConfig {

    //  队列：
    @Bean
    public Queue delayQueue(){
        return new Queue(MqConst.QUEUE_CANCEL_ORDER,true,false,false);
    }
    //  交换机:
    @Bean
    public CustomExchange delayExchange(){
        //  创建map 集合
        Map<String, Object> args = new HashMap<String, Object>();
        //  设置参数
        args.put("x-delayed-type", "direct");
        return new CustomExchange(MqConst.EXCHANGE_CANCEL_ORDER, "x-delayed-message", true, false, args);
    }
    //  绑定关系：
    @Bean
    public Binding bindingDelay(){
        return BindingBuilder.bind(delayQueue()).to(delayExchange()).with(MqConst.ROUTING_CANCEL_ORDER).noargs();
    }
}
