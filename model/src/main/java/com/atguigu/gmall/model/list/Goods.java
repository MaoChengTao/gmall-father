package com.atguigu.gmall.model.list;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.Date;
import java.util.List;

/**
 * 这个类就是我们要存储的商品数据，将商品数据放入index、type
 * 我们设计：index = goods、type = info
 * index、type如何指定？通过 spring-boot-starter-data-elasticsearch 添加注解表明
 * shards：分片 和 shards：副本 ===>>> 保证es查询效率，提高负载均衡用！
 */
@Data
@Document(indexName = "goods" ,type = "info",shards = 3,replicas = 2)
public class Goods {
    // skuInfo 基本信息************************
    // 商品Id
    @Id
    private Long id;

    // 默认tup
    @Field(type = FieldType.Keyword, index = false)
    private String defaultImg;

    // 商品名称
    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String title;

    // 商品价格
    @Field(type = FieldType.Double)
    private Double price;

    // 创建时间
    @Field(type = FieldType.Date)
    private Date createTime; // 新品

    // 品牌信息************************
    // 品牌id
    @Field(type = FieldType.Long)
    private Long tmId;

    // 品牌名称
    @Field(type = FieldType.Keyword)
    private String tmName;

    // 品牌logo
    @Field(type = FieldType.Keyword)
    private String tmLogoUrl;

    // 分类信息************************
    @Field(type = FieldType.Long)
    private Long category1Id;

    @Field(type = FieldType.Keyword)
    private String category1Name;

    @Field(type = FieldType.Long)
    private Long category2Id;

    @Field(type = FieldType.Keyword)
    private String category2Name;

    @Field(type = FieldType.Long)
    private Long category3Id;

    @Field(type = FieldType.Keyword)
    private String category3Name;

    @Field(type = FieldType.Long)
    private Long hotScore = 0L;

    // 平台属性集合对象************************
    // Nested 支持嵌套查询
    @Field(type = FieldType.Nested)
    private List<SearchAttr> attrs;

}
