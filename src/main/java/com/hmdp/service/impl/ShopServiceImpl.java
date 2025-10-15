package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisConstants;
import lombok.val;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private RedisTemplate redisTemplate;
    /**
     * 根据id查询商铺信息
     * @param id 商铺id
     * @return 商铺详情数据
     */
    @Override
    public Result getShopById(Long id) {
        String key = RedisConstants.CACHE_SHOP_KEY+id;
        //1.从redis查询商铺缓存
        String shopJson = redisTemplate.opsForValue().get(key).toString();
        //2.如果存在，直接返回
        if(StrUtil.isNotBlank(shopJson)){
            return Result.ok(JSONUtil.toBean(shopJson,Shop.class));
        }
        //  3.如果不存在，查询数据库
        Shop shop = getById(id); //mybatis-plus查询数据库
        //  4.如果数据库不存在，返回错误
        if(shop == null){
            return Result.fail("商铺不存在");
        }
        //  6.存在，写入redis
        redisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(shop));
        // 7.返回
        return Result.ok(shop);
    }
}
