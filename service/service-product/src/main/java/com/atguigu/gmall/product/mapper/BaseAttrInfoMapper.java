package com.atguigu.gmall.product.mapper;

import com.atguigu.gmall.model.product.BaseAttrInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BaseAttrInfoMapper extends BaseMapper<BaseAttrInfo> {
    /**
     * 查询平台属性集合：根据一级分类Id、二级分类Id、三级分类Id | 编写 xml 文件
     *
     * @param categoryId1
     * @param categoryId2
     * @param categoryId3
     * @return
     */
    List<BaseAttrInfo> getBaseAttrInfoList(@Param("categoryId1") Long categoryId1,
                                           @Param("categoryId2") Long categoryId2,
                                           @Param("categoryId3") Long categoryId3);

    // 根据 skuId 获取平台属性值集合
    List<BaseAttrInfo> selectBaseAttrInfoListBySkuId(@Param("skuId") Long skuId);
}
