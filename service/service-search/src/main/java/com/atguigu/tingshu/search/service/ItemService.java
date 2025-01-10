package com.atguigu.tingshu.search.service;

import java.util.Map;

public interface ItemService {


    /**
     * 根据专辑id获取专辑详情
     * @param albumId
     * @return
     */
    Map<String, Object> getAlbumInfoItem(Long albumId);
}
