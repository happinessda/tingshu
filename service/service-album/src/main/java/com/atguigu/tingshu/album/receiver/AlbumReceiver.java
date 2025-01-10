package com.atguigu.tingshu.album.receiver;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.album.service.TrackInfoService;
import com.atguigu.tingshu.common.rabbit.constant.MqConst;
import com.atguigu.tingshu.vo.album.TrackStatMqVo;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * @author fzx
 * @ClassName AlbumReceiver
 * @description: TODO
 * @date 2024年12月02日
 * @version: 1.0
 */
@Component
@Slf4j
public class AlbumReceiver {

    @Autowired
    private TrackInfoService trackInfoService;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 更新声音播放进度
     *
     * @param trackJson
     * @param message
     * @param channel
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_TRACK_STAT_UPDATE, durable = "true", autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_TRACK),
            key = MqConst.ROUTING_TRACK_STAT_UPDATE
    ))
    public void updateTrackStat(String trackJson, Message message, Channel channel) {
        //  判断当前对象是否为空
        if (!StringUtils.isEmpty(trackJson)) {
            //  将这个字符串转换为实体对象;
            TrackStatMqVo trackStatMqVo = JSONObject.parseObject(trackJson, TrackStatMqVo.class);
            log.info("更新声音播放进度：{}", trackStatMqVo);
            String businessNo = trackStatMqVo.getBusinessNo();
            //  防止消息重复消费.
            Boolean result = redisTemplate.opsForValue().setIfAbsent(businessNo, 1, 30, TimeUnit.SECONDS);
            if (result){
                try {
                    //  调用服务层方法.
                    trackInfoService.updateTrackStat(trackStatMqVo);
                } catch (Exception e) {
                    log.error(e.getMessage());
                    //  需要重试消费;
                    this.redisTemplate.delete(businessNo);
                }
            }
        }
        try {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
