package com.atguigu.gmall.common.cache;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @Target(ElementType.METHOD) 注解使用在：方法上
 * @Retention(RetentionPolicy.RUNTIME) 注解的声明周期：运行时
 */

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface GmallCache {
    /**
     * 定义一个前缀 缓存key的前缀。用来区分是哪一个方法的缓存。
     *  比如： @GmallCache(prifix = "categoryView")
     *        @GmallCache(prifix = "price")
     */

    String prefix() default "cache";
}
