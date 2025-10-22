package com.hmdp.controller;


import com.hmdp.dto.Result;
import com.hmdp.service.IFollowService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/follow")
@Slf4j
public class FollowController {
    @Autowired
    private IFollowService followService;
    /**
     * 关注或取关用户
     */
    @PutMapping("/{id}/{isFollow}")
    public Result follow (@PathVariable("id") Long id, @PathVariable("isFollow") Boolean isFollow) {
        log.info("关注或取关用户，id：{}，isFollow：{}", id, isFollow);
        followService.follow(id, isFollow);
        return Result.ok();
    }
    /**
     * 查询关注状态
     */
    @GetMapping("/or/not/{id}")
    public Result isFollow (@PathVariable("id") Long followUserId) {
        log.info("查询关注状态，id：{}", followUserId);
        Boolean isFollow = followService.isFollow(followUserId);
        return Result.ok(isFollow);
    }

    /**
     * 共同关注
     */
     @GetMapping("/common/{id}")
    public Result commonFollow (@PathVariable("id") Long followUserId) {
        log.info("查询共同关注，id：{}", followUserId);
        Result result = followService.commonFollow(followUserId);
        return Result.ok(result);
    }
}
