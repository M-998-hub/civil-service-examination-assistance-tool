package com.m998.civilservice.security.aspect;

import com.m998.civilservice.security.annotation.CacheException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * Redis缓存切面，防止Redis宕机影响正常业务逻辑
 *
 * 功能说明：为缓存操作提供“容错保护”
 */
@Aspect
@Component
@Order(2) // 执行顺序：数字越小越先执行
public class RedisCacheAspect {
    private static Logger LOGGER = LoggerFactory.getLogger(RedisCacheAspect.class);

    /**
     * 切点定义：拦截哪些方法
     */
    @Pointcut("execution(public * com.m998.civilservice.service.*CacheService.*(..))")
    public void cacheAspect() {
    }

    /**
     * 环绕通知：在方法执行前后进行增强
     * @param joinPoint
     * @return
     * @throws Throwable
     */
    @Around("cacheAspect()")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取被拦截方法的签名
        Signature signature = joinPoint.getSignature();
        MethodSignature methodSignature = (MethodSignature) signature; // 将通用的 Signature 对象强制转换为更具体的 MethodSignature 对象
        Method method = methodSignature.getMethod(); // 获取方法对象（用于检查注解）
        Object result = null;
        try {
            // 执行原方法（调用缓存操作）
            result = joinPoint.proceed();
        } catch (Throwable throwable) {
            //有CacheException注解的方法需要抛出异常
            if (method.isAnnotationPresent(CacheException.class)) {
                // 有注解，向上抛出
                throw throwable;
            } else {
                // 无注解，只记录日志，不抛出异常，业务继续
                LOGGER.error(throwable.getMessage());
            }
        }
        return result;
    }

}
