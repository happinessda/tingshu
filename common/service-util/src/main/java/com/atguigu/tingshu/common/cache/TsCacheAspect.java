package com.atguigu.tingshu.common.cache;

import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.login.TsLogin;
import com.atguigu.tingshu.common.result.ResultCodeEnum;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.user.UserInfo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * @author fzx
 * @ClassName TsCacheAspect
 * @description: TODO
 * @date 2024年12月03日
 * @version: 1.0
 */
@Component
@Aspect
@Slf4j
public class TsCacheAspect {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;


    //   @annotation(org.springframework.transaction.annotation.Transactional)
    //  环绕通知：切注解TsLogin 这个注解;
    @Around("@annotation(tsCache)")
    public Object doTsCacheAspect(ProceedingJoinPoint point, TsCache tsCache) throws Throwable {
        //  声明一个对象
        Object obj = new Object();
        //  先获取到缓存的key, 获取到专辑参数
        String key = tsCache.prefix() + Arrays.asList(point.getArgs());
        try {
            //  根据key获取缓存数据
            obj = redisTemplate.opsForValue().get(key);
            //  判断缓存中是否有数据
            if (null == obj) {
                //  声明一个分布式锁的key
                String lockKey = key + ":lock";
                RLock lock = redissonClient.getLock(lockKey);
                //  上锁
                lock.lock();
                try {
                    //  执行业务逻辑，查询数据库之前再查询一次缓存.
                    obj = redisTemplate.opsForValue().get(key);
                    if (null != obj) {
                        //  说明换成中有数据
                        return obj;
                    }
                    //  执行查询数据库的操作
                    obj = point.proceed();
                    if (null == obj){
                        //  存储一个空对象放入缓存
                        this.redisTemplate.opsForValue().set(key,new Object(),RedisConstant.ALBUM_TEMPORARY_TIMEOUT, TimeUnit.SECONDS);
                        //  返回数据
                        return new Object();
                    }
                    //  将数据存储到缓存中
                    this.redisTemplate.opsForValue().set(key,obj,RedisConstant.ALBUM_TIMEOUT, TimeUnit.DAYS);
                    //  返回数据
                    return obj;
                } finally {
                    //  解锁
                    lock.unlock();
                }
            } else {
                return obj;
            }
        } catch (Throwable e) {
            log.error(e.getMessage());
        }
        //  返回数据
        return point.proceed();
    }
}
