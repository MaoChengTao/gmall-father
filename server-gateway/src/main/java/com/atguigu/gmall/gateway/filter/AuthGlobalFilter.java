package com.atguigu.gmall.gateway.filter;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.result.ResultCodeEnum;
import com.atguigu.gmall.common.util.IpUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 自定义全局过滤器
 */
@Component
public class AuthGlobalFilter implements GlobalFilter {

    @Autowired
    private RedisTemplate redisTemplate;

    // 使用插值表达式获取从web-all访问时需要登录的控制器
    @Value("${authUrls.url}")
    private String authUrls;

    // 创建一个Path匹配对象
    private AntPathMatcher antPathMatcher = new AntPathMatcher();

    /**
     * 过滤拦截方法 验证需要登录的请求
     *
     * @param exchange
     * @param chain
     * @return
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 思路：获取用户的请求的地址 验证是否有权访问、判断是否需要登录、保存用户信息

        // 使用 exchange 获取 request
        ServerHttpRequest request = exchange.getRequest();

        // 使用 request 获取 用户请求的地址
        String path = request.getURI().getPath();

        // 判断一：用户请求的 path 是内部接口则网关拦截不允许外部访问！
        if (antPathMatcher.match("/**/inner/**", path)) {
            // 获取响应 response
            ServerHttpResponse response = exchange.getResponse();
            //调用方法设置无权访问响应
            return out(response, ResultCodeEnum.PERMISSION);
        }

        // 判断二：访问哪些控制器需要登录
        // 获取用户Id -> 有用户id -> 说明登录
        String userId = getUserId(request);
        // 获取临时用户Id
        String userTempId = getUserTempId(request);

        // 验证 token 是否被盗用
        // getUserId()里通过判断ip来验证token是否被盗用 被盗用了则返回-1
        if ("-1".equals(userId)) {// true 表示 token 被盗用
            // 获取响应 response
            ServerHttpResponse response = exchange.getResponse();
            //调用方法设置无权访问响应
            return out(response, ResultCodeEnum.PERMISSION);
        }

        // 判断三：用户登录认证 (访问api接口、异步请求、校验用户必须登录，未登录不允许访问）
        if (antPathMatcher.match("/api/**/auth/**", path)) {
            if (StringUtils.isEmpty(userId)) {
                // 获取响应 response
                ServerHttpResponse response = exchange.getResponse();
                //调用方法设置未登陆响应
                return out(response, ResultCodeEnum.LOGIN_AUTH);
            }
        }

        // 判断四：用户访问 web-all 时需要登录的控制器
        /*
            验证用户访问 web-all 时 需要登录的控制器 从配置文件获取控制器地址
            authUrls = authUrls.url:trade.html,myOrder.html,list.html
        */
        String[] split = authUrls.split(",");
        // 循环判断是否包含：trade.html、myOrder.htm、list.html
        for (String authUrl : split) {
            // 当前的 url 包含需要登录的控制器域名，但是用户未登录Id为空
            if (path.indexOf(authUrl) != -1 && StringUtils.isEmpty(userId)) { // 未登录
                // 提示登录 重定向到登录页面
                // 获取响应 response
                ServerHttpResponse response = exchange.getResponse();

                // 设置状态码303：请求的资源由另一个url处理 需要重定向
                response.setStatusCode(HttpStatus.SEE_OTHER);

                // 设置响应头 指定重定向的地址
                response.getHeaders().set(HttpHeaders.LOCATION, "http://www.gmall.com/login.html?originUrl=" + request.getURI());

                // 重定向到登录页面
                return response.setComplete();
            }
        }

        // 如果用户已经登录：从网关模块获取 userId 传递给后端的各个微服务模块
        if (!StringUtils.isEmpty(userId) || !StringUtils.isEmpty(userTempId)) {
            if (!StringUtils.isEmpty(userId)) {
                // 将 用户id 放入请求头 header 中
                request.mutate().header("userId", userId).build();
            }

            if (!StringUtils.isEmpty(userTempId)) {
                // 将 userTempId 放入请求头 header 中
                request.mutate().header("userTempId", userTempId).build();
            }

            // 因为 用户 id 在 request 的 header 中，需要将 request 变成 exchange 对象
            // exchange.mutate().request(request).build():将现在的 request 变成 exchange 对象
            return chain.filter(exchange.mutate().request(request).build());
        }
        return chain.filter(exchange);
    }

    /**
     * 获取临时用户Id
     *
     * @param request
     * @return
     */
    private String getUserTempId(ServerHttpRequest request) {
        // userTempId 被保存在 cookie 和 header 中
        String userTempId = "";

        List<String> list = request.getHeaders().get("userTempId");

        if (!CollectionUtils.isEmpty(list)) {

            userTempId = list.get(0);

        } else {
            HttpCookie httpCookie = request.getCookies().getFirst("userTempId");

            if (httpCookie != null) {

                userTempId = httpCookie.getValue();

                return userTempId;
            }
        }
        return userTempId;
    }

    /**
     * 获取用户Id
     *
     * @param request
     * @return
     */
    private String getUserId(ServerHttpRequest request) {
        /*
        用户ID存储在缓存中 String userKey = RedisConst.USER_LOGIN_KEY_PREFIX + token;
        获取token 拼接key去缓存中查询用户信息
        token 分别存储在 cookie 和 header 中【详见页面js代码】
         */

        // 声明一个 token
        String token = "";

        // 获取 header 中的 token
        List<String> tokenList = request.getHeaders().get("token");

        if (!CollectionUtils.isEmpty(tokenList)) {// header中有数据

            token = tokenList.get(0);

        } else {// header中无数据 从 cookie 获取

            HttpCookie httpCookie = request.getCookies().getFirst("token");

            if (httpCookie != null) {

                token = httpCookie.getValue();
            }
        }

        // 如果 token 不为空 利用 token 缓存中获取用户信息
        if (!StringUtils.isEmpty(token)) {
            // 拼接缓存中的key
            String userKey = "user:login:" + token;
            // 获取缓存中的用户信息
            String userStr = (String) redisTemplate.opsForValue().get(userKey);
            // 将字符串转换为
            JSONObject userJson = JSONObject.parseObject(userStr);
            // 获取缓存中的 ip
            String ip = userJson.getString("ip");
            // 获取当前用户登录的ip
            String ipAddress = IpUtil.getGatwayIpAddress(request);
            // 校验 token 是否被盗用
            if (ip.equals(ipAddress)) {// 当前用户登录的ip与缓存中的ip【一致】
                // 获取缓存中的 userId
                String userId = userJson.getString("userId");
                // 返回 userId
                return userId;
            } else {// 当前用户登录的ip与缓存中的ip【不一致】
                return "-1";
            }
        }
        return "";
    }

    /**
     * 接口鉴权失败返回数据(输出信息到页面)
     *
     * @param response
     * @param resultCodeEnum
     * @return
     */
    private Mono<Void> out(ServerHttpResponse response, ResultCodeEnum resultCodeEnum) {
        // 从 ResultCodeEnum 获取用户没有权限登录的信息 返回用户没有权限登录
        Result<Object> result = Result.build(null, resultCodeEnum);

        // 将 result 对象里的数据写入页面

        /*
            需要对 result 对象进行转换 如果涉及中文 需要设置字符集
         */

        // 将 result 对象转换为字符串 -> 再转换为字节数组
        byte[] bytes = JSONObject.toJSONString(result).getBytes();

        // 将字节数组转变为一个数据流
        DataBuffer wrap = response.bufferFactory().wrap(bytes);

        // 设置页面的头部信息
        response.getHeaders().add("Content-Type", "application/json;charset=UTF-8");

        // 使用 response 的输出流输出信息到页面
        return response.writeWith(Mono.just(wrap));
    }
}
