package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisConstants;
import lombok.val;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private RedisTemplate redisTemplate;

    /**
     * 查询商铺类型列表
     * @return
     */
    @Override
    public Result getShopTypeList() {
        String key= RedisConstants.SHOP_TYPE_KEY;
        //1.从缓存中查询
        Object typeList = redisTemplate.opsForValue().get(key);
        //2.若缓存存在，直接返回
        if(typeList != null){
            //将字符串转换为对象
            List<ShopType> shopTypes = JSONUtil.toList((String) typeList, ShopType.class);
            return Result.ok(shopTypes);
        }
        //3.若缓存不存在，查询数据库
        List<ShopType> shopTypes = query().orderByAsc("sort").list();
        //4.若数据库不存在，返回空
        if(shopTypes.isEmpty()){
            return Result.ok(List.of());
        }
        //5.将查询结果写入缓存
        redisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(shopTypes));
        //6.返回结果
        return Result.ok(shopTypes);
    }
}
