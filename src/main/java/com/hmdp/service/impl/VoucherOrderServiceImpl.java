package com.hmdp.service.impl;


import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.config.RedisIdWorker;
import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.hmdp.utils.UserHolder;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collections;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;
    @Resource
    private RedisIdWorker redisIdWorker;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private RedissonClient redissonClient;
    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }
//    /**
//     * 秒杀优惠券
//     * @param voucherId
//     * @return
//     */
//    @Override
//    public Result seckillVoucher(Long voucherId) {
//        //查询优惠券
//        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
//        //判断优惠券是否存在
//        if (voucher == null) {
//            return Result.fail("优惠券不存在");
//        }
//        //判断秒杀是否开始
//        if (voucher.getBeginTime().isAfter(LocalDateTime.now())) {
//            return Result.fail("秒杀未开始");
//        }
//        //判断秒杀是否结束
//        if (voucher.getEndTime().isBefore(LocalDateTime.now())) {
//            return Result.fail("秒杀已结束");
//        }
//        //判断库存是否充足
//        Integer stock = voucher.getStock();
//        if (stock <= 0) {
//            return Result.fail("库存不足");
//        }
//        Long userId = UserHolder.getUser().getId();
//        //创建锁对象
//       // SimpleRedisLock lock = new SimpleRedisLock(stringRedisTemplate, "order:" + userId);
//        RLock lock = redissonClient.getLock("lock:order:" + userId);
//        //尝试获取锁
//        boolean success = lock.tryLock();
//        if (!success) {
//            // 3. 如果获取锁失败，返回失败结果
//            return Result.fail("订单已存在");
//        }
//        // 3. 如果获取锁成功，执行业务逻辑
//        try {
//            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
//            return proxy.createVoucherOrder(voucherId);
//        } finally {
//            // 4. 释放锁
//            lock.unlock();
//        }
//    }


    @Override
    public Result seckillVoucher(Long voucherId) {
        //获取用户
        Long userId = UserHolder.getUser().getId();
        long orderId = redisIdWorker.nextId("order");
        // 1.执行lua脚本
        Long result = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(), userId.toString(), String.valueOf(orderId)
        );
        int r = result.intValue();
        // 2.判断结果是否为0
        if (r != 0) {
            // 2.1.不为0 ，代表没有购买资格
            return Result.fail(r == 1 ? "库存不足" : "不能重复下单");
        }
        //TODO 保存阻塞队列
        // 3.返回订单id
        return Result.ok(orderId);
    }

    @Transactional
    public Result createVoucherOrder(Long voucherId) {
        //判断该用户是否购买过
        Long userId = UserHolder.getUser().getId();
            //判断该用户是否购买过
            int count = Math.toIntExact(query().eq("user_id", userId).eq("voucher_id", voucherId).count());
            if (count > 0) {
                return Result.fail("用户已购买过该优惠券");
            }
            //扣减库存
            boolean success = seckillVoucherService.update()
                    .setSql("stock = stock - 1")
                    .eq("voucher_id", voucherId)
                    .gt("stock", 0)  // 乐观锁判断库存是否充足
                    .update();
            if (!success) {
                return Result.fail("库存不足");
            }
            //创建订单
            VoucherOrder voucherOrder = new VoucherOrder();
            voucherOrder.setVoucherId(voucherId);  // 代金券id
            //生成全局唯一订单id
            Long orderId = redisIdWorker.nextId("order");
            voucherOrder.setId(orderId);
            //设置用户id
            voucherOrder.setUserId(UserHolder.getUser().getId());
            boolean save = save(voucherOrder);
            if (!save) {
                return Result.fail("订单创建失败");
            }
            //返回订单id
            return Result.ok(orderId);
        }


}
