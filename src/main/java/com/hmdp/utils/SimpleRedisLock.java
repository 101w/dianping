package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class SimpleRedisLock implements ILock {
    private  static final String LOCK_PREFIX = "lock:";
    private final String ID_PREFIX = UUID.randomUUID().toString()+":";
    private StringRedisTemplate stringRedisTemplate;
    private String name;
    public SimpleRedisLock(StringRedisTemplate stringRedisTemplate, String name) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.name = name;
    }

    /**
     * 尝试获取锁
     * @param timeoutSec 超时时间，单位秒
     * @return
     */
    @Override
    public boolean tryLock(long timeoutSec) {
        // 1. 构造锁的key
        String lockKey = LOCK_PREFIX + name;
        // 2. 构造锁的值
        String lockValue = ID_PREFIX + Thread.currentThread().getId();
        // 3. 尝试获取锁
        Boolean success = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, lockValue, timeoutSec, TimeUnit.SECONDS);
        // 4. 返回结果
        return Boolean.TRUE.equals(success);
    }
    /**
     * 释放锁
     */
    @Override
    public void unlock() {
        // 1. 构造锁的key
        String lockKey = LOCK_PREFIX + name;
        //获取线程标识
        String threadId = ID_PREFIX + Thread.currentThread().getId();
        //判断是否是当前线程的锁
        String currentLockValue = stringRedisTemplate.opsForValue().get(lockKey);
        if (currentLockValue == null) {
            return;
        }
        if (threadId.equals(currentLockValue)) {
            // 2. 释放锁
            stringRedisTemplate.delete(lockKey);
        }

    }
}
