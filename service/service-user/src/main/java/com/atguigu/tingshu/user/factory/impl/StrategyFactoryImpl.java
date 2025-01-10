package com.atguigu.tingshu.user.factory.impl;

import com.atguigu.tingshu.user.factory.StrategyFactory;
import com.atguigu.tingshu.user.strategy.ItemTypeStrategy;
import com.atguigu.tingshu.user.strategy.impl.AlbumStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * @author fzx
 * @ClassName StrategyFactoryImpl
 * @description: TODO
 * @date 2024年12月09日
 * @version: 1.0
 */
@Service
public class StrategyFactoryImpl implements StrategyFactory {

    @Autowired
    private Map<String, ItemTypeStrategy> itemTypeStrategyMap;

    @Override
    public ItemTypeStrategy writePaiRecode(String itemType) {
        //  判断是否包含改类型
        if (itemTypeStrategyMap.containsKey(itemType)){
            //  1001； 1002；1003
            ItemTypeStrategy itemTypeStrategy = itemTypeStrategyMap.get(itemType);
            //  返回数据
            return itemTypeStrategy;
        }
        //  默认返回空
        return null;

    }
}
