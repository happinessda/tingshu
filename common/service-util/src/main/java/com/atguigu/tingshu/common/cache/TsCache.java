package com.atguigu.tingshu.common.cache;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author fzx
 * @ClassName TsCache
 * @description: TODO
 * @date 2024年12月03日
 * @version: 1.0
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface TsCache {

    //  定义一个前缀: 组成缓存的key
    String prefix() default "cache:";

}
