package com.atguigu.tingshu.album.service.impl;

import com.atguigu.tingshu.album.config.VodConstantProperties;
import com.atguigu.tingshu.album.mapper.AlbumInfoMapper;
import com.atguigu.tingshu.album.mapper.TrackInfoMapper;
import com.atguigu.tingshu.album.mapper.TrackStatMapper;
import com.atguigu.tingshu.album.service.TrackInfoService;
import com.atguigu.tingshu.album.service.VodService;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.UploadFileUtil;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.model.album.TrackStat;
import com.atguigu.tingshu.query.album.TrackInfoQuery;
import com.atguigu.tingshu.user.client.UserInfoFeignClient;
import com.atguigu.tingshu.vo.album.*;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qcloud.vod.VodUploadClient;
import com.qcloud.vod.model.VodUploadRequest;
import com.qcloud.vod.model.VodUploadResponse;
import com.tencentcloudapi.vod.v20180717.models.MediaInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class TrackInfoServiceImpl extends ServiceImpl<TrackInfoMapper, TrackInfo> implements TrackInfoService {

    @Autowired
    private TrackInfoMapper trackInfoMapper;

    @Autowired
    private VodConstantProperties vodConstantProperties;

    @Autowired
    private AlbumInfoMapper albumInfoMapper;

    @Autowired
    private VodService vodService;

    @Autowired
    private TrackStatMapper trackStatMapper;

    @Autowired
    private UserInfoFeignClient userInfoFeignClient;

    @Override
    public List<TrackInfo> findPaidTrackInfoList(Long trackId, Integer trackCount) {
        //  创建声音集合列表：
        List<TrackInfo> paidTrackInfoList = new ArrayList<>();
        //  1. 先获取到声音对象; select * from track_info where id = 48241;
        TrackInfo trackInfo = this.getById(trackId);
        //  2.  判断用户购买的集数;
        if (trackCount > 0) {
            //  3.  找到用户已购买的集合列表：
            Result<List<Long>> trackIdListResult = userInfoFeignClient.findUserPaidTrackList(trackInfo.getAlbumId());
            Assert.notNull(trackIdListResult, "用户购买声音Id列表为空");
            List<Long> paidTrackIdList = trackIdListResult.getData();
            //  4.  查询数据; select * from track_info where album_id = 1429 and order_num > 6 and id not in (48244,48245,,48246) limit 10;
            //  构建查询条件
            LambdaQueryWrapper<TrackInfo> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(TrackInfo::getAlbumId, trackInfo.getAlbumId()).gt(TrackInfo::getOrderNum, trackInfo.getOrderNum());
            if (paidTrackIdList.size() > 0) {
                wrapper.notIn(TrackInfo::getId, paidTrackIdList);
            }
            //  限制查询数量：
            wrapper.last(" limit " + trackCount);
            //  接收数据
            paidTrackInfoList = trackInfoMapper.selectList(wrapper);
        } else {
            paidTrackInfoList.add(trackInfo);
        }
        //  返回集合列表
        return paidTrackInfoList;
    }

    @Override
    public List<Map<String, Object>> findUserTrackPaidList(Long trackId) {
        //  1.  获取到所有的声音Id; select * from track_info where album_id = 1429 and order_num > 6;
        TrackInfo trackInfo = trackInfoMapper.selectById(trackId);
        LambdaQueryWrapper<TrackInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TrackInfo::getAlbumId, trackInfo.getAlbumId()).gt(TrackInfo::getOrderNum, trackInfo.getOrderNum());
        List<Long> allTrackIdList = trackInfoMapper.selectList(wrapper).stream().map(TrackInfo::getId).collect(Collectors.toList());

        //  2.  获取到用户购买的声音Id 列表; select * from user_paid_track where user_id = 28 and album_id = 1429
        Result<List<Long>> trackIdListResult = userInfoFeignClient.findUserPaidTrackList(trackInfo.getAlbumId());
        //  判断集合
        Assert.notNull(trackIdListResult, "用户购买声音Id列表为空");
        List<Long> trackPaidIdList = trackIdListResult.getData();
        //  3.  获取真正需要购买的声音集合列表
        List<Long> list = allTrackIdList.stream().filter(tId -> !trackPaidIdList.contains(tId)).collect(Collectors.toList());
        //  根据专辑Id获取专辑对象，便于获取单条声音的价格;
        AlbumInfo albumInfo = albumInfoMapper.selectById(trackInfo.getAlbumId());

        //  声明一个集合列表;
        List<Map<String, Object>> listMap = new ArrayList<>();
        //  4.  构建map集合返回数据；
        if (list.size() >= 0) {
            //  创建mpa 集合
            Map<String, Object> hashMap = new HashMap<>();
            hashMap.put("name", "本集");
            //  本集的钱=当前专辑的价格;
            hashMap.put("price", albumInfo.getPrice());
            //  用户要购买的集数;
            hashMap.put("trackCount", 0);
            listMap.add(hashMap);
        }
        //  后10集以内的数据;   8集-本集，后集;   18-本集，后10集, 后18集
        if (list.size() > 0 && list.size() <= 10) {
            //  创建mpa 集合
            Map<String, Object> hashMap = new HashMap<>();
            int count = list.size();
            hashMap.put("name", "后" + count + "集");
            //  本集的钱=当前专辑的价格;
            hashMap.put("price", albumInfo.getPrice().multiply(new BigDecimal(String.valueOf(count))));
            //  用户要购买的集数;
            hashMap.put("trackCount", count);
            listMap.add(hashMap);
        }
        //  判断
        if (list.size() > 10) {
            //  创建mpa 集合
            Map<String, Object> hashMap = new HashMap<>();
            hashMap.put("name", "后10集");
            //  本集的钱=当前专辑的价格;
            hashMap.put("price", albumInfo.getPrice().multiply(new BigDecimal(String.valueOf("10"))));
            //  用户要购买的集数;
            hashMap.put("trackCount", 10);
            listMap.add(hashMap);
        }
        //  18
        if (list.size() > 10 && list.size() <= 20) {
            //  创建mpa 集合
            Map<String, Object> hashMap = new HashMap<>();
            int count = list.size();
            hashMap.put("name", "后" + count + "集");
            //  本集的钱=当前专辑的价格;
            hashMap.put("price", albumInfo.getPrice().multiply(new BigDecimal(String.valueOf(count))));
            //  用户要购买的集数;
            hashMap.put("trackCount", count);
            listMap.add(hashMap);
        }
        //  判断
        if (list.size() > 20) {
            //  创建mpa 集合
            Map<String, Object> hashMap = new HashMap<>();
            hashMap.put("name", "后20集");
            //  本集的钱=当前专辑的价格;
            hashMap.put("price", albumInfo.getPrice().multiply(new BigDecimal(String.valueOf("20"))));
            //  用户要购买的集数;
            hashMap.put("trackCount", 20);
            listMap.add(hashMap);
        }

        //  18
        if (list.size() > 20 && list.size() <= 30) {
            //  创建mpa 集合
            Map<String, Object> hashMap = new HashMap<>();
            int count = list.size();
            hashMap.put("name", "后" + count + "集");
            //  本集的钱=当前专辑的价格;
            hashMap.put("price", albumInfo.getPrice().multiply(new BigDecimal(String.valueOf(count))));
            //  用户要购买的集数;
            hashMap.put("trackCount", count);
            listMap.add(hashMap);
        }
        //  判断
        if (list.size() > 30) {
            //  创建mpa 集合
            Map<String, Object> hashMap = new HashMap<>();
            hashMap.put("name", "后30集");
            //  本集的钱=当前专辑的价格;
            hashMap.put("price", albumInfo.getPrice().multiply(new BigDecimal(String.valueOf("30"))));
            //  用户要购买的集数;
            hashMap.put("trackCount", 30);
            listMap.add(hashMap);
        }

        //  返回数据
        return listMap;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTrackStat(TrackStatMqVo trackStatMqVo) {
        //  更新声音的播放量
        this.trackInfoMapper.updateTrackStat(trackStatMqVo.getTrackId(), trackStatMqVo.getCount(), trackStatMqVo.getStatType());

        //  更新对应专辑的播放量;
        if (trackStatMqVo.getStatType().equals(SystemConstant.TRACK_STAT_PLAY)) {
            //  更新专辑
            this.albumInfoMapper.updateAlbumStat(trackStatMqVo.getAlbumId(), trackStatMqVo.getCount(), SystemConstant.ALBUM_STAT_PLAY);
        }
    }

    @Override
    public IPage<AlbumTrackListVo> getAlbumTrackPage(Page<AlbumTrackListVo> albumTrackListVoPage, Long albumId, Long userId) {
        //  先获取当前专辑对应声音列表！
        IPage<AlbumTrackListVo> pageInfo = trackInfoMapper.selectAlbumTrackPage(albumTrackListVoPage, albumId);
        //  主要设置是否需要付费字段 isShowPaidMark;
        AlbumInfo albumInfo = this.albumInfoMapper.selectById(albumId);
        //  判断当前用户是否登录
        if (null == userId) {
            //  说明未登录！需要设置当前试听的声音免费 isShowPaidMark = false，其他声音收费 isShowPaidMark = true;
            if (!albumInfo.getPayType().equals(SystemConstant.ALBUM_PAY_TYPE_FREE)) {
                //  先找到免费试听的集数; 先找到专辑对象;
                Integer tracksForFree = albumInfo.getTracksForFree();
                //  select * from track_info where order_num > 5 and album_id = 1429;
                pageInfo.getRecords().stream().filter(albumTrackListVo -> albumTrackListVo.getOrderNum() > tracksForFree).forEach(albumTrackListVo -> albumTrackListVo.setIsShowPaidMark(true));
            }
            //  返回数据
            return pageInfo;
        } else {
            //  获取到当前用户对象
            Result<UserInfoVo> userInfoVoResult = this.userInfoFeignClient.getUserInfoVo(userId);
            //  获取数据
            UserInfoVo userInfoVo = userInfoVoResult.getData();
            //  声明一个字段;
            boolean isNeedPaidMark = false;
            //  说明用户登录; 判断当前专辑的类型;
            if (SystemConstant.ALBUM_PAY_TYPE_VIPFREE.equals(albumInfo.getPayType())) {
                //  判断当前专辑是vip免费; 判断当前用户是否是vip; is_vip字段;
                if ((1 == userInfoVo.getIsVip() && userInfoVo.getVipExpireTime().before(new Date())) || userInfoVo.getIsVip() == 0) {
                    isNeedPaidMark = true;
                }
            } else if (SystemConstant.ALBUM_PAY_TYPE_REQUIRE.equals(albumInfo.getPayType())) {
                //  当前专辑是付费类型
                isNeedPaidMark = true;
            }
            //  统一处理付费情况：
            if (isNeedPaidMark) {
                //  除去用户购买的专辑或购买的声音之外{user_paid_album，user_paid_track}，要付费！
                //  获取需要付费的声音Id 列表;
                List<AlbumTrackListVo> trackListVoList = pageInfo.getRecords().stream().filter(albumTrackListVo -> albumTrackListVo.getOrderNum() > albumInfo.getTracksForFree()).collect(Collectors.toList());
                List<Long> trackIdNeedPaidList = trackListVoList.stream().map(AlbumTrackListVo::getTrackId).collect(Collectors.toList());
                //  远程调用
                Result<Map<Long, Integer>> mapResult = userInfoFeignClient.userIsPaidTrack(albumId, trackIdNeedPaidList);
                //  获取数据
                Assert.notNull(mapResult, "付费声音结果集为空");
                //  map.put(trackId,1): 当前声音已购买，map.put(trackId,0): 当前声音未购买
                Map<Long, Integer> map = mapResult.getData();
                //  循环遍历
                for (AlbumTrackListVo albumTrackListVo : trackListVoList) {
                    //  设置当前声音是否需要付费;
                    //                    if (map.get(albumTrackListVo.getTrackId()) == 1) {
                    //                        albumTrackListVo.setIsShowPaidMark(false);
                    //                    } else {
                    //                        albumTrackListVo.setIsShowPaidMark(true);
                    //                    }
                    //  代码优化：
                    albumTrackListVo.setIsShowPaidMark(map.get(albumTrackListVo.getTrackId()) == 1 ? false : true);
                }
            }
        }
        //  返回数据
        return pageInfo;
    }

    @Override
    public void updateTrackInfo(Long trackId, TrackInfoVo trackInfoVo) {
        //  区分是否修改了当前的音频文件!
        //  根据声音Id 获取到 声音对象
        TrackInfo trackInfo = this.getById(trackId);
        //  原生声音对象; trackInfo.getMediaFileId();
        String oldMediaFileId = trackInfo.getMediaFileId();
        //  属性拷贝;
        BeanUtils.copyProperties(trackInfoVo, trackInfo);
        //  最新的文件Id; trackInfoVo.getMediaFileId();
        if (!oldMediaFileId.equals(trackInfoVo.getMediaFileId())) {
            //  说明当前音频文件被修改了！ 重新计算当前声音的大小，类型，url，时长;
            TrackMediaInfoVo mediaInfo = vodService.getMediaInfo(trackInfoVo.getMediaFileId());
            //  赋值最新数据;
            trackInfo.setMediaUrl(mediaInfo.getMediaUrl());
            trackInfo.setMediaDuration(mediaInfo.getDuration());
            trackInfo.setMediaSize(mediaInfo.getSize());
            trackInfo.setMediaType(mediaInfo.getType());
            //  云点播中的数据就要删除.
            vodService.removeMedia(oldMediaFileId);
        }
        //  更新数据
        this.updateById(trackInfo);


    }

    @Override
    public TrackInfo getTrackInfo(Long trackId) {
        return trackInfoMapper.selectById(trackId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeTrackInfoById(Long trackId) {
        //  先获取到专辑对象;
        TrackInfo trackInfo = trackInfoMapper.selectById(trackId);
        //  track_info is_deleted = 1;
        trackInfoMapper.deleteById(trackId);
        //  track_stat is_deleted = 1;
        trackStatMapper.delete(new LambdaQueryWrapper<TrackStat>().eq(TrackStat::getTrackId, trackId));
        //  album_info.include_track_count-1;
        AlbumInfo albumInfo = this.albumInfoMapper.selectById(trackInfo.getAlbumId());
        //  修改包含声音总数
        albumInfo.setIncludeTrackCount(albumInfo.getIncludeTrackCount() - 1);
        //  更新专辑最新数据
        this.albumInfoMapper.updateById(albumInfo);
        //  声音对应的order_num 应该-1;
        //  update track_info set order_num = order_num - 1 where order_num > 51 and album_id = 1 and is_deleted = 0;
        trackInfoMapper.updateOrderNum(trackInfo.getOrderNum(), albumInfo.getId());
        //  删除云点播中的数据:
        vodService.removeMedia(trackInfo.getMediaFileId());

    }

    @Override
    public IPage<TrackListVo> getUserTrackPage(Page<TrackListVo> page, TrackInfoQuery trackInfoQuery) {
        //  调用mapper 层方法
        return trackInfoMapper.selectUserTrackPage(page, trackInfoQuery);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveTrackInfo(TrackInfoVo trackInfoVo, Long userId) {
        //	track_info
        TrackInfo trackInfo = new TrackInfo();
        //	属性拷贝
        BeanUtils.copyProperties(trackInfoVo, trackInfo);
        //	用户Id
        trackInfo.setUserId(userId);
        //	order_num 声音的序号
        //	根据专辑Id获取到专辑对象
        AlbumInfo albumInfo = albumInfoMapper.selectById(trackInfoVo.getAlbumId());
        trackInfo.setOrderNum(albumInfo.getIncludeTrackCount() + 1);
        //	赋值流媒体相关字段数据 media_duration media_file_id media_size media_type; 可以调用云点播的api获取！
        TrackMediaInfoVo mediaInfo = vodService.getMediaInfo(trackInfoVo.getMediaFileId());
        trackInfo.setMediaDuration(mediaInfo.getDuration());
        trackInfo.setMediaUrl(mediaInfo.getMediaUrl());
        trackInfo.setMediaSize(mediaInfo.getSize());
        trackInfo.setMediaType(mediaInfo.getType());
        //	保存声音
        trackInfoMapper.insert(trackInfo);

        //  声音统计初始化 track_stat;
        this.saveTrackStat(trackInfo.getId(), SystemConstant.TRACK_STAT_PLAY);
        this.saveTrackStat(trackInfo.getId(), SystemConstant.TRACK_STAT_COLLECT);
        this.saveTrackStat(trackInfo.getId(), SystemConstant.TRACK_STAT_PRAISE);
        this.saveTrackStat(trackInfo.getId(), SystemConstant.TRACK_STAT_COMMENT);

        //  更新专辑对应的声音总数;
        albumInfo.setIncludeTrackCount(albumInfo.getIncludeTrackCount() + 1);
        this.albumInfoMapper.updateById(albumInfo);

    }

    public void saveTrackStat(Long trackId, String statPlay) {
        //  创建声音统计对象
        TrackStat trackStat = new TrackStat();
        //  赋值：
        trackStat.setTrackId(trackId);
        trackStat.setStatType(statPlay);
        trackStat.setStatNum(new Random().nextInt(1000));
        this.trackStatMapper.insert(trackStat);
    }

    @Override
    public Map<String, Object> uploadTrack(MultipartFile file) {
        //	创建一个上传客户端对象
        VodUploadClient client = new VodUploadClient(vodConstantProperties.getSecretId(), vodConstantProperties.getSecretKey());
        //	构建上传路径:
        VodUploadRequest request = new VodUploadRequest();
        String tempPath = UploadFileUtil.uploadTempPath(vodConstantProperties.getTempPath(), file);
        request.setMediaFilePath(tempPath);
        //	调用上传方法
        try {
            VodUploadResponse response = client.upload(vodConstantProperties.getRegion(), request);
            log.info("Upload FileId = {}", response.getFileId());
            //	设置一个map；存储流媒体文件Id 以及 url路径
            Map<String, Object> map = new HashMap<>();
            map.put("mediaFileId", response.getFileId());
            map.put("mediaUrl", response.getMediaUrl());
            //	返回数据
            return map;
        } catch (Exception e) {
            // 业务方进行异常处理
            log.error("Upload Err", e);
        }
        //	默认返回
        return new HashMap<>();
    }
}
