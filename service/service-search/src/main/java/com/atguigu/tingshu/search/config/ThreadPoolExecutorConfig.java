package com.atguigu.tingshu.search.config;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author fzx
 * @ClassName ThreadPoolExecutorConfig
 * @description: TODO
 * @date 2024年11月26日
 * @version: 1.0
 */
@Component
public class ThreadPoolExecutorConfig {

    @Bean
    public ThreadPoolExecutor threadPoolExecutor(){
        //  七大核心参数，核心线程个数如何设置 io:2n cpu:n+1 n:当前cpu核数，线程池工作原理！
        //  获取当前服务器的cpu核数
        int corePoolSize = Runtime.getRuntime().availableProcessors();
        return new ThreadPoolExecutor(
                corePoolSize,
                20,
                60,
                java.util.concurrent.TimeUnit.SECONDS,
                new java.util.concurrent.ArrayBlockingQueue<>(100),
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.CallerRunsPolicy());
    }
}
