package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Follow;
import com.hmdp.mapper.FollowMapper;
import com.hmdp.service.IFollowService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IUserService;
import com.hmdp.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {
     @Autowired
     private StringRedisTemplate stringRedisTemplate;
     @Autowired
     private IUserService userService;

    /**
     * 关注或取关用户
     * @param id
     * @param isFollow
     */
     @Override
    public void follow(Long id, Boolean isFollow) {
        //1.获取当前登录用户----若为空，显示：当前用户未登录
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            throw new RuntimeException("请先登录");
        }
        Long userId = user.getId();
        String key="follows:"+userId;
        //2.根据isFollow判断是关注还是取关
        if(isFollow){
            //3.若关注，则插入数据
            Follow follow = new Follow();  //设置关注对象
            follow.setUserId(userId);
            follow.setFollowUserId(id);
            follow.setCreateTime(LocalDateTime.now());
            int insert = baseMapper.insert(follow);
            if(insert > 0){
               //关注成功
                stringRedisTemplate.opsForSet().add(key,id.toString());
            }
        }else {
            //4，若取关，则删除数据
            int delete = baseMapper.delete(new QueryWrapper<Follow>()
                    .eq("user_id", userId)
                    .eq("follow_user_id", id));
            if(delete > 0){
                //取关成功
                stringRedisTemplate.opsForSet().remove(key,id.toString());
            }
        }

    }

    /**
     * 查询关注状态
     * @param followUserId
     * @return
     */
    @Override
    public Boolean isFollow(Long followUserId) {
        //1.获取当前登录用户
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            //未登录，默认未关注
            return false;
        }
        Long userId = user.getId();
        //2.根据当前登录用户和要判断的用户查询数据库是否有相关数据
        Integer count = Math.toIntExact(baseMapper.selectCount(new QueryWrapper<Follow>()
                .eq("user_id", userId)
                .eq("follow_user_id", followUserId)));
        //3.返回结果
        return count > 0;
    }

    /**
     * 共同关注
     * @param followUserId
     * @return
     */
    @Override
    public Result commonFollow(Long followUserId) {
        //1.获取当前登录用户
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            //未登录，默认未关注
            return Result.ok(Collections.emptyList());
        }
        Long userId = user.getId();
        //2.求交集
        String key1="follows:"+userId;
        String key2="follows:"+followUserId;
        Set<String> intersect = stringRedisTemplate.opsForSet().intersect(key1, key2);
        //3.解析集合
        if(intersect == null || intersect.isEmpty()){
            //无共同关注
            return Result.ok(Collections.emptyList());
        }
        List<Long> ids = intersect.stream().map(Long::valueOf).collect(Collectors.toList());
        //4.查询用户
        List<UserDTO> userDTOS = userService.listByIds(ids)
                .stream()
                .map(u -> BeanUtil.copyProperties(u, UserDTO.class))
                        .collect(Collectors.toList());
        //5.返回结果
        return Result.ok(userDTOS);
    }
}
