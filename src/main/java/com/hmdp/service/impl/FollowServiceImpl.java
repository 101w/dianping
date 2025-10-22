package com.hmdp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Follow;
import com.hmdp.mapper.FollowMapper;
import com.hmdp.service.IFollowService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.UserHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

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
        //2.根据isFollow判断是关注还是取关
        if(isFollow){
            //3.若关注，则插入数据
            Follow follow = new Follow();  //设置关注对象
            follow.setUserId(userId);
            follow.setFollowUserId(id);
            follow.setCreateTime(LocalDateTime.now());
            baseMapper.insert(follow);
        }else {
            //4，若取关，则删除数据
            baseMapper.delete(new QueryWrapper<Follow>()
                    .eq("user_id", userId)
                    .eq("follow_user_id", id));
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
}
