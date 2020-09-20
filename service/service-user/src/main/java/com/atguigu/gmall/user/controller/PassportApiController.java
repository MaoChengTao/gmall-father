package com.atguigu.gmall.user.controller;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.util.IpUtil;
import com.atguigu.gmall.model.user.UserInfo;
import com.atguigu.gmall.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/user/passport")
public class PassportApiController {

    @Autowired
    private UserService userService;

    @Autowired
    private RedisTemplate redisTemplate;

    @PostMapping("login")
    public Result login(@RequestBody UserInfo userInfo, HttpServletRequest request) {

        UserInfo loginInfo = userService.login(userInfo);

        if (null != loginInfo) {// 登录成功
            // 制作 token(uuid)
            String token = UUID.randomUUID().toString();

            // 定义 Map 保存用户信息（登录成功返回 token 与 用户信息）
            Map<String, Object> map = new HashMap<>();
            map.put("nickName", loginInfo.getNickName());
            map.put("token", token);

            // 将用户信息存入缓存
            // 定义缓存中的 key
            String userKey = RedisConst.USER_LOGIN_KEY_PREFIX + token;

            // 需要存储的用户信息：userId {判断是否登录}、ip{保证登录的时候是同一台电脑}
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("userId", loginInfo.getId());
            jsonObject.put("ip", IpUtil.getIpAddress(request));

            redisTemplate.opsForValue().set(userKey, jsonObject.toJSONString(), RedisConst.USERKEY_TIMEOUT, TimeUnit.SECONDS);

            // 返回 map 。token 与 用户信息记录到 cookie 里 由 js 完成。
            return Result.ok(map);

        } else { // 登录失败
            return Result.fail().message("用户名或密码错误!");
        }
    }

    @GetMapping("logout")
    public Result logout(HttpServletRequest request) {
        // 退出登录 删除缓存中的用户信息
        redisTemplate.delete(RedisConst.USER_LOGIN_KEY_PREFIX + request.getHeader("token"));
        return Result.ok();
    }
}
