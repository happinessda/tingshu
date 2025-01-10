package com.atguigu.tingshu.album.client.impl;


import com.atguigu.tingshu.album.client.AlbumInfoFeignClient;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.AlbumAttributeValue;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.vo.album.AlbumStatVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class AlbumInfoDegradeFeignClient implements AlbumInfoFeignClient {


    @Override
    public Result<AlbumInfo>  getAlbumInfo(Long albumId) {
        log.error("熔断...");
        AlbumInfo albumInfo = new AlbumInfo();
        albumInfo.setId(10L);
        albumInfo.setAlbumTitle("测试数据");
        return Result.ok(albumInfo);
    }

    @Override
    public Result<AlbumStatVo> getAlbumStatVo(Long albumId) {
        return null;
    }

    @Override
    public Result<List<AlbumAttributeValue>> findAlbumAttributeValue(Long albumId) {
        return null;
    }
}
