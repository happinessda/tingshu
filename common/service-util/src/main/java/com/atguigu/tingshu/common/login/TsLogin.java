package com.atguigu.tingshu.common.login;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface TsLogin {

    /**
     * 表示是否需要登录 true:表示必须要登录，false:表示可以不需要登录
     * @return
     */
    boolean required() default true;
}
