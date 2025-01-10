package com.atguigu.tingshu.search.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.Suggestion;
import com.alibaba.fastjson.JSON;
import com.atguigu.tingshu.album.client.AlbumInfoFeignClient;
import com.atguigu.tingshu.album.client.CategoryFeignClient;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.PinYinUtils;
import com.atguigu.tingshu.model.album.*;
import com.atguigu.tingshu.model.search.AlbumInfoIndex;
import com.atguigu.tingshu.model.search.AttributeValueIndex;
import com.atguigu.tingshu.model.search.SuggestIndex;
import com.atguigu.tingshu.query.search.AlbumIndexQuery;
import com.atguigu.tingshu.search.repository.AlbumIndexRepository;
import com.atguigu.tingshu.search.repository.SuggestIndexRepository;
import com.atguigu.tingshu.search.service.SearchService;
import com.atguigu.tingshu.user.client.UserInfoFeignClient;
import com.atguigu.tingshu.vo.search.AlbumInfoIndexVo;
import com.atguigu.tingshu.vo.search.AlbumSearchResponseVo;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.suggest.Completion;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;


@Slf4j
@Service
@SuppressWarnings({"all"})
public class SearchServiceImpl implements SearchService {

    @Autowired
    private AlbumInfoFeignClient albumInfoFeignClient;

    @Autowired
    private CategoryFeignClient categoryFeignClient;

    @Autowired
    private UserInfoFeignClient userInfoFeignClient;
    //  内嵌了很多api 方法，同时启动的时候还会根据实体类的属性注解自动生成mapping结构,索引库！
    @Autowired
    private AlbumIndexRepository albumIndexRepository;

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    @Autowired
    private ElasticsearchClient elasticsearchClient;

    @Autowired
    private SuggestIndexRepository suggestIndexRepository;

    @Autowired
    private RedisTemplate redisTemplate;


    @Override
    public void upperAlbum(Long albumId) {
        //  创建对象
        AlbumInfoIndex albumInfoIndex = new AlbumInfoIndex();

        CompletableFuture<AlbumInfo> albumInfoCompletableFuture = CompletableFuture.supplyAsync(() -> {
            //  获取专辑信息.
            Result<AlbumInfo> infoResult = albumInfoFeignClient.getAlbumInfo(albumId);
            //  获取数据
            Assert.notNull(infoResult, "专辑信息结果集为空");
            AlbumInfo albumInfo = infoResult.getData();
            Assert.notNull(albumInfo, "专辑信息为空");
            //  属性拷贝：
            BeanUtils.copyProperties(albumInfo, albumInfoIndex);
            return albumInfo;
        }, threadPoolExecutor);
        //  获取分类数据
        CompletableFuture<Void> categoryCompletableFuture = albumInfoCompletableFuture.thenAcceptAsync(albumInfo -> {
            //  根据三级分类Id获取到分类视图对象.
            Result<BaseCategoryView> baseCategoryViewResult = categoryFeignClient.getCategoryView(albumInfo.getCategory3Id());
            Assert.notNull(baseCategoryViewResult, "分类结果集为空");
            BaseCategoryView baseCategoryView = baseCategoryViewResult.getData();
            Assert.notNull(baseCategoryView, "分类视图对象为空");
            albumInfoIndex.setCategory1Id(baseCategoryView.getCategory1Id());
            albumInfoIndex.setCategory2Id(baseCategoryView.getCategory2Id());
        }, threadPoolExecutor);

        //  获取用户信息
        CompletableFuture<Void> userCompletableFuture = albumInfoCompletableFuture.thenAcceptAsync(albumInfo -> {
            Result<UserInfoVo> userInfoVoResult = userInfoFeignClient.getUserInfoVo(albumInfo.getUserId());
            Assert.notNull(userInfoVoResult, "用户信息结果集为空");
            UserInfoVo userInfoVo = userInfoVoResult.getData();
            Assert.notNull(userInfoVo, "用户信息为空");
            albumInfoIndex.setAnnouncerName(userInfoVo.getNickname());
        }, threadPoolExecutor);

        //  属性赋值：
        CompletableFuture<Void> attrCompletableFuture = CompletableFuture.runAsync(() -> {
            Result<List<AlbumAttributeValue>> listResult = albumInfoFeignClient.findAlbumAttributeValue(albumId);
            Assert.notNull(listResult, "属性值结果集为空");
            List<AlbumAttributeValue> albumAttributeValueList = listResult.getData();
            Assert.notNull(albumAttributeValueList, "属性值为空");
            //  albumAttributeValueList 循环遍历当前集合，获取到  attributeId  valueId 给 albumInfoIndex 中的属性赋值！
            List<AttributeValueIndex> attributeValueIndexList = albumAttributeValueList.stream().map(albumAttributeValue -> {
                //  创建对象
                AttributeValueIndex attributeValueIndex = new AttributeValueIndex();
                attributeValueIndex.setAttributeId(albumAttributeValue.getAttributeId());
                attributeValueIndex.setValueId(albumAttributeValue.getValueId());
                //  返回数据
                return attributeValueIndex;
            }).collect(Collectors.toList());
            //  赋值数据了.
            albumInfoIndex.setAttributeValueIndexList(attributeValueIndexList);
        }, threadPoolExecutor);

        //  初始化专辑的统计数据
        albumInfoIndex.setPlayStatNum(new Random().nextInt(100000));
        albumInfoIndex.setBuyStatNum(new Random().nextInt(1000000));
        albumInfoIndex.setSubscribeStatNum(new Random().nextInt(10000000));
        albumInfoIndex.setCommentStatNum(new Random().nextInt(100000000));

        //  多任务组合：
        CompletableFuture.allOf(
                albumInfoCompletableFuture,
                categoryCompletableFuture,
                userCompletableFuture,
                attrCompletableFuture
        ).join();
        log.info("上架专辑：{}", albumInfoIndex);
        //  将找个对象保存到es中！
        albumIndexRepository.save(albumInfoIndex);
        //  将数据保存到提词库！ 做的专辑标题;
        SuggestIndex suggestIndex = new SuggestIndex();
        suggestIndex.setId(UUID.randomUUID().toString().replaceAll("-",""));
        suggestIndex.setTitle(albumInfoIndex.getAlbumTitle());
        suggestIndex.setKeyword(new Completion(new String[]{albumInfoIndex.getAlbumTitle()}));
        suggestIndex.setKeywordPinyin(new Completion(new String[]{PinYinUtils.toHanyuPinyin(albumInfoIndex.getAlbumTitle())}));
        suggestIndex.setKeywordSequence(new Completion(new String[]{PinYinUtils.getFirstLetter(albumInfoIndex.getAlbumTitle())}));
        suggestIndexRepository.save(suggestIndex);

        //  做的专辑简介;
        SuggestIndex suggestIndexIntro = new SuggestIndex();
        suggestIndexIntro.setId(UUID.randomUUID().toString().replaceAll("-",""));
        suggestIndexIntro.setTitle(albumInfoIndex.getAlbumIntro());
        suggestIndexIntro.setKeyword(new Completion(new String[]{albumInfoIndex.getAlbumIntro()}));
        suggestIndexIntro.setKeywordPinyin(new Completion(new String[]{PinYinUtils.toHanyuPinyin(albumInfoIndex.getAlbumIntro())}));
        suggestIndexIntro.setKeywordSequence(new Completion(new String[]{PinYinUtils.getFirstLetter(albumInfoIndex.getAlbumIntro())}));
        suggestIndexRepository.save(suggestIndexIntro);

        //  作者;

    }

    @Override
    public List<AlbumInfoIndex> findRankingList(Long baseCategory1Id, String ranking) {
        //  hget key field;
        return (List<AlbumInfoIndex>) this.redisTemplate.opsForHash().get(RedisConstant.RANKING_KEY_PREFIX+baseCategory1Id, ranking);
    }

    @Override
    public void updateLatelyAlbumRanking() {
        //  1.  火器到所有的一级分类Id列表;
        Result<List<BaseCategory1>> baseCategory1Result = categoryFeignClient.findAllCategory1();
        Assert.notNull(baseCategory1Result, "一级分类结果集为空");
        List<BaseCategory1> baseCategory1List = baseCategory1Result.getData();
        //  循环遍历
        for (BaseCategory1 baseCategory1 : baseCategory1List) {
            //  五个维度
            String[] rankingDimensionArray = new String[]{"hotScore", "playStatNum", "subscribeStatNum", "buyStatNum", "commentStatNum"};
            for (String ranking : rankingDimensionArray) {
                //  执行dsl语句
                SearchResponse<AlbumInfoIndex> searchResponse = null;
                try {
                    searchResponse = elasticsearchClient.search(s -> s.index("albuminfo")
                                    .query(q -> q.term(t -> t.field("category1Id").value(baseCategory1.getId())))
                                    .sort(st -> st.field(f -> f.field(ranking).order(SortOrder.Desc)))
                            ,
                            AlbumInfoIndex.class);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                //  获取数据 Hash: hset key field value;
                List<AlbumInfoIndex> albumInfoIndexList = searchResponse.hits().hits().stream().map(Hit::source).collect(Collectors.toList());
                //  并放入缓存;
                this.redisTemplate.opsForHash().put(RedisConstant.RANKING_KEY_PREFIX+baseCategory1.getId(), ranking, albumInfoIndexList);
            }
        }
    }

    @Override
    public List<String> completeSuggest(String keyword) {
        //  用客户端生成dsl语句！
        SearchResponse<SuggestIndex> searchResponse = null;
        try {
            searchResponse = elasticsearchClient.search(r -> r.index("suggestinfo")
                            .suggest(s -> s.suggesters("ts-suggest-keyword",
                                            s1 -> s1.prefix(keyword).completion(c -> c.field("keyword")
                                                    .fuzzy(f -> f.fuzziness("auto"))
                                                    .skipDuplicates(true)))
                                    .suggesters("ts-suggest-keywordPinyin",
                                            s1 -> s1.prefix(keyword).completion(c -> c.field("keywordPinyin")
                                                    .fuzzy(f -> f.fuzziness("auto"))
                                                    .skipDuplicates(true)))
                                    .suggesters("ts-suggest-keywordSequence",
                                            s1 -> s1.prefix(keyword).completion(c -> c.field("keywordSequence")
                                                    .fuzzy(f -> f.fuzziness("auto"))
                                                    .skipDuplicates(true)))
                            )
                    ,
                    SuggestIndex.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        //  获取数据
        List<String> list = new ArrayList<>();
        list.addAll(this.getSuggestTitle(searchResponse, "ts-suggest-keyword"));
        list.addAll(this.getSuggestTitle(searchResponse, "ts-suggest-keywordPinyin"));
        list.addAll(this.getSuggestTitle(searchResponse, "ts-suggest-keywordSequence"));
        //  返回集合
        return list;
    }

    private Collection<String> getSuggestTitle(SearchResponse<SuggestIndex> searchResponse, String keyword) {
        List<Suggestion<SuggestIndex>> suggestionList = searchResponse.suggest().get(keyword);
        for (Suggestion<SuggestIndex> suggestIndexSuggestion : suggestionList) {
            //  获取数据
            List<String> list = suggestIndexSuggestion.completion().options().stream().map(suggestIndexCompletionSuggestOption -> {
                return suggestIndexCompletionSuggestOption.source().getTitle();
            }).collect(Collectors.toList());
            //  返回数据
            return list;
        }
        return null;
    }

    @Override
    public List<Map<String,Object>> channel(Long category1Id) {
        //  第一个要获取到分类对象； 第二个获取到专辑列表;
        Result<List<BaseCategory3>> baseCategory3ListResult = categoryFeignClient.findTopBaseCategory3(category1Id);
        Assert.notNull(baseCategory3ListResult, "分类结果集为空");
        List<BaseCategory3> baseCategory3List = baseCategory3ListResult.getData();
        Assert.notNull(baseCategory3List, "分类列表为空");
        //  将baseCategory3List 这个集合转换为map 集合 key=category3Id value = category3;
        Map<Long, BaseCategory3> baseCategory3Map = baseCategory3List.stream().collect(Collectors.toMap(BaseCategory3::getId, baseCategory3 -> baseCategory3));

//        Map<Long, BaseCategory3> collect = baseCategory3List.stream().collect(Collectors.toMap(baseCategory3 ->  baseCategory3.getId(), baseCategory3 -> baseCategory3));
        List<Long> category3IdList = baseCategory3List.stream().map(BaseCategory3::getId).collect(Collectors.toList());
        //  编写dsl 语句;
        List<FieldValue> category3IdFieldValueList = category3IdList.stream().map(category3Id -> FieldValue.of(category3Id)).collect(Collectors.toList());
        //  List<FieldValue> fieldValueList = baseCategory3List.stream().map(baseCategory3 -> FieldValue.of(baseCategory3.getId())).collect(Collectors.toList());

        //  .value(List<FieldValue>)
        SearchResponse<AlbumInfoIndex> searchResponse = null;
        try {
            searchResponse = elasticsearchClient.search(s -> s.index("albuminfo")
                            .query(q -> q.terms(t -> t.field("category3Id")
                                    .terms(tm -> tm.value(category3IdFieldValueList))))
                            .aggregations("aggCategory3Id", a -> a.terms(t -> t.field("category3Id"))
                                    .aggregations("aggTopSix", a2 -> a2.topHits(th -> th.size(6)
                                            .sort(st -> st.field(f -> f.field("buyStatNum").order(SortOrder.Desc)))))
                            )
                    ,
                    AlbumInfoIndex.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        //  从结果集中获取数据{获取三级分类对象：baseCategory3Map，专辑列表}
        Aggregate aggCategory3Id = searchResponse.aggregations().get("aggCategory3Id");
        List<Map<String, Object>> list = aggCategory3Id.lterms().buckets().array().stream().map(bucket -> {
            //  创建map 集合
            Map<String, Object> hashMap = new HashMap<>();
            //  获取到了三级分类Id
            long category3Id = bucket.key();
            //  获取专辑列表
            Aggregate aggTopSix = bucket.aggregations().get("aggTopSix");
            //  获取专辑列表
            List<AlbumInfoIndex> albumInfoIndexList = aggTopSix.topHits().hits().hits().stream().map(hit -> {
                //  将字符串转换为某个对象：
                AlbumInfoIndex albumInfoIndex = JSON.parseObject(hit.source().toString(), AlbumInfoIndex.class);
                //  返回数据
                return albumInfoIndex;
            }).collect(Collectors.toList());
            //  存储三级分类对象
            hashMap.put("baseCategory3", baseCategory3Map.get(category3Id));
            //  获取专辑列表;
            hashMap.put("list", albumInfoIndexList);

            //  返回集合数据
            return hashMap;
        }).collect(Collectors.toList());
        //  返回数据
        return list;
    }

    @Override
    public AlbumSearchResponseVo search(AlbumIndexQuery albumIndexQuery) {
        //  1.  先生成dsl语句！ 构建一个 SearchRequest 对象！
        SearchRequest searchRequest = this.queryBuildDsl(albumIndexQuery);
        //  2.  执行search方法
        SearchResponse<AlbumInfoIndex> searchResponse = null;
        try {
            searchResponse = elasticsearchClient.search(searchRequest, AlbumInfoIndex.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        //  3.  创建对象 AlbumSearchResponseVo
        //  3.1 将searchResponse对象中的数据赋值给AlbumSearchResponseVo
        AlbumSearchResponseVo albumSearchResponseVo = this.parseResultData(searchResponse);
        //  3.2 给albumSearchResponseVo属性赋值：
        //        private List<AlbumInfoIndexVo> list = new ArrayList<>();
        //        private Long total;//总记录数
        //        private Integer pageSize;//每页显示的内容
        //        private Integer pageNo;//当前页面
        //        private Long totalPages;
        //  总记录数
        albumSearchResponseVo.setTotal(searchResponse.hits().total().value());
        //  每页显示的条数
        albumSearchResponseVo.setPageSize(albumIndexQuery.getPageSize());
        //  当前页面
        albumSearchResponseVo.setPageNo(albumIndexQuery.getPageNo());
        //  总页数 10 3 4   9 3 3
        albumSearchResponseVo.setTotalPages((albumSearchResponseVo.getTotal() + albumSearchResponseVo.getPageSize() - 1) / albumSearchResponseVo.getPageSize());
        //  4.  返回数据
        return albumSearchResponseVo;
    }

    private SearchRequest queryBuildDsl(AlbumIndexQuery albumIndexQuery) {
        //  创建SearchRequest 对象
        SearchRequest.Builder searchRequestBuilder = new SearchRequest.Builder();
        searchRequestBuilder.index("albuminfo");
        //  创建一个query 对象;
        Query.Builder queryBuilder = new Query.Builder();
        //  创建 bool 对象
        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();

        //  1.  判断用户是否根据关键词检索
        if (!StringUtils.isEmpty(albumIndexQuery.getKeyword())) {
            boolQueryBuilder.should(s -> s.match(m -> m.field("albumTitle").query(albumIndexQuery.getKeyword())))
                    .should(s -> s.match(m -> m.field("albumIntro").query(albumIndexQuery.getKeyword())))
                    .should(s -> s.match(m -> m.field("announcerName").query(albumIndexQuery.getKeyword())));

            //  高亮
            searchRequestBuilder.highlight(h -> h.fields("albumTitle", hf -> hf.preTags("<span style=color:red>").postTags("</span>")));
        }
        //  2.  判断当前用户是否根据分类Id
        if (null != albumIndexQuery.getCategory3Id()) {
            boolQueryBuilder.filter(f -> f.term(t -> t.field("category3Id").value(albumIndexQuery.getCategory3Id())));
        }
        if (null != albumIndexQuery.getCategory2Id()) {
            boolQueryBuilder.filter(f -> f.term(t -> t.field("category2Id").value(albumIndexQuery.getCategory2Id())));
        }
        if (null != albumIndexQuery.getCategory1Id()) {
            boolQueryBuilder.filter(f -> f.term(t -> t.field("category1Id").value(albumIndexQuery.getCategory1Id())));
        }
        //  3. 根据属性值进行过滤 属性id:属性值id
        List<String> attributeList = albumIndexQuery.getAttributeList();
        //  判断集合是否为空
        if (!CollectionUtils.isEmpty(attributeList)) {
            //  循环遍历
            for (String attribute : attributeList) {
                //  attribute=属性id:属性值id
                String[] split = attribute.split(":");
                if (null != split && split.length == 2) {
                    boolQueryBuilder.filter(f -> f.nested(n -> n.path("attributeValueIndexList")
                            .query(q -> q.bool(b -> b.filter(bf -> bf.term(t -> t.field("attributeValueIndexList.attributeId").value(split[0])))
                                    .filter(bf -> bf.term(t -> t.field("attributeValueIndexList.valueId").value(split[1])))
                            ))));
                }
            }
        }
        //  分页，排序
        int from = (albumIndexQuery.getPageNo() - 1) * albumIndexQuery.getPageSize();
        searchRequestBuilder.from(from);
        searchRequestBuilder.size(albumIndexQuery.getPageSize());

        //  获取排序规则 综合排序[1:desc] 播放量[2:desc] 发布时间[3:desc]；asc:升序 desc:降序
        String order = albumIndexQuery.getOrder();
        if (!StringUtils.isEmpty(order)) {
            //  order=1:desc
            String[] split = order.split(":");
            if (null != split && split.length == 2) {
                //  声明一个排序字段
                String finalField = "";
                //  判断根据哪个字段进行排序
                switch (split[0]) {
                    case "1":
                        finalField = "hotScore";
                        break;
                    case "2":
                        finalField = "playStatNum";
                        break;
                    case "3":
                        finalField = "createTime";
                        break;
                }
                //  设置排序规则
                String finalField1 = finalField;
                searchRequestBuilder.sort(s -> s.field(f -> f.field(finalField1).order("asc".equals(split[1]) ? SortOrder.Asc : SortOrder.Desc)));
            }
        }
        BoolQuery boolQuery = boolQueryBuilder.build();
        queryBuilder.bool(boolQuery);
        Query query = queryBuilder.build();
        //  调用query方法
        searchRequestBuilder.query(query);
        //  调用build方法获取到对象
        SearchRequest searchRequest = searchRequestBuilder.build();
        //  打印dsl 语句
        System.out.println("dsl:\t" + searchRequest.toString());
        //  返回对象
        return searchRequest;
    }

    private AlbumSearchResponseVo parseResultData(SearchResponse<AlbumInfoIndex> searchResponse) {
        //  主要给 private List<AlbumInfoIndexVo> list = new ArrayList<>(); 属性赋值;
        AlbumSearchResponseVo albumSearchResponseVo = new AlbumSearchResponseVo();
        //  获取数据
        List<Hit<AlbumInfoIndex>> hits = searchResponse.hits().hits();
        List<AlbumInfoIndexVo> list = hits.stream().map(hit -> {
            //  创建对象
            AlbumInfoIndexVo albumInfoIndexVo = new AlbumInfoIndexVo();
            //  获取到source
            AlbumInfoIndex albumInfoIndex = hit.source();
            //  赋值：
            BeanUtils.copyProperties(albumInfoIndex, albumInfoIndexVo);
            //  判断当前用户是否根据关键词进行检索，应该获取到高亮字段.
            if (null != hit.highlight().get("albumTitle")) {
                //  获取高亮标题
                String albumTitle = hit.highlight().get("albumTitle").get(0);
                //  赋值
                albumInfoIndexVo.setAlbumTitle(albumTitle);
            }
            //  返回数据
            return albumInfoIndexVo;
        }).collect(Collectors.toList());
        //  赋值；
        albumSearchResponseVo.setList(list);
        //  返回数据
        return albumSearchResponseVo;
    }

    @Override
    public void lowerAlbum(Long albumId) {
        //  删除
        albumIndexRepository.deleteById(albumId);
    }
}
