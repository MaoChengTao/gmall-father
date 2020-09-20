package com.atguigu.gmall.user.service.impl;

import com.atguigu.gmall.model.user.UserInfo;
import com.atguigu.gmall.user.mapper.UserInfoMapper;
import com.atguigu.gmall.user.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    protected UserInfoMapper userInfoMapper;

    @Override
    public UserInfo login(UserInfo userInfo) {
        // 对用户输入的密码加密
        String password = userInfo.getPasswd();
        String newPassword= DigestUtils.md5DigestAsHex(password.getBytes());

        // 验证用户信息
        QueryWrapper<UserInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("login_name",userInfo.getLoginName());
        wrapper.eq("passwd",newPassword);

        UserInfo loginInfo = userInfoMapper.selectOne(wrapper);

        if (null != loginInfo){
            return loginInfo;
        }
        return null;
    }
}
