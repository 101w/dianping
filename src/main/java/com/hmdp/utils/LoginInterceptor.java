package com.hmdp.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


import java.util.Map;
import java.util.concurrent.TimeUnit;


@Slf4j
public class LoginInterceptor implements HandlerInterceptor{
    private StringRedisTemplate stringRedisTemplate;
    public LoginInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //1.获取请求头中的token
        String token = request.getHeader("authorization");
        if(StrUtil.isBlank(token)){
            //不存在，返回未登录错误
            response.setStatus(401);
            return false;
        }
        //2.基于token获取redis中的用户
        String key=RedisConstants.LOGIN_USER_KEY+token;
        //3.判断用户是否存在
        Map<Object,Object> userMap = stringRedisTemplate.opsForHash().entries(key);
        if(userMap.isEmpty()){
            //4.如果不存在，返回未登录错误
            response.setStatus(401);
            return false;
        }
        //5.用户存在，保存用户信息到ThreadLocal
        //将map中的数据转换为UserDTO对象
        UserDTO userDTO= new UserDTO();
        /**
        userDTO.setId(Long.valueOf(userMap.get("id").toString()));
        userDTO.setNickName(userMap.get("nickName").toString());
        userDTO.setIcon(userMap.get("icon").toString());**/
        // 安全地转换id
        Object idObj = userMap.get("id");
        if (idObj != null && !idObj.toString().trim().isEmpty()) {
            try {
                userDTO.setId(Long.valueOf(idObj.toString()));
            } catch (NumberFormatException e) {
                // 记录日志或处理异常
                log.warn("Failed to convert id: {}", idObj);
            }
        }

        // 安全地设置其他属性
        userDTO.setNickName(userMap.get("nickName") != null ? userMap.get("nickName").toString() : "");
        userDTO.setIcon(userMap.get("icon") != null ? userMap.get("icon").toString() : "");


        //将userDTO对象保存到ThreadLocal
        UserHolder.saveUser(userDTO);

        //刷新token有效期
        stringRedisTemplate.expire(key,RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);
        //6.放行
        return true;
    }
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        //7.移除用户
        UserHolder.removeUser();
    }
}