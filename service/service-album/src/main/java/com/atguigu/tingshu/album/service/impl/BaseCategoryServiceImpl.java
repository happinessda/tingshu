package com.atguigu.tingshu.album.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.tingshu.album.mapper.*;
import com.atguigu.tingshu.album.service.BaseCategoryService;
import com.atguigu.tingshu.common.cache.TsCache;
import com.atguigu.tingshu.model.album.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@SuppressWarnings({"all"})
public class BaseCategoryServiceImpl extends ServiceImpl<BaseCategory1Mapper, BaseCategory1> implements BaseCategoryService {

    @Autowired
    private BaseCategory1Mapper baseCategory1Mapper;

    @Autowired
    private BaseCategory2Mapper baseCategory2Mapper;

    @Autowired
    private BaseCategory3Mapper baseCategory3Mapper;


    @Autowired
    private BaseCategoryViewMapper baseCategoryViewMapper;

    @Autowired
    private BaseAttributeMapper baseAttributeMapper;

    @Override
    public List<BaseCategory3> findTopBaseCategory3(Long category1Id) {
        //  通过一级分类Id 查找 三级分类下的热门数据;
        //  select c2.id from base_category2 c2 where c2.category1_id = 1;
        List<BaseCategory2> baseCategory2List = baseCategory2Mapper.selectList(new LambdaQueryWrapper<BaseCategory2>().eq(BaseCategory2::getCategory1Id, category1Id));
        //  101,102,103
        List<Long> category2IdList = baseCategory2List.stream().map(BaseCategory2::getId).collect(Collectors.toList());
        //  select * from base_category3 c3 where c3.is_top = 1 and c3.category2_id in (101,102,103) limit 7;
        LambdaQueryWrapper<BaseCategory3> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BaseCategory3::getIsTop, 1).in(BaseCategory3::getCategory2Id, category2IdList).last(" limit 7 ");
        List<BaseCategory3> baseCategory3List = baseCategory3Mapper.selectList(wrapper);
        //  返回数据
        return baseCategory3List;
    }

    @Override
    public JSONObject getBaseCategoryById(Long category1Id) {
        //	创建对象
        JSONObject category1 = new JSONObject();
        //	查询一级分类表：
        BaseCategory1 baseCategory1 = baseCategory1Mapper.selectById(category1Id);
        LambdaQueryWrapper<BaseCategory2> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BaseCategory2::getCategory1Id, baseCategory1.getId());
        category1.put("categoryId", category1Id);
        category1.put("categoryName", baseCategory1.getName());
        //	查询二级分类数据
        List<BaseCategory2> baseCategory2List = baseCategory2Mapper.selectList(wrapper);
        List<JSONObject> categoryChild2List = baseCategory2List.stream().map(baseCategory2 -> {
            //	创建二级对象
            JSONObject category2 = new JSONObject();
            category2.put("categoryId", baseCategory2.getId());
            category2.put("categoryName", baseCategory2.getName());
            //	获取三级分类对象
            List<BaseCategory3> baseCategory3List = this.baseCategory3Mapper.selectList(new LambdaQueryWrapper<BaseCategory3>().eq(BaseCategory3::getCategory2Id, baseCategory2.getId()));
            List<JSONObject> categoryChild3List = baseCategory3List.stream().map(baseCategory3 -> {
                //	创建对象
                JSONObject category3 = new JSONObject();
                category3.put("categoryId", baseCategory3.getId());
                category3.put("categoryName", baseCategory3.getName());
                //	返回数据
                return category3;
            }).collect(Collectors.toList());
            category2.put("categoryChild", categoryChild3List);
            //	返回二级分类对象
            return category2;
        }).collect(Collectors.toList());
        //  存储数据
        category1.put("categoryChild", categoryChild2List);
        //	返回数据
        return category1;
    }

    @Override
    @TsCache(prefix = "category:info:")
    public BaseCategoryView getCategoryView(Long category3Id) {
        // select * from base_category_view where id = 1001;
        return baseCategoryViewMapper.selectById(category3Id);
    }

    @Override
    public List<BaseAttribute> findAttribute(Long category1Id) {
        //	调用mapper 层方法
        return baseAttributeMapper.selectAttribute(category1Id);
    }

    @Override
    public List<JSONObject> getBaseCategoryList() {
        //	创建集合
        List<JSONObject> list = new ArrayList<>();
        //	先查询所有的分类数据：一级，二级，三级； select * from base_category_view;
        List<BaseCategoryView> baseCategoryViewList = baseCategoryViewMapper.selectList(null);
        //	将这个List 集合数据转为 Map集合 key=category1Id; value=List<BaseCategoryView>;
        Map<Long, List<BaseCategoryView>> map = baseCategoryViewList.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory1Id));
        //	循环遍历当前map 集合;
        Iterator<Map.Entry<Long, List<BaseCategoryView>>> iterator = map.entrySet().iterator();
        //	第一次循环 获取到音乐
        while (iterator.hasNext()) {
            //	获取迭代器中的数据
            Map.Entry<Long, List<BaseCategoryView>> entry = iterator.next();
            //	获取key 与 value
            Long category1Id = entry.getKey();
            List<BaseCategoryView> categoryViewList = entry.getValue();
            //	创建一个类JSONObject
            JSONObject category1 = new JSONObject();
            //	获取分类Id
            category1.put("categoryId", category1Id);
            //	想办法查询数据并赋值！ 获取集合中的第一条数据对应的name数据！
            category1.put("categoryName", categoryViewList.get(0).getCategory1Name());
            //	声明一个集合来存储二级分类对象
            ArrayList<JSONObject> category2ChildList = new ArrayList<>();
            //	必须知道当前一级分类对应的二级分类数据！
            Map<Long, List<BaseCategoryView>> map1 = categoryViewList.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory2Id));
            Iterator<Map.Entry<Long, List<BaseCategoryView>>> iterator1 = map1.entrySet().iterator();
            while (iterator1.hasNext()) {
                Map.Entry<Long, List<BaseCategoryView>> entry1 = iterator1.next();
                //	获取二级分类Id
                Long category2Id = entry1.getKey();
                //	获取二级分类的集合数据
                List<BaseCategoryView> viewList = entry1.getValue();
                //	创建一个类JSONObject二级对象
                JSONObject category2 = new JSONObject();
                //	获取分类Id
                category2.put("categoryId", category2Id);
                category2.put("categoryName", viewList.get(0).getCategory2Name());
                //	获取三级分类数据
                List<JSONObject> category3List = viewList.stream().map(baseCategoryView -> {
                    //	创建一个类JSONObject三级对象
                    JSONObject category3 = new JSONObject();
                    Long category3Id = baseCategoryView.getCategory3Id();
                    String category3Name = baseCategoryView.getCategory3Name();
                    category3.put("categoryId", category3Id);
                    category3.put("categoryName", category3Name);
                    return category3;
                }).collect(Collectors.toList());
                category2.put("categoryChild", category3List);
                //	将二级分类数据添加到集合中;
                category2ChildList.add(category2);
            }
            category1.put("categoryChild", category2ChildList);
            //	编写实现类：
            list.add(category1);
        }
        //	返回数据
        return list;
    }
}
