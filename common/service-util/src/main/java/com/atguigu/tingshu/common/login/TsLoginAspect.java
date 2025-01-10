package com.atguigu.tingshu.common.login;

import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.result.ResultCodeEnum;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.user.UserInfo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

/**
 * @author fzx
 * @ClassName TsLoginAspect
 * @description: TODO
 * @date 2024年11月23日
 * @version: 1.0
 */
@Component
@Aspect
public class TsLoginAspect {

    @Autowired
    private RedisTemplate redisTemplate;

    //   @annotation(org.springframework.transaction.annotation.Transactional)
    //  环绕通知：切注解TsLogin 这个注解;
    @Around("execution(* com.atguigu.tingshu.*.api.*.*(..)) && @annotation(tsLogin)")
    public Object doTsLoginAspect(ProceedingJoinPoint point,TsLogin tsLogin) throws Throwable {
        Object obj = new Object();
        //  如何判断用户是否登录的?
        /*
        1.  需要通过token 来判断用户是否登录!
            如果请求头中有token，则说明登录！否则，没有登录！
         */
        //  获取到请求头中的 token
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) requestAttributes;
        //  获取的请求对象
        HttpServletRequest request = servletRequestAttributes.getRequest();
        //  获取到响应对象
        //  HttpServletResponse response = servletRequestAttributes.getResponse();
        String token = request.getHeader("token");
        //  判断注解的属性：
        if (tsLogin.required()){
            //  判断token 是否为空
            if (StringUtils.isEmpty(token)){
                //  抛出异常
                throw new GuiguException(ResultCodeEnum.LOGIN_AUTH);
            }
            //  根据token 从缓存(redis)中获取用户信息数据; user_info==UserInfo;
            String userLoginKey = RedisConstant.USER_LOGIN_KEY_PREFIX + token;
            UserInfo userInfo = (UserInfo) this.redisTemplate.opsForValue().get(userLoginKey);
            //  判断
            if (null == userInfo){
                //  抛出异常
                throw new GuiguException(ResultCodeEnum.LOGIN_AUTH);
            }
        }
        try {
            //  说明此处不需要登录.
            if (!StringUtils.isEmpty(token)){
                //  从缓存中获取数据
                String userLoginKey = RedisConstant.USER_LOGIN_KEY_PREFIX + token;
                UserInfo userInfo = (UserInfo) this.redisTemplate.opsForValue().get(userLoginKey);
                if (null != userInfo){
                    //  存储 用户Id 到本地线程
                    AuthContextHolder.setUserId(userInfo.getId());
                }
            }
            //  执行方法体
            return point.proceed();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        } finally {
            //  释放本地内存，防止ThreadLocal 内存泄漏！
            AuthContextHolder.removeUserId();
        }
    }

}
