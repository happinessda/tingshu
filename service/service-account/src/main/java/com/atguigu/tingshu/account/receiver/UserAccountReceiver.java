package com.atguigu.tingshu.account.receiver;

import com.atguigu.tingshu.account.service.UserAccountService;
import com.atguigu.tingshu.common.rabbit.constant.MqConst;
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
 * @ClassName UserAccount
 * @description: TODO
 * @date 2024年11月23日
 * @version: 1.0
 */
@Component
public class UserAccountReceiver {

    @Autowired
    private UserAccountService userAccountService;

    //  第一个作用：设置绑定关系; 第二个作用：监听这个消息；
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_USER_REGISTER, durable = "true", autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_USER),
            key = {MqConst.ROUTING_USER_REGISTER}
    ))
    public void userRegister(Long userId, Message message, Channel channel){
        //  获取消息发送的内容;
        if (null != userId){
            //  调用服务层方法
            userAccountService.initUserAccount(userId);
        }
        //  开启消息确认模式
        try {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 充值成功
     * @param orderNo
     * @param message
     * @param channel
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_RECHARGE_PAY_SUCCESS, durable = "true", autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_ORDER),
            key = {MqConst.ROUTING_RECHARGE_PAY_SUCCESS}
    ))
    public void rechargePaySuccess(String orderNo, Message message, Channel channel){
        //  判断订单编号是否为可控
        if (!StringUtils.isEmpty(orderNo)){
            //  调用服务层方法
            userAccountService.rechargePaySuccess(orderNo);
        }
        //  开启消息确认模式
        try {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
