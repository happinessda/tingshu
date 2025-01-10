package com.atguigu.tingshu.search.api;

import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.search.AlbumInfoIndex;
import com.atguigu.tingshu.query.search.AlbumIndexQuery;
import com.atguigu.tingshu.search.service.SearchService;
import com.atguigu.tingshu.vo.search.AlbumSearchResponseVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "搜索专辑管理")
@RestController
@RequestMapping("api/search/albumInfo")
@SuppressWarnings({"all"})
public class SearchApiController {

    @Autowired
    private SearchService searchService;

    /**
     * 获取排行榜
     * @param baseCategory1Id
     * @param ranking
     * @return
     */
    @Operation(summary = "获取排行榜")
    @GetMapping("/findRankingList/{baseCategory1Id}/{ranking}")
    public Result findRankingList(@PathVariable Long baseCategory1Id,
                                   @PathVariable String ranking){
        //  调用服务层方法
        List<AlbumInfoIndex> albumInfoIndexList = searchService.findRankingList(baseCategory1Id, ranking);
        //  返回数据
        return Result.ok(albumInfoIndexList);
    }

    /**
     * 更新排行榜
     * @return
     */
    @SneakyThrows
    @Operation(summary = "更新排行榜")
    @GetMapping("updateLatelyAlbumRanking")
    public Result updateLatelyAlbumRanking(){
        //  调用服务层方法
        searchService.updateLatelyAlbumRanking();
        //  返回数据
        return Result.ok();
    }

    /**
     * 自动补全
     * @param keyword
     * @return
     */
    @Operation(summary = "自动补全")
    @GetMapping("/completeSuggest/{keyword}")
    public Result completeSuggest(@PathVariable String keyword){
        //  调用服务层方法
        List<String> suggestList = searchService.completeSuggest(keyword);
        //  返回数据
        return Result.ok(suggestList);
    }

    /**
     * 频道页数据
     * @param category1Id
     * @return
     */
    @Operation(summary = "获取频道页数据")
    @GetMapping("/channel/{category1Id}")
    public Result channel(@PathVariable Long category1Id){
        //  调用服务层方法
        List<Map<String,Object>> mapList = searchService.channel(category1Id);
        //  返回数据
        return Result.ok(mapList);
    }

    /**
     *  检索
     * @param albumIndexQuery
     * @return
     */
    @Operation(summary = "检索")
    @PostMapping
    public Result search(@RequestBody AlbumIndexQuery albumIndexQuery){
        //  调用服务层方法
        AlbumSearchResponseVo searchResponseVo = searchService.search(albumIndexQuery);
        //  返回数据
        return Result.ok(searchResponseVo);
    }

    /**
     * 批量上架专辑
     * @return
     */
    @Operation(summary = "批量上架专辑")
    @GetMapping("/batchUpperAlbum")
    public Result batchUpperAlbum(){
        //  循环
        for (long i = 1; i <= 1500; i++) {
            searchService.upperAlbum(i);
        }
        //  返回数据
        return Result.ok();
    }

    /**
     * 上架专辑
     * @param albumId
     * @return
     */
    @Operation(summary = "上架专辑")
    @GetMapping("/upperAlbum/{albumId}")
    public Result upperAlbum(@PathVariable Long albumId){
        //  调用服务层方法
        this.searchService.upperAlbum(albumId);
        //  返回数据
        return Result.ok();
    }

    /**
     * 下架专辑
     * @param albumId
     * @return
     */
    @Operation(summary = "下架专辑")
    @GetMapping("/lowerAlbum/{albumId}")
    public Result lowerAlbum(@PathVariable Long albumId){
        //  调用服务层方法
        this.searchService.lowerAlbum(albumId);
        //  返回数据
        return Result.ok();
    }
}

