package com.atguigu.tingshu.search.service.impl;

import com.atguigu.tingshu.album.client.AlbumInfoFeignClient;
import com.atguigu.tingshu.album.client.CategoryFeignClient;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import com.atguigu.tingshu.search.service.ItemService;
import com.atguigu.tingshu.user.client.UserInfoFeignClient;
import com.atguigu.tingshu.vo.album.AlbumStatVo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class ItemServiceImpl implements ItemService {

    @Autowired
    private AlbumInfoFeignClient albumInfoFeignClient;

    @Autowired
    private CategoryFeignClient categoryFeignClient;

    @Autowired
    private UserInfoFeignClient userInfoFeignClient;

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    @Autowired
    private RedissonClient redissonClient;

    @Override
    public Map<String, Object> getAlbumInfoItem(Long albumId) {
        //  创建map集合
        Map<String, Object> result = new HashMap<>();

        //  获取布隆过滤器
        //        RBloomFilter<Object> bloomFilter = redissonClient.getBloomFilter(RedisConstant.ALBUM_BLOOM_FILTER);
        //        if (!bloomFilter.contains(albumId)){
        //            //  返回数据
        //            return result;
        //        }

        //  应该使用多线程优化:
        CompletableFuture<AlbumInfo> albumInfoCompletableFuture = CompletableFuture.supplyAsync(() -> {
            //  远程调用获取数据
            Result<AlbumInfo> albumInfoResult = albumInfoFeignClient.getAlbumInfo(albumId);
            Assert.notNull(albumInfoResult, "专辑信息结果集为空");
            AlbumInfo albumInfo = albumInfoResult.getData();
            Assert.notNull(albumInfo, "专辑信息为空");
            result.put("albumInfo", albumInfo);
            //  返回数据
            return albumInfo;
        },threadPoolExecutor);
        //  获取分类数据
        CompletableFuture<Void> baseCategoryViewCompletableFuture = albumInfoCompletableFuture.thenAcceptAsync(albumInfo -> {
            //  远程调用
            Result<BaseCategoryView> categoryViewResult = categoryFeignClient.getCategoryView(albumInfo.getCategory3Id());
            //  判断
            Assert.notNull(categoryViewResult, "分类结果集为空");
            BaseCategoryView baseCategoryView = categoryViewResult.getData();
            //  判断
            Assert.notNull(baseCategoryView, "分类对象为空");
            result.put("baseCategoryView", baseCategoryView);
        },threadPoolExecutor);

        //  获取主播信息
        CompletableFuture<Void> userCompletableFuture = albumInfoCompletableFuture.thenAcceptAsync(albumInfo -> {
            //  远程调用
            Result<UserInfoVo> userInfoVoResult = userInfoFeignClient.getUserInfoVo(albumInfo.getUserId());
            //  判断
            Assert.notNull(userInfoVoResult, "用户信息结果集为空");
            UserInfoVo userInfoVo = userInfoVoResult.getData();
            //  判断
            Assert.notNull(userInfoVo, "用户信息为空");
            result.put("announcer", userInfoVo);
        },threadPoolExecutor);

        CompletableFuture<Void> statCompletableFuture = CompletableFuture.runAsync(() -> {
            //  远程调用根据专辑Id获取专辑统计方法.
            Result<AlbumStatVo> albumStatVoResult = albumInfoFeignClient.getAlbumStatVo(albumId);
            Assert.notNull(albumStatVoResult, "专辑统计结果集为空");
            AlbumStatVo albumStatVo = albumStatVoResult.getData();
            //  判断
            Assert.notNull(albumStatVo, "专辑统计对象为空");
            result.put("albumStatVo", albumStatVo);
        },threadPoolExecutor);
        //  多任务组合
        CompletableFuture.allOf(
                albumInfoCompletableFuture,
                statCompletableFuture,
                baseCategoryViewCompletableFuture,
                userCompletableFuture
        ).join();
        //  返回数据
        return result;
    }
}
