package com.atguigu.tingshu.handler;

import com.atguigu.tingshu.entity.CDCEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import top.javatool.canal.client.annotation.CanalTable;
import top.javatool.canal.client.handler.EntryHandler;

/**
 * @author fzx
 * @ClassName AlbumInfoCdcHandler
 * @description: TODO
 * @date 2024年12月04日
 * @version: 1.0
 */
@CanalTable("album_info")
@Component
@Slf4j
public class AlbumInfoCdcHandler implements EntryHandler<CDCEntity> {

    @Autowired
    private RedisTemplate redisTemplate;


    @Override
    public void insert(CDCEntity cdcEntity) {
        EntryHandler.super.insert(cdcEntity);
    }

    @Override
    public void update(CDCEntity before, CDCEntity after) {
        log.info("监听到数据修改,ID:{}", after.getId());
        //  只要有更新，则会执行当前这个方法.
        System.out.println("before:\t"+before.getId());
        System.out.println("after:\t"+after.getId());
        String key = "album:info:[" + after.getId()+"]";
        try {
            //  canal 底层维护这一个队列！对于每个写操作都有顺序性！保证不会乱序！
            this.redisTemplate.delete(key);
        } catch (Exception e) {
            //  发送消息到队列; 监听到异常，就进行重试3次 -- 记录异常表.
            throw new RuntimeException(e);
        }


    }

    @Override
    public void delete(CDCEntity cdcEntity) {
        EntryHandler.super.delete(cdcEntity);
    }
}
