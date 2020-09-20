package com.atguigu.gmall.common.cache;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.constant.RedisConst;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * GmallCacheAspect 用于将数据放入缓存
 * 使用完整的分布式锁
 * 利用环绕通知获取对应的数据 做业务处理
 */
@Component
@Aspect// 表示切面类
public class GmallCacheAspect {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    /**
     * 利用 环绕通知 获取对应的数据 做业务处理
     *
     * @Around(" @annotation(com.atguigu.gmall.common.cache.GmallCache)") 使用环绕通知为 @GmallCache 增加功能。
     * 环绕通知要有参数ProceedingJoinPoint proceedingJoinPoint(该参数可以调用目标方法)
     * @GmallCache 注解将数据放入缓存。返回的数据类型是不确定的，是根据调用方法而言，所以使用 Object 。
     */
    @Around("@annotation(com.atguigu.gmall.common.cache.GmallCache)")
    public Object cacheAroundAdvice(ProceedingJoinPoint point) {
        // 创建一个 Object 用于接收数据
        Object object = null;

        // 使用 point.getSignature() 获取方法签名 使用 MethodSignature 接收方法签名
        MethodSignature signature = (MethodSignature) point.getSignature();

        // 通过方法签名 signature 获取方法上的注解
        GmallCache gmallCache = signature.getMethod().getAnnotation(GmallCache.class);

        // 获取注解的前缀
        String prefix = gmallCache.prefix();

        // 模拟缓存的key，比如是 sku:39。现在获取到前缀了，还需要获取参数...

        // 通过 point 获取方法的参数（有可能是多个参数 所以是数组）
        Object[] args = point.getArgs();

        // 定义一个缓存的 key。比如：key = sku:39
        String key = prefix + Arrays.asList(args).toString();

        // 通过 key 获取缓存中的数据
        object = caCheHit(key, signature);

        // 判断从缓存中获取到的数据是否为空
        if (object == null) {// 缓存中数据为空：查询 DB + 数据存进缓存
            try {
                // 定义一个锁的 key
                String lockKey = key + ":lock";

                // 使用 Redisson 做分布式锁
                RLock lock = redissonClient.getLock(lockKey);

                // 尝试加锁
                boolean isSuccess = lock.tryLock(RedisConst.SKULOCK_EXPIRE_PX1, RedisConst.SKULOCK_EXPIRE_PX2, TimeUnit.SECONDS);

                // 判断加锁是否成功
                if (isSuccess) {// true 加锁成功
                    try {
                        // 查询 DB 中的数据：使用 point.proceed(point.getArgs()) 调用目标方法
                        object = point.proceed(point.getArgs());

                        // 判断数据库中的数据是否为空【防止缓存穿透】
                        if (object == null) {
                            // 创建空对象
                            // 获取被调用的方法的返回值的类型
                            Class clazz = ((MethodSignature) point.getSignature()).getReturnType();
                            // 根据返回值类型使用 newInstance() 创建对象（默认调用的是无参构造器）
                            Object o = clazz.newInstance();

                            // 空对象放入缓存
                            redisTemplate.opsForValue().set(key, JSONObject.toJSONString(o), RedisConst.SKUKEY_TEMPORARY_TIMEOUT, TimeUnit.SECONDS);
                            //redisTemplate.opsForValue().set(key, o, RedisConst.SKUKEY_TEMPORARY_TIMEOUT, TimeUnit.SECONDS);

                            // 返回空对象或者null
                            return null;
                        }

                        // 将查询出来的真正的数据放入缓存
                        redisTemplate.opsForValue().set(key, JSONObject.toJSONString(object), RedisConst.SKUKEY_TIMEOUT, TimeUnit.SECONDS);

                        // 返回真正的数据
                        return object;

                    } catch (Throwable throwable) {
                        throwable.printStackTrace();
                    } finally {
                        // 释放锁
                        lock.unlock();
                    }

                } else {// false 加锁失败
                    Thread.sleep(1000);// 等待 尝试从缓存获取数据
                    caCheHit(key, signature);// 直接调用caCheHit获取缓存数据
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        } else {// 缓存中数据不为空
            return object;
        }
        return object;
    }

    /**
     * 通过 key 获取缓存中的数据
     *
     * @param key       缓存中的key
     * @param signature 方法签名 用于获取方法返回值的数据类型
     * @return
     */
    private Object caCheHit(String key, MethodSignature signature) {
        // 获取数据。redis 的 String 数据类型 ： key，value 都是字符串
        String dataInCache = (String) redisTemplate.opsForValue().get(key);

        if (!StringUtils.isEmpty(dataInCache)) {
            /**
             * 缓存中有数据：返回的数据类型是 执行方法 时 方法对应的返回类型
             * 比如：
             *      getSkuInfo()返回的是 SkuInfo ，需要将 cache 转换为 SkuInfo
             *      getSkuPrice()返回的是 BigDecimal，需要将 cache 转换为 BigDecimal
             */

            // 获取方法返回值的数据类型
            Class returnType = signature.getReturnType();

            // 转换数据类型并返回数据
            return JSONObject.parseObject(dataInCache, returnType);
        }
        return null;
    }
}
