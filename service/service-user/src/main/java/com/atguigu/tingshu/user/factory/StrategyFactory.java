package com.atguigu.tingshu.user.factory;

import com.atguigu.tingshu.user.strategy.ItemTypeStrategy;

/**
 * @author fzx
 * @ClassName StrategyFactory
 * @description: TODO
 * @date 2024年12月09日
 * @version: 1.0
 */
public interface StrategyFactory {

    //  定义抽象方法; 解决接口选择问题！
    ItemTypeStrategy writePaiRecode(String itemType);

}
