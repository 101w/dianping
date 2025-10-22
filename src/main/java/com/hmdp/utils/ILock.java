package com.hmdp.utils;

public interface ILock {
    /**
     * 尝试获取锁
     * @param timeoutSec 超时时间，单位秒
     * @return true表示获取成功，false表示获取失败
     */
     boolean tryLock(long timeoutSec);

     /**
      * 释放锁
      */
     void unlock();
}
