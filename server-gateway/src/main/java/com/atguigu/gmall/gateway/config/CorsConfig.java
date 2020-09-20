package com.atguigu.gmall.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {
    @Bean // 将对象注入 springIOC 容器
    public CorsWebFilter corsWebFilter() {

        // 3 Cors跨域配置对象
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.addAllowedOrigin("*");// 允许所有域名跨域
        corsConfiguration.setAllowCredentials(true);// 允许携带cookie
        corsConfiguration.addAllowedMethod("*");// 允许所有请求的方法
        corsConfiguration.addAllowedHeader("*");// 允许携带所有请求头


        // 2 创建 配置源对象
        UrlBasedCorsConfigurationSource configSource = new UrlBasedCorsConfigurationSource();
        // 第一个参数 path：过滤拦截哪个路径
        // 第二个参数 CorsConfiguration 指定跨域的规则配置
        configSource.registerCorsConfiguration("/**",corsConfiguration);

        // 1 创建 CorsWebFilter 对象
        return new CorsWebFilter(configSource);
    }
}
