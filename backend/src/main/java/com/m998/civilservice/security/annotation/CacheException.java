package com.m998.civilservice.security.annotation;

import java.lang.annotation.*;

/**
 * 自定义注解，有该注解的缓存方法会抛出异常
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME) // 使得注解在运行时也保留，可以通过反射读取
public @interface CacheException {
}
