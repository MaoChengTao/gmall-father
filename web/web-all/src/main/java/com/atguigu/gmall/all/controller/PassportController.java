package com.atguigu.gmall.all.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;

// 用户登录接口
@Controller
public class PassportController {
    // 登录的请求地址'http://passport.gmall.com/login.html?originUrl='+window.location.href

    @GetMapping("login.html")
    public String login(HttpServletRequest request){
        // 获取请求中的参数 originUrl
        String originUrl = request.getParameter("originUrl");
        // 将 originUrl 保存进 request
        request.setAttribute("originUrl",originUrl);
        // 跳转到登录页面
        return "login";
    }
}
