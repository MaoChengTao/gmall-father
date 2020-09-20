package com.atguigu.gmall.product.service.impl;

import com.atguigu.gmall.product.service.TestService;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class TestServiceImpl implements TestService {

    // 引入操作 redis 的客户端工具类
    @Autowired
    private StringRedisTemplate redisTemplate;

    // 注入 RedissonClient
    @Autowired
    private RedissonClient redissonClient;

    /**
     * Redisson分布式锁：使用 Redisson 实现分布式锁
     *      @Autowired 注入 RedissonClient
     */
    @Override
    public void testLock() {

        // 1 从 RedissonClient 获取锁
        String skuId = "39";
        String lockKey = "lock:" + skuId;
        RLock lock = redissonClient.getLock(lockKey);

        // 2 上锁
        lock.lock();

        // 查询 redis 中 num 的值
        String value = redisTemplate.opsForValue().get("num");

        // 没有该值 return
        if (StringUtils.isEmpty(value)) {
            return;
        }

        // 有值就转成 int
        int num = Integer.parseInt(value);

        // 把 redis 中 num +1
        redisTemplate.opsForValue().set("num", String.valueOf(++num));

        // 3 释放锁
        lock.unlock();
    }

    /**
     * Redis分布式锁三：优化之LUA脚本保证删除的原子性
     * 问题描述：
     * 线程一：执行到判断，返回结果是true，还没有执行 delete，lock刚好过期时间已到。也就是说还差delete没执行。
     * 线程二：进来了，重新设了 uuid 和 缓存的值！
     * 此时，cpu 又开始执行线程一，线程一就会将线程二的锁给删除！！！
     * 这样删除就不具备原子性！！！
     */
    /*@Override
    public void testLock() {
        // 1 利用 redis setnx 来加锁，设置锁的过期时间、并设置 UUID 防误删
        // 1.1 设置 UUID 防误删
        String uuid = UUID.randomUUID().toString();

        // 1.2 结合商品详情设置一个新的 key
        String skuId = "39";// 访问 skuId 为 39 号的商品
        String lockKey = "lock:" + skuId; // 锁住的是每个商品的数据

        Boolean lock = redisTemplate.opsForValue().setIfAbsent(lockKey, uuid, 2, TimeUnit.SECONDS);

        if (lock) {// 如果返回 true 上锁成功

            // 查询 redis 中 num 的值
            String value = redisTemplate.opsForValue().get("num");

            // 没有该值 return
            if (StringUtils.isEmpty(value)) {
                return;
            }

            // 有值就转成 int
            int num = Integer.parseInt(value);

            // 把 redis 中 num +1
            redisTemplate.opsForValue().set("num", String.valueOf(++num));

            // 2 完成业务后 释放锁！！！需要判断 uuid！！！
            //if (uuid.equals(redisTemplate.opsForValue().get("lock"))) {
            // 线程一：执行到判断，返回结果是true，还没有执行 delete，lock刚好过期时间已到。也就是说还差delete没执行。
            // 线程二：进来了，重新设了 uuid 和 缓存的值！
            // 此时，cpu 又开始执行线程一，线程一就会将线程二的锁给删除！！！
            // 这样删除就不具备原子性！！！
            //redisTemplate.delete("lock");
            //}

            // 2 使用 Lua 脚本 删除锁

            // 2.1 定义 Lua 脚本：这个脚本只在客户端传入的值和键的口令串相匹配时，才对键进行删除。
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";

            // 2.2 将 Lua 脚本 放入 defaultRedisScript
            DefaultRedisScript defaultRedisScript = new DefaultRedisScript();
            defaultRedisScript.setScriptText(script);

            // 2.3  设置lua脚本返回类型为Long
            // 因为删除判断的时候， Lua 脚本返回的0，给其封装为 数值类型。如果不封装那么默认返回 String 类型，那么返回 字符串 与 0 会发生错误。
            defaultRedisScript.setResultType(Long.class);

            // 2.4 执行 Lua 脚本：第一个要是 script 脚本 ，第二个需要判断的 key，第三个就是 key 所对应的值
            redisTemplate.execute(defaultRedisScript, Arrays.asList(lockKey), uuid);

        } else {// 如果返回 false 说明：其余线程进来程序，执行加锁的代码，但是会返回false(因为当前线程没释放锁)
            try {
                // 3 等待之前的线程执行完 每隔1秒钟回调一次，再次尝试获取锁
                Thread.sleep(1000);
                testLock();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }*/

    /**
     * Redis分布式锁三：优化之UUID防误删
     * 场景：如果业务逻辑的执行时间是7s。执行流程如下
     * index1业务逻辑没执行完，3秒后锁被自动释放。
     * index2获取到锁，执行业务逻辑，3秒后锁被自动释放。
     * index3获取到锁，执行业务逻辑
     * index1业务逻辑执行完成，开始调用del释放锁，这时释放的是index3的锁，导致index3的业务只执行1s就被别人释放。
     * 最终等于没锁的情况。
     */
    /*@Override
    public void testLock() {
        // 1 利用 redis setnx 来加锁，设置锁的过期时间、并设置 UUID 防误删
        // 1.1 设置 UUID 防误删
        String uuid = UUID.randomUUID().toString();
        Boolean lock = redisTemplate.opsForValue().setIfAbsent("lock", uuid, 2, TimeUnit.SECONDS);

        if (lock) {// 如果返回 true 上锁成功

            // 查询 redis 中 num 的值
            String value = redisTemplate.opsForValue().get("num");

            // 没有该值 return
            if (StringUtils.isEmpty(value)) {
                return;
            }

            // 有值就转成 int
            int num = Integer.parseInt(value);

            // 把 redis 中 num +1
            redisTemplate.opsForValue().set("num", String.valueOf(++num));

            // 2 完成业务后 释放锁！！！需要判断 uuid！！！
            if (uuid.equals(redisTemplate.opsForValue().get("lock"))) {
                // 代码执行的 uuid 与 缓存中的 uuid 是一致 才释放锁
                redisTemplate.delete("lock");
            }

        } else {// 如果返回 false 说明：其余线程进来程序，执行加锁的代码，但是会返回false(因为当前线程没释放锁)
            try {
                // 3 等待之前的线程执行完 每隔1秒钟回调一次，再次尝试获取锁
                Thread.sleep(1000);
                testLock();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }*/

    /**
     * Redis分布式锁二：优化之设置锁的过期时间
     *  问题：setnx刚好获取到锁，业务逻辑出现异常，导致锁无法释放，这个锁会一直存在，导致后面的线程无法进来。
     *  解决：设置过期时间，自动释放锁。
     */
    /*@Override
    public void testLock() {
        // 1 利用 redis setnx 来加锁，并设置锁的过期时间
        Boolean lock = redisTemplate.opsForValue().setIfAbsent("lock", "athome", 2, TimeUnit.SECONDS);

        if (lock) {// 如果返回 true 上锁成功

            // 查询 redis 中 num 的值
            String value = redisTemplate.opsForValue().get("num");

            // 没有该值return
            if (StringUtils.isEmpty(value)) {
                return;
            }

            // 有值就转成 int
            int num = Integer.parseInt(value);

            // 把 redis 中 num +1
            redisTemplate.opsForValue().set("num", String.valueOf(++num));

            // 2 完成业务后 释放锁
            redisTemplate.delete("lock");

        } else {// 如果返回 false 说明：其余线程进来程序，执行加锁的代码，但是会返回false(因为当前线程没释放锁)
            try {
                // 3 等待之前的线程执行完 每隔1秒钟回调一次，再次尝试获取锁
                Thread.sleep(1000);
                testLock();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }*/

    /**
     * Redis分布式锁一：利用 setnx 来加锁
     * NX ：只在键不存在时，才对键进行设置操作。
     */
    /*@Override
    public void testLock() {

        // 1 利用 redis setnx 来加锁
        Boolean lock = redisTemplate.opsForValue().setIfAbsent("lock", "athome");

        if (lock) {// 如果返回 true 上锁成功

            // 查询 redis 中 num 的值
            String value = redisTemplate.opsForValue().get("num");
            System.out.println(value);

            // 没有该值return
            if (StringUtils.isEmpty(value)) {
                return;
            }

            // 有值就转成 int
            int num = Integer.parseInt(value);

            // 把 redis 中 num +1
            redisTemplate.opsForValue().set("num", String.valueOf(++num));

            // 2 完成业务后 释放锁
            redisTemplate.delete("lock");

        } else {// 如果返回 false 说明：其余线程进来程序，执行加锁的代码，但是会返回false(因为当前线程没释放锁)
            try {
                // 3 等待之前的线程执行完 每隔1秒钟回调一次，再次尝试获取锁
                Thread.sleep(1000);
                testLock();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }*/

    /**
     * 本地锁 synchronized
     */
   /* @Override
    public synchronized void testLock() {
        // 1 查询 redis 中 num 的值
        String value = redisTemplate.opsForValue().get("num");
        System.out.println(value);

        // 2 没有该值return
        if (StringUtils.isBlank(value)){
            return;
        }

        // 3 有值就转成 int
        int num = Integer.parseInt(value);

        // 4 把 redis 中 num +1
        redisTemplate.opsForValue().set("num", String.valueOf(++num));
    }*/
}
