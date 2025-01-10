package com.atguigu.tingshu.order.receiver;

import com.atguigu.tingshu.common.rabbit.constant.MqConst;
import com.atguigu.tingshu.order.service.OrderInfoService;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;

/**
 * @author fzx
 * @ClassName OrderReceiver
 * @description: TODO
 * @date 2024年12月07日
 * @version: 1.0
 */
@Component
public class OrderReceiver {

    @Autowired
    private OrderInfoService orderInfoService;

    @RabbitListener(bindings = @QueueBinding(
            exchange = @Exchange(value = MqConst.EXCHANGE_ORDER),
            value = @Queue(value = MqConst.QUEUE_ORDER_PAY_SUCCESS, durable = "true",autoDelete = "false"),
            key = {MqConst.ROUTING_ORDER_PAY_SUCCESS}
    ))
    public void orderPaySuccess(String orderNo,Message message, Channel channel){
        //  判断
        if (!StringUtils.isEmpty(orderNo)){
            //  更新订单状态；订单状态：1402-已支付    记录用户购买记录;
            orderInfoService.orderPaySuccess(orderNo);
        }
        try {
            //  手动确认
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * 取消订单
     * @param orderNo
     * @param message
     * @param channel
     */
    @RabbitListener(queues = MqConst.QUEUE_CANCEL_ORDER)
    public void cancelOrder(String orderNo, Message message, Channel channel){
        System.out.println("取消订单");
        try {
            if (!StringUtils.isEmpty(orderNo)){
                //  调用服务层方法
                orderInfoService.cancelOrder(orderNo);
            }
        } catch (Exception e) {
            e.getMessage();
        }
        try {
            //  手动确认
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
