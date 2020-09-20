package com.atguigu.gmall.list.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.list.repository.GoodsRepository;
import com.atguigu.gmall.list.service.SearchService;
import com.atguigu.gmall.model.list.*;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.client.ProductFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SearchServiceImpl implements SearchService {

    @Autowired
    private GoodsRepository goodsRepository;// 封装了操作 es 数据的CRUD

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RestHighLevelClient restHighLevelClient;// 操作 dsl 语句

    // 上架：将商品对应的 skuId 保存到 es 中
    @Override
    public void upperGoods(Long skuId) {
        // 创建 Goods 实例 封装数据
        Goods goods = new Goods();

        /*
        goods需要保存的数据：
            skuInfo的基本信息
            分类数据
            品牌数据
            平台属性集合
        */

        // 获取 skuInfo
        SkuInfo skuInfo = productFeignClient.getAttrValueList(skuId);

        // 给 goods 每一项赋值
        if (null != skuInfo) {// 判断 skuInfo 不为空
            // 设置 goods 的基本信息
            goods.setId(skuInfo.getId());
            goods.setDefaultImg(skuInfo.getSkuDefaultImg());
            goods.setTitle(skuInfo.getSkuName());
            goods.setPrice(skuInfo.getPrice().doubleValue());
            goods.setCreateTime(new Date());

            // 获取品牌信息
            BaseTrademark trademark = productFeignClient.getTrademark(skuInfo.getTmId());

            if (null != trademark) {
                // 设置 goods 的品牌信息
                goods.setTmId(trademark.getId());
                goods.setTmName(trademark.getTmName());
                goods.setTmLogoUrl(trademark.getLogoUrl());
            }

            // 获取分类数据
            BaseCategoryView categoryView = productFeignClient.getCategoryView(skuInfo.getCategory3Id());

            if (null != categoryView) {
                // 设置 goods 的分类数据
                goods.setCategory1Id(categoryView.getCategory1Id());
                goods.setCategory1Name(categoryView.getCategory1Name());
                goods.setCategory2Id(categoryView.getCategory2Id());
                goods.setCategory2Name(categoryView.getCategory2Name());
                goods.setCategory3Id(categoryView.getCategory3Id());
                goods.setCategory3Name(categoryView.getCategory3Name());
            }
        }

        // 获取平台属性值集合。目的：为了给 Goods 中的 List<SearchAttr> attrs 赋值
        List<BaseAttrInfo> attrList = productFeignClient.getAttrList(skuId);

        if (!CollectionUtils.isEmpty(attrList)) {
           /*
            Goods中存储的是List<SearchAttr>集合，需要为集合里的每一个 SearchAttr 对象赋值
                SearchAttr:
                    private Long attrId;
                    private String attrValue;
                    private String attrName;
                需要为这三个属性赋值，然后封装为集合。
             */
            // 注意将流转换为 list ！！！
            List<SearchAttr> searchAttrList = attrList.stream().map((baseAttrInfo) -> {
                // 创建一个 SearchAttr 对象
                SearchAttr searchAttr = new SearchAttr();
                // 设置平台属性Id
                searchAttr.setAttrId(baseAttrInfo.getId());
                // 设置平台属性名
                searchAttr.setAttrName(baseAttrInfo.getAttrName());

                // 获取平台属性集合对象
                List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();

                 /*
                 为什么 get(0) ？
                    因为通过 productFeignClient.getAttrList(skuId) 获取的数据中，每一行数据 对应的 平台属性 和 对应的平台属性值 只有一个!!!
                 */
                // 从平台属性集合中获取属性值名称
                String valueName = attrValueList.get(0).getValueName();

                // 设置平台属性值
                searchAttr.setAttrName(valueName);

                return searchAttr;
            }).collect(Collectors.toList());// 将 stream 流换换为 List 返回

            // 设置 goods 的平台属性集合
            goods.setAttrs(searchAttrList);

        }
        // 保存 goods 到 es 中
        goodsRepository.save(goods);
    }

    // 下架：将商品对应的 skuId 在 es 中删除
    @Override
    public void lowerGoods(Long skuId) {
        // 将商品对应的 skuId 在 es 中删除
        goodsRepository.deleteById(skuId);

    }

    @Override
    public void incrHotScore(Long skuId) {
        // 定义缓存数据的 key
        String hotKey = "hotScore";

        // 保存数据到缓存 (设置每被访问一次 +1)
        Double hotScoreCount = redisTemplate.opsForZSet().incrementScore(hotKey, "skuId:" + skuId, 1);

        // 累计被访问十次 就更新 es 商品中的 热度值
        if (hotScoreCount % 10 == 0) {
            // 通过 skuId 找到商品
            Optional<Goods> optional = goodsRepository.findById(skuId);
            Goods goods = optional.get();

            // 更新商品的 hotScore
            goods.setHotScore(Math.round(hotScoreCount));

            // 保存最新的商品数据到 es
            goodsRepository.save(goods);
        }
    }

    @Override
    public SearchResponseVo search(SearchParam searchParam) throws IOException {
        /*
        思路：
            1 获取用户的查询条件 转换为 dsl 语句
            2 执行 dsl 语句查询数据 返回查询的结果集
            3 将查询的结果集 转换为 检索结果集对象 SearchResponseVo
         */

        // 根据用户查询的条件构建 dsl 语句 (返回检索请求对象)
        SearchRequest searchRequest = buildQueryDsl(searchParam);

        // 执行 dsl 语句查询 (返回检索响应对象 即 结果集)
        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);

        // 将dsl查询的结果集转换为检索结果集实体：searchResponse --->>> SearchResponseVo
        SearchResponseVo searchResponseVo = parseSearchResult(searchResponse);

        // 赋值 searchResponseVo
        searchResponseVo.setPageNo(searchParam.getPageNo());
        searchResponseVo.setPageSize(searchParam.getPageSize());
        /*
            总页数 (pageTotal + pageSize - 1 ) / getPageSize\
            pageTotal总记录数：从查询的结果集 获取 封装到 SearchResponseVo
         */
        long totalPages = (searchResponseVo.getTotal() + searchParam.getPageSize() - 1) / searchParam.getPageSize();
        searchResponseVo.setTotalPages(totalPages);

        // 返回检索结果集对象 searchResponseVo
        return searchResponseVo;
    }

    /**
     * 转换查询结果集为搜索结果集实体
     * @param searchResponse 查询条件对象
     * @return 返回搜索结果集实体
     */
    private SearchResponseVo parseSearchResult(SearchResponse searchResponse) {
        // 创建一个搜索结果集实体的对象
        SearchResponseVo searchResponseVo = new SearchResponseVo();
        /*
         需要赋值：
            private List<SearchResponseTmVo> trademarkList;
            private List<SearchResponseAttrVo> attrsList = new ArrayList<>();
            private List<Goods> goodsList = new ArrayList<>();
            private Long total;// 总记录数
        */
        // 获取单独的 hits 节点数据
        SearchHits hits = searchResponse.getHits();

        // 获取商品信息集合 hits
        SearchHit[] subHits = hits.getHits();

        // 声明一个存储商品对象的集合
        List<Goods> goodsList = new ArrayList<>();

        if (null != subHits && subHits.length > 0) {
            // 循环遍历
            for (SearchHit subHit : subHits) {
                // 获取 source，source 对应着 Goods 实体类
                String sourceAsString = subHit.getSourceAsString();

                // 将字符串转换为实体类 设置转换的类型Goods.class
                Goods goods = JSONObject.parseObject(sourceAsString, Goods.class);

                // 获取高亮（细节：通过全文检索出来的 goods 的标题需要设置高亮）
                HighlightField highlightField = subHit.getHighlightFields().get("title");
                if (null != highlightField) {
                    // 获取高亮的 title
                    Text title = highlightField.getFragments()[0];

                    // 商品名称设置高亮
                    goods.setTitle(title.toString());
                }
                // 每循环一次将数据保存进 goodsList
                goodsList.add(goods);
            }
        }
        // 保存商品信息集合
        searchResponseVo.setGoodsList(goodsList);

        // 获取品牌数据 ：从聚合中的数据获取 无重复数据
        // 将 最外层聚合aggregations 看作 map
        Map<String, Aggregation> aggregationMap = searchResponse.getAggregations().getAsMap();

        // 将 tmIdAgg聚合 看做 map，key 是 tmIdAgg，value 是 tmIdAgg 里的数据。
        /*
            Aggregation tmIdAgg = tmIdMap.get("tmIdAgg"); Aggregation 无法获取  getBuckets()；
            需要使用 ParsedLongTerms 接收从而来获取 getBuckets()
        */
        ParsedLongTerms tmIdAgg = (ParsedLongTerms) aggregationMap.get("tmIdAgg");

        // 获取 tmIdAgg聚合 下的 buckets
        List<SearchResponseTmVo> trademarkList = tmIdAgg.getBuckets().stream().map((bucket) -> {
            // 创建一个品牌对象
            SearchResponseTmVo searchResponseTmVo = new SearchResponseTmVo();

            // 获取 品牌Id 并 赋值
            String tmId = bucket.getKeyAsString();
            searchResponseTmVo.setTmId(Long.parseLong(tmId));

            // 将 buckets 下的聚合作 map （是在tmIdAgg聚合基础之下的聚合，故取名 tmIdSubMap ）
            Map<String, Aggregation> tmIdSubMap = bucket.getAggregations().getAsMap();

            // 获取 品牌名称  并 赋值
            /*
                先获取聚合 tmNameAgg
                需要使用 ParsedStringTerms 接收从而来获取 getBuckets()里String类型的品牌名称
                Aggregation接收 无法获取 getBuckets()
             */
            ParsedStringTerms tmNameAgg = (ParsedStringTerms) tmIdSubMap.get("tmNameAgg");

            // 返回结果集 tmNameAgg 中的数据集合 buckets 中 key 只有一个值 故 tmNameAgg.getBuckets().get(0)
            String tmName = tmNameAgg.getBuckets().get(0).getKeyAsString();
            // 赋值 品牌名称
            searchResponseTmVo.setTmName(tmName);

            // 获取 品牌logoUrl 并 赋值
             /*
                先获取聚合 tmLogoUrlAgg
                需要使用 ParsedStringTerms 接收从而来获取 getBuckets()里String类型的品牌logoUrl
                Aggregation接收 无法获取 getBuckets()
             */
            ParsedStringTerms tmLogoUrlAgg = (ParsedStringTerms) tmIdSubMap.get("tmLogoUrlAgg");

            // 返回结果集 tmLogoUrlAgg 中的数据集合 buckets 中 key 只有一个值 故 tmLogoUrlAgg.getBuckets().get(0)
            String tmLogoUrl = tmLogoUrlAgg.getBuckets().get(0).getKeyAsString();
            // 赋值 品牌logoUrl
            searchResponseTmVo.setTmLogoUrl(tmLogoUrl);

            // 返回品牌对象searchResponseTmVo
            return searchResponseTmVo;
        }).collect(Collectors.toList());// 将流中的元素收集到 list
        // 保存品牌数据
        searchResponseVo.setTrademarkList(trademarkList);

        // 获取总记录数并保存
        long totalHits = hits.getTotalHits();
        searchResponseVo.setTotal(totalHits);

        // 获取平台属性集合
        // 获取attrsAgg聚合：数据类型是 nested 使用 ParsedNested 接收
        ParsedNested attrsAgg = (ParsedNested) aggregationMap.get("attrsAgg");

        // 获取 attrsAgg聚合 下的 attrIdAgg聚合 使用 ParsedLongTerms 接收调用 getBuckets()
        ParsedLongTerms attrIdAgg = attrsAgg.getAggregations().get("attrIdAgg");

        // 获取attrIdAgg聚合下的 buckets
        List<? extends Terms.Bucket> buckets = attrIdAgg.getBuckets();

        if (!CollectionUtils.isEmpty(buckets)) {

            List<SearchResponseAttrVo> attrsList = buckets.stream().map((bucket) -> {
                // 声明平台属性对象 SearchResponseAttrVo
                SearchResponseAttrVo searchResponseAttrVo = new SearchResponseAttrVo();

                // 获取 平台属性Id 并 赋值
                String attrId = bucket.getKeyAsString();
                searchResponseAttrVo.setAttrId(Long.parseLong(attrId));

                // 获取 平台属性名称 并 赋值
                // 先获取 tmNameAgg 聚合（获取String类型的平台属性名称 使用 ParsedStringTerms 接收）
                ParsedStringTerms tmNameAgg = bucket.getAggregations().get("attrNameAgg");
                // 通过 tmNameAgg 聚合获取 buckets 从而获取 平台属性名称
                String attrName = tmNameAgg.getBuckets().get(0).getKeyAsString();
                // 赋值
                searchResponseAttrVo.setAttrName(attrName);

                // 获取 平台属性值集合 并 赋值
                // 先获取 attrValueAgg 聚合（获取String类型的平台属性值 使用 ParsedStringTerms 接收）
                ParsedStringTerms attrValueAgg = bucket.getAggregations().get("attrValueAgg");
                // 通过 attrValueAgg 聚合获取 buckets 从而获取 平台属性值集合
                List<? extends Terms.Bucket> valueAggBucketList = attrValueAgg.getBuckets();
                // 通过 steam 流获取数据
                List<String> attrValueList = valueAggBucketList.stream().map((attrValueBucket) -> {
                    String attrValue = attrValueBucket.getKeyAsString();
                    return attrValue;
                }).collect(Collectors.toList());// steam 流 转换为 list
                // 赋值
                searchResponseAttrVo.setAttrValueList(attrValueList);

                // 返回平台属性对象
                return searchResponseAttrVo;

            }).collect(Collectors.toList());// stream 流 转换为 list

            // 保存平台属性集合
            searchResponseVo.setAttrsList(attrsList);
        }
        return searchResponseVo;
    }

    /**
     * 根据用户查询的条件构建dsl语句
     *
     * @param searchParam 查询条件对象
     * @return
     */
    private SearchRequest buildQueryDsl(SearchParam searchParam) {
        // 构建查询器
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        // query --->>> bool
        // 构建 BoolQueryBuilder
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

        // bool --->>> filter

        // 构建分类过滤 判断一级分类Id(用户在点击的时候，只能点击一个值，所以此处使用term)
        if (!StringUtils.isEmpty(searchParam.getCategory1Id())) {
            // filter --->>> term
            TermQueryBuilder category1Id = QueryBuilders.termQuery("category1Id", searchParam.getCategory1Id());

            // bool --->>> filter
            boolQueryBuilder.filter(category1Id);
        }
        // 构建分类过滤 判断二级分类Id
        if (!StringUtils.isEmpty(searchParam.getCategory2Id())) {
            // filter --->>> term
            TermQueryBuilder category2Id = QueryBuilders.termQuery("category2Id", searchParam.getCategory2Id());

            // bool --->>> filter
            boolQueryBuilder.filter(category2Id);
        }
        // 构建分类过滤 判断三级分类Id
        if (!StringUtils.isEmpty(searchParam.getCategory3Id())) {
            // filter --->>> term
            TermQueryBuilder category3Id = QueryBuilders.termQuery("category3Id", searchParam.getCategory3Id());

            // bool --->>> filter
            boolQueryBuilder.filter(category3Id);
        }

        // bool --->>> filter

        // filter --->>> term
        // 构建品牌查询
        // 获取品牌
        String trademark = searchParam.getTrademark();// 示例 trademark=2:华为
        if (!StringUtils.isEmpty(trademark)) {
            // 按照冒号分隔品牌 分隔后数组长度为2
            String[] split = trademark.split(":");
            // 判断分隔后的数据格式是否正确
            if (null != split && split.length == 2) {
                // 根据品牌id tmId 过滤 filter --->>> term "tmId":"4"
                TermQueryBuilder tmId = QueryBuilders.termQuery("tmId", split[0]);

                // bool --->>> filter
                boolQueryBuilder.filter(tmId);
            }
        }

        // bool --->>> filter

        // 构建平台属性查询
        /*
            示例 props=23:4G:运行内存
            平台属性Id:平台属性值:平台属性名
         */
        String[] props = searchParam.getProps();
        if (null != props && props.length > 0) {
            // 循环遍历获取数组里的数据
            for (String prop : props) { // prop=平台属性Id:平台属性值:平台属性名

                // 按冒号拆分数据 数组长度为3
                String[] split = prop.split(":");

                // 判断分隔后的数据格式是否正确
                if (null != split && split.length == 3) {

                    // 构建嵌套查询 boolQuery
                    BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

                    // 构建子查询 subBoolQuery
                    BoolQueryBuilder subBoolQuery = QueryBuilders.boolQuery();

                    // 子查询 subBoolQuery 设置子查询条件
                    // must --->>> term
                    subBoolQuery.must(QueryBuilders.termQuery("attrs.attrId", split[0]));// 平台属性Id
                    subBoolQuery.must(QueryBuilders.termQuery("attrs.attrValue", split[1]));// 平台属性值

                    // 嵌套查询 boolQuery 设置 nested 查询
                    // bool --->>> must --->>> nested
                    boolQuery.must(QueryBuilders.nestedQuery("attrs", subBoolQuery, ScoreMode.None));

                    // filter --->>> bool --->>> must --->>> nested（添加到整个过滤对象中）
                    boolQueryBuilder.filter(boolQuery);
                }
            }
        }

        // bool --->>> must
        // 构建关键字查询
        if (!StringUtils.isEmpty(searchParam.getKeyword())) {
            // MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder("title",searchParam.getKeyword());
            // operator(Operator.AND)：表示查询要同时满足关键字经过分词后所组成的不同的词
            // 比如：小米手机；分词后：小米 | 手机；查询的时候：小米And手机同时满足
            MatchQueryBuilder title = QueryBuilders.matchQuery("title", searchParam.getKeyword()).operator(Operator.AND);

            // bool --->>> must
            boolQueryBuilder.must(title);
        }

        // 调用 query 方法 执行查询
        searchSourceBuilder.query(boolQueryBuilder);

        // 构建排序
        /*
            默认按照热度排序。电商页面也可以选择排序：按销量、价格等排序。
            排序规则 1:hotScore | 2:price
            如果页面点击的是 综合(1) | 价格(2)，参数传递如：&order=1:asc
            &order=1:asc 表示热度升序排序  &order=1:desc 表示按热度降序排列
            &order=2:asc 表示价格升序排序  &order=2:desc 表示价格度降序排列
        */
        // 获取排序规则
        String order = searchParam.getOrder();// 1:asc...
        if (!StringUtils.isEmpty(order)) {
            // 按冒号分隔数据 分隔后数组长度为2
            String[] split = order.split(":");

            // 判断分隔后数据格式是否正确
            if (null != split && split.length == 2) {// 数据格式正确

                // 声明一个排序字段
                String field = null;

                // 判断排序字段
                switch (split[0]) {
                    case "1":
                        field = "hotScore";
                        break;
                    case "2":
                        field = "price";
                        break;
                }

                // 设置 es 排序规则
                searchSourceBuilder.sort(field, "asc".equals(split[1]) ? SortOrder.ASC : SortOrder.DESC);

            } else {// 如果数据格式不正确 给默认排序规则
                searchSourceBuilder.sort("hotScore", SortOrder.DESC);
            }
        }

        // 构建分页
        // 设置起始页 (pageNo-1)*PageSize
        int from = (searchParam.getPageNo() - 1) * searchParam.getPageSize();
        searchSourceBuilder.from(from);
        searchSourceBuilder.size(searchParam.getPageSize());

        // 构建高亮
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.field("title");// 设置高亮字段
        highlightBuilder.preTags("<span style=color:red>");// 设置前缀
        highlightBuilder.postTags("</span>");// 设置后缀

        searchSourceBuilder.highlighter(highlightBuilder);

        // 设置品牌聚合
        TermsAggregationBuilder tmIdAggregationBuilder = AggregationBuilders.terms("tmIdAgg").field("tmId")
                .subAggregation(AggregationBuilders.terms("tmNameAgg").field("tmName"))
                .subAggregation(AggregationBuilders.terms("tmLogoUrlAgg").field("tmLogoUrl"));

        searchSourceBuilder.aggregation(tmIdAggregationBuilder);

        // 设置平台属性聚合 数据类型是 nested
        searchSourceBuilder.aggregation(AggregationBuilders.nested("attrsAgg", "attrs")
                .subAggregation(AggregationBuilders.terms("attrIdAgg").field("attrs.attrId")
                        .subAggregation(AggregationBuilders.terms("attrValueAgg").field("attrs.attrValue"))
                        .subAggregation(AggregationBuilders.terms("attrNameAgg").field("attrs.attrName"))));

        // 结果集的过滤：查询的结果集中只要 "id","defaultImg","title","price" 四个字段
        searchSourceBuilder.fetchSource(new String[]{"id", "defaultImg", "title", "price"}, null);

        // 创建检索请求对象，指定查询的索引库、类型：GET /goods/info/_search
        SearchRequest searchRequest = new SearchRequest("goods");
        searchRequest.types("info");
        searchRequest.source(searchSourceBuilder);// 将 searchSourceBuilder 放入 source 中

        log.info("dsl语句 ===>>> " + searchSourceBuilder);

        // 返回检索请求对象
        return searchRequest;
    }
}
