package com.atguigu.tingshu.album.service;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.model.album.*;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface BaseCategoryService extends IService<BaseCategory1> {


    /**
     * 获取一级分类
     * @return
     */
    List<JSONObject> getBaseCategoryList();


    /**
     * 根据一级分类id获取属性数据
     * @param category1Id
     * @return
     */
    List<BaseAttribute> findAttribute(Long category1Id);

    /**
     * 根据三级分类id获取分类数据
     * @param category3Id
     * @return
     */
    BaseCategoryView getCategoryView(Long category3Id);

    /**
     * 根据一级分类id获取分类数据
     * @param category1Id
     * @return
     */
    JSONObject getBaseCategoryById(Long category1Id);

    /**
     * 根据一级分类id获取三级分类数据
     * @param category1Id
     * @return
     */
    List<BaseCategory3> findTopBaseCategory3(Long category1Id);
}
