package com.atguigu.tingshu.search.repository;

import com.atguigu.tingshu.model.search.AlbumInfoIndex;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * @author fzx
 * @ClassName AlbumIndexRepository
 * @description: TODO
 * @date 2024年11月26日
 * @version: 1.0
 */
public interface AlbumIndexRepository extends ElasticsearchRepository<AlbumInfoIndex,Long> {
}
