package com.atguigu.tingshu.search.repository;

import com.atguigu.tingshu.model.search.AlbumInfoIndex;
import com.atguigu.tingshu.model.search.SuggestIndex;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * @author fzx
 * @ClassName AlbumIndexRepository
 * @description: TODO
 * @date 2024年11月26日
 * @version: 1.0
 */
public interface SuggestIndexRepository extends ElasticsearchRepository<SuggestIndex,String> {
}
