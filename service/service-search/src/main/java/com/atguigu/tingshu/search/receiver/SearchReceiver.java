package com.atguigu.tingshu.search.receiver;

import com.atguigu.tingshu.common.rabbit.constant.MqConst;
import com.atguigu.tingshu.search.service.SearchService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * @author fzx
 * @ClassName SearchReceiver
 * @description: TODO
 * @date 2024年11月26日
 * @version: 1.0
 */
@Component
@Slf4j
public class SearchReceiver {

    @Autowired
    private SearchService searchService;

    /**
     * 上架专辑
     *
     * @param albumId
     * @param message
     * @param channel
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_ALBUM_UPPER, durable = "true", autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_ALBUM),
            key = {MqConst.ROUTING_ALBUM_UPPER}
    ))
    public void upperAlbum(Long albumId, Message message, Channel channel) {
        System.out.println("上架专辑：" + albumId);
        try {
            //  判断 mysql,setnx key value; 业务字段;
            if (null != albumId) {
                searchService.upperAlbum(albumId);
            }
        } catch (Exception e) {
            //  记录重回的次数，超过一定次数；记录消费消息异常记录表！
            //  channel.basicNack(message.getMessageProperties().getDeliveryTag(),false,true);
            log.error("消息消费异常{},消息异常记录表", e.getMessage());
        }
        //  手动确认
        try {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 下架专辑
     *
     * @param albumId
     * @param message
     * @param channel
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_ALBUM_LOWER, durable = "true", autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_ALBUM),
            key = {MqConst.ROUTING_ALBUM_LOWER}
    ))
    public void lowerAlbum(Long albumId, Message message, Channel channel) {
        System.out.println("下架专辑：" + albumId);
        try {
            //  判断
            if (null != albumId) {
                searchService.lowerAlbum(albumId);
            }
        } catch (Exception e) {
            e.getMessage();
        }
        //  手动确认
        try {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
