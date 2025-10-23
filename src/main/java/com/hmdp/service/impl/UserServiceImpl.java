package com.hmdp.service.impl;

import com.hmdp.utils.UserHolder;
import org.apache.commons.beanutils.BeanUtils;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.USER_SIGN_KEY;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    /**
     * 发送手机验证码
     * @param phone 手机号
     * @param session HttpSession
     * @return 验证码
     */
    @Override
    public Result sendCode(String phone, HttpSession session) {
        //1.校验手机号
        if(RegexUtils.isPhoneInvalid(phone)){
            //2.如果不符合，返回错误信息
            return Result.fail("手机号格式错误");
        }
        //3.符合，生成验证码
        String code = RandomUtil.randomNumbers(6);
        //4.保存验证码到session
        //session.setAttribute("code",code);
        //4.保存验证码到redis
        stringRedisTemplate.opsForValue().set(RedisConstants.LOGIN_CODE_KEY+phone,code,RedisConstants.LOGIN_CODE_TTL, TimeUnit.MINUTES);
        //5.发送验证码
        // TODO 发送验证码
        log.info("发送验证码成功，验证码为：{}",code);
        //5.返回验证码
        return Result.ok();
    }

    /**
     * 登录功能
     *
     * @param loginForm 登录参数，包含手机号、验证码；或者手机号、密码
     * @param session   HttpSession
     * @return
     */
    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        //1.校验手机号
        String phone = loginForm.getPhone();
        if(RegexUtils.isPhoneInvalid(phone)){
            //2.如果不符合，返回错误信息
            throw new IllegalArgumentException("手机号格式错误！");
        }
        //3.校验验证码
        String cacheCode = stringRedisTemplate.opsForValue().get(RedisConstants.LOGIN_CODE_KEY+phone);
        String code = loginForm.getCode();
        if(code==null || !code.equals(cacheCode)){
            //4.如果不一致，报错
            throw new IllegalArgumentException("验证码错误");
        }
        //5.根据手机号查询用户
        User user = query().eq("phone", phone).one();
        //6.若用户不存在，创建新用户
        if(user==null){
            User newUser=new User()
                    .setCreateTime(LocalDateTime.now())
                    .setPhone(phone)
                    .setNickName("user_"+RandomUtil.randomString(6));
            //7.保存用户
            save(newUser);
            user=newUser;
        }
        //8.此时用户一定存在，将用户保存到redis
          // 生成Token令牌--UUID
        String token = UUID.randomUUID().toString();
          // 将用户转换为hashmap类型
        UserDTO userDTO = new UserDTO();
        userDTO.setId(user.getId());
        userDTO.setNickName(user.getNickName());
        userDTO.setIcon(user.getIcon());
        //将userDTO转成map类型
        Map<String, String> map = new HashMap<>();
        map.put("id", userDTO.getId() != null ? userDTO.getId().toString() : "");
        map.put("nickName", userDTO.getNickName() != null ? userDTO.getNickName() : "");
        map.put("icon", userDTO.getIcon() != null ? userDTO.getIcon() : "");
        // 存储
        String tokenKey = RedisConstants.LOGIN_USER_KEY+token;
        stringRedisTemplate.opsForHash().putAll(tokenKey,map);
        //设置token有效期
        stringRedisTemplate.expire(tokenKey,RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);
        // 返回token
        return Result.ok(token);
    }

        /**
         * 签到功能
         */
    @Override
    public void sign() {
       //获取当前用户
        UserDTO user = UserHolder.getUser();
        //校验用户是否存在
        if(user==null){
            throw new IllegalArgumentException("用户未登录");
        }
        Long userId = user.getId();
        //获取日期
        LocalDateTime now = LocalDateTime.now();
       //拼接key  --sign:userId:yearmonth
        String key = USER_SIGN_KEY+userId+now.format(DateTimeFormatter.ofPattern("yyyyMM"));
       //获取今天是本月第几天
        Integer dayOfMonth = now.getDayOfMonth();
       //写入
        stringRedisTemplate.opsForValue().setBit(key,dayOfMonth-1,true);
    }

    /**
     * 签到统计
     * @return 签到次数
     */
    @Override
    public int signCount() {
        // 1.获取当前登录用户
        Long userId = UserHolder.getUser().getId();
        // 2.获取日期
        LocalDateTime now = LocalDateTime.now();
        // 3.拼接key
        String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        String key = USER_SIGN_KEY + userId + keySuffix;
        // 4.获取今天是本月的第几天
        int dayOfMonth = now.getDayOfMonth();
        // 5.获取本月截止今天为止的所有的签到记录，返回的是一个十进制的数字 BITFIELD sign:5:202203 GET u14 0
        List<Long> result = stringRedisTemplate.opsForValue().bitField(
                key,
                BitFieldSubCommands.create()
                        .get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth)).valueAt(0)
        );
        if (result == null || result.isEmpty()) {
            // 没有任何签到结果
            return 0;
        }
        Long num = result.get(0);
        if (num == null || num == 0) {
            return 0;
        }
        // 6.循环遍历
        int count = 0;
        while (true) {
            // 6.1.让这个数字与1做与运算，得到数字的最后一个bit位  // 判断这个bit位是否为0
            if ((num & 1) == 0) {
                // 如果为0，说明未签到，结束
                break;
            }else {
                // 如果不为0，说明已签到，计数器+1
                count++;
            }
            // 把数字右移一位，抛弃最后一个bit位，继续下一个bit位
            num >>>= 1;
        }
        return count;
    }

}