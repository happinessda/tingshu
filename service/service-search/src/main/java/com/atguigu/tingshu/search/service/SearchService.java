package com.atguigu.tingshu.search.service;

import com.atguigu.tingshu.model.search.AlbumInfoIndex;
import com.atguigu.tingshu.query.search.AlbumIndexQuery;
import com.atguigu.tingshu.vo.search.AlbumSearchResponseVo;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface SearchService {


    /**
     * 上架专辑
     * @param albumId
     */
    void upperAlbum(Long albumId);

    /**
     * 下架专辑
     * @param albumId
     */
    void lowerAlbum(Long albumId);

    /**
     * 检索
     * @param albumIndexQuery
     * @return
     */
    AlbumSearchResponseVo search(AlbumIndexQuery albumIndexQuery);

    /**
     * 获取频道页数据
     * @param category1Id
     * @return
     */
    List<Map<String,Object>> channel(Long category1Id);

    /**
     * 自动补全
     * @param keyword
     * @return
     */
    List<String> completeSuggest(String keyword);

    /**
     * 更新排行榜
     */
    void updateLatelyAlbumRanking();


    /**
     *  获取排行榜
     * @param baseCategory1Id
     * @param ranking
     * @return
     */
    List<AlbumInfoIndex> findRankingList(Long baseCategory1Id, String ranking);
}
