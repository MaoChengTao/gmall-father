package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.list.client.ListFeignClient;
import com.atguigu.gmall.model.list.SearchParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 商品列表接口
 */
@Controller
@RequestMapping
public class ListController {

    @Autowired
    private ListFeignClient listFeignClient;

    /**
     * 搜索
     *
     * @param searchParam
     * @param model
     * @return
     */
    @GetMapping("list.html")
    public String search(SearchParam searchParam, Model model) {
        Result<Map> result = listFeignClient.list(searchParam);

        // 保存数据到 model(底层会保存进request域中) 包含有trademarkList、attrsList、goodsList等页面需要的数据
        model.addAllAttributes(result.getData());// ListApiController.list()->Result.ok(searchResponseVo);

        // 拼接url (通过 searchParam 确定用户检索的条件是什么 实现条件的拼接)
        String urlParam = makeUrlParam(searchParam);
        // 处理品牌条件回显
        String trademarkParam = makeTrademark(searchParam.getTrademark());
        // 处理平台属性条件回显
        List<Map<String, String>> propsParamList = makeProps(searchParam.getProps());
        // 处理排序
        Map<String, Object> orderMap = dealOrder(searchParam.getOrder());

        // 存储 searchParam：前台需要 searchParam 判断检索的条件 显示文本内容
        model.addAttribute("searchParam", searchParam);
        // 存储 urlParam ：返回拼接检索参数的url
        model.addAttribute("urlParam", urlParam);
        // 存储 trademarkParam ：页面用于品牌面包屑回显
        model.addAttribute("trademarkParam", trademarkParam);
        // 存储 propsParamList ： 页面用于平台属性面包屑回显
        model.addAttribute("propsParamList", propsParamList);
        // 存储 orderMap ：页面排序使用
        model.addAttribute("orderMap", orderMap);

        return "list/index";
    }

    /**
     * 处理排序
     * http://list.gmall.com/list.html?category3Id=61&order=1:asc
     *      页面需要 ${orderMap.type} ${orderMap.sort}
     *      ${orderMap.type} 指按照哪个字段排序 1=综合 2=价格
     *      ${orderMap.sort} 指排序规则 asc、desc
     * <p>
     * class OrderMap{
     *      Long type;
     *      String sort;
     * }
     * 看做一个 Map
     * map.put("type",1);
     * map.put("sort","asc");
     *
     * @param order
     * @return
     */
    private Map<String, Object> dealOrder(String order) {
        // 定义map保存排序字段
        Map<String, Object> map = new HashMap<>();

        // 判断传递的数据不能为空
        if (!StringUtils.isEmpty(order)) {// 如果排序参数不为空 制定排序
            // 拆分数据 order=1:asc
            String[] split = order.split(":");
            // 判断拆分后数据格式是否正确
            if (null != split && split.length == 2) {
                map.put("type", split[0]);
                map.put("sort", split[1]);
            }
        } else {// 如果排序参数为空 给一个默认排序规则
            map.put("type", "1");
            map.put("sort", "desc");
        }
        return map;
    }

    /**
     * 处理平台属性条件回显
     * http://list.gmall.com/list.html?category3Id=61&props=1:2800-4499:价格&props=2:6.55-6.64英寸:屏幕尺寸
     * 将平台属性和属性值看做 Map 可以选择多组平台属性 故为 List<Map<String, String>>
     *
     * @param props
     * @return
     */
    private List<Map<String, String>> makeProps(String[] props) {
        // 声明一个集合保存需要回显的平台属性
        List<Map<String, String>> list = new ArrayList<>();

        // 判断平台属性集合不为空
        if (null != props && props.length > 0) {
            // 循环遍历取出每一组平台属性 props=1:2800-4499:价格
            for (String prop : props) {
                // 分割数据
                String[] split = prop.split(":");
                // 判断分割后的数据格式是否正确
                if (null != split && split.length == 3) {
                    // 声明一个 Map 保存数据 [根据页面需要的数据存储key]
                    Map<String, String> map = new HashMap();
                    map.put("attrId", split[0]);
                    map.put("attrValue", split[1]);
                    map.put("attrName", split[2]);
                    // 将 map 放入集合
                    list.add(map);
                }
            }
        }
        return list;
    }

    /**
     * 处理品牌条件回显
     * http://list.gmall.com/list.html?category3Id=61&trademark=2:华为
     * 需要回显请求参数中的"华为"
     *
     * @param trademark
     * @return
     */
    private String makeTrademark(String trademark) {
        // 判断请求参数中的 trademark 不为空
        if (!StringUtils.isEmpty(trademark)) {
            // 分割请求参数 &trademark=2:华为
            String[] split = trademark.split(":");
            // 判断分隔后的数据格式是否正确
            if (null != split && split.length == 2) {
                return "品牌：" + split[1];
            }
        }
        return null;
    }

    /**
     * 拼接检索参数 urlParam 返回
     *
     * @param searchParam
     * @return
     */
    private String makeUrlParam(SearchParam searchParam) {
        // 创建一个StringBuilder对象 接收拼接的检索参数urlParam
        StringBuilder urlParam = new StringBuilder();

        // 检索入口：判断是否是通过关键字检索
        // http://list.gmall.com/list.html?keyword=手机
        if (searchParam.getKeyword() != null) {
            // 拼接检索条件
            urlParam.append("keyword=").append(searchParam.getKeyword());
        }
        // 检索入口：判断是否是通过一级分类id检索
        // http://list.gmall.com/list.html?category1Id=2
        if (searchParam.getCategory1Id() != null) {
            // 拼接检索条件
            urlParam.append("category1Id=").append(searchParam.getCategory1Id());
        }
        // 检索入口：判断是否是通过二级分类id检索
        // http://list.gmall.com/list.html?category2d=13
        if (searchParam.getCategory2Id() != null) {
            // 拼接检索条件
            urlParam.append("category2Id=").append(searchParam.getCategory2Id());
        }
        // 检索入口：判断是否是通过三级分类id检索
        // http://list.gmall.com/list.html?category3Id=61
        if (searchParam.getCategory3Id() != null) {
            // 拼接检索条件
            urlParam.append("category3Id=").append(searchParam.getCategory3Id());
        }

        // 通过品牌检索(在检索入口之上进行检索)
        // http://list.gmall.com/list.html?category3Id=61&trademark=4:华为
        if (searchParam.getKeyword() != null) {
            // 大于0说明是在检索入口基础之上进行的品牌检索
            if (urlParam.length() > 0) {
                // 在检索入口的基础之上拼接品牌条件
                urlParam.append("&trademark=").append(searchParam.getKeyword());
            }
        }

        // 通过平台属性检索(在检索入口之上进行检索)
        // http://list.gmall.com/list.html?category3Id=61&trademark=2:华为&props=23:4G:运行内存&props=10:128:机身存储

        if (searchParam.getProps() != null) {
            //循环获取数组中的数据
            for (String props : searchParam.getProps()) { // props=23:4G:运行内存
                // 大于0说明是在检索入口基础之上进行的平台属性值检索
                if (urlParam.length() > 0) {
                    // 在检索入口的基础之上拼接平台属性值
                    urlParam.append("&props=").append(props);
                }
            }
        }
        // 检索示例：http://list.gmall.com/list.html?category3Id=61&trademark=2:华为&props=23:4G:运行内存&props=10:128:机身存储
        // 前台页面通过控制器 list.html 检索，故在其之后拼接上检索的参数："list.html?" + urlParam.toString()
        return "list.html?" + urlParam.toString();
    }
}
