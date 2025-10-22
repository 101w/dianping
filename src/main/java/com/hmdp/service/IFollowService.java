package com.hmdp.service;

import com.hmdp.entity.Follow;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IFollowService extends IService<Follow> {

    /**
     * 关注或取关用户
     */
    void follow(Long id, Boolean isFollow);

    /**
     * 查询关注状态
     */
    Boolean isFollow(Long followUserId);
}
