package com.atguigu.tingshu.user.service.impl;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import com.atguigu.tingshu.album.client.TrackInfoFeignClient;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.rabbit.constant.MqConst;
import com.atguigu.tingshu.common.rabbit.service.RabbitService;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.model.user.*;
import com.atguigu.tingshu.user.client.UserInfoFeignClient;
import com.atguigu.tingshu.user.factory.StrategyFactory;
import com.atguigu.tingshu.user.mapper.*;
import com.atguigu.tingshu.user.service.UserInfoService;
import com.atguigu.tingshu.user.strategy.ItemTypeStrategy;
import com.atguigu.tingshu.vo.user.UserInfoVo;
import com.atguigu.tingshu.vo.user.UserPaidRecordVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import org.joda.time.LocalDateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements UserInfoService {

    @Autowired
    private UserInfoMapper userInfoMapper;

    //	启动的时候，自动从spring 容器中获取的对象； cn.binarywang.wx.miniapp.api
    //	需要重新定义当前对象，不需要使用启动时创建的，我需要重写;
    @Autowired
    private WxMaService wxMaService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RabbitService rabbitService;

    @Autowired
    private UserPaidAlbumMapper userPaidAlbumMapper;

    @Autowired
    private UserPaidTrackMapper userPaidTrackMapper;

    @Autowired
    private VipServiceConfigMapper vipServiceConfigMapper;

    @Autowired
    private TrackInfoFeignClient trackInfoFeignClient;

    @Autowired
    private UserInfoFeignClient userInfoFeignClient;

    @Autowired
    private UserVipServiceMapper userVipServiceMapper;

    @Autowired
    private StrategyFactory strategyFactory;


    @Override
    public void updateVipExpireStatus() {
        //  更新过期用户
        userInfoMapper.updateIsVip();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void savePaidRecord(UserPaidRecordVo userPaidRecordVo) {
        //  根据类型调用不同的接口实现.
        ItemTypeStrategy strategy = strategyFactory.writePaiRecode(userPaidRecordVo.getItemType());
        strategy.savePaidRecord(userPaidRecordVo);

        //  本质就是保存数据：user_paid_album user_paid_track user_vip_service
        // if (SystemConstant.ORDER_ITEM_TYPE_ALBUM.equals(userPaidRecordVo.getItemType())) {
            //  先查询是否有购买记录;
            //            Boolean result = isPaidAlbum(userPaidRecordVo.getItemIdList().get(0), userPaidRecordVo.getUserId());
            //            if (result) return;
            //            //  添加购买专辑信息;
            //            UserPaidAlbum userPaidAlbum = new UserPaidAlbum();
            //            userPaidAlbum.setUserId(userPaidRecordVo.getUserId());
            //            userPaidAlbum.setAlbumId(userPaidRecordVo.getItemIdList().get(0));
            //            userPaidAlbum.setOrderNo(userPaidRecordVo.getOrderNo());
            //            userPaidAlbumMapper.insert(userPaidAlbum);
        // } else if (SystemConstant.ORDER_ITEM_TYPE_TRACK.equals(userPaidRecordVo.getItemType())) {
            //  添加购买声音 : 如果只有小程序--单线程的。不会出现脏数据。如果有个平台; pc, h5;
            //  for (Long trackId : userPaidRecordVo.getItemIdList()) {
            //  通过用户Id，专辑Id 查询到已购买的集合; 判断已购买的集合中是否包含trackId; 如果包含，不能执行插入。使用continue; 可能需要退还对应的金额;
            //  否则，直接执行插入数据；
            //  }
            //            Long count = this.userPaidTrackMapper.selectCount(new LambdaQueryWrapper<UserPaidTrack>().eq(UserPaidTrack::getOrderNo, userPaidRecordVo.getOrderNo()).eq(UserPaidTrack::getUserId, userPaidRecordVo.getUserId()));
            //            if (count > 0) return;
            //            //  获取到当前订单明细;
            //            List<Long> trackIdList = userPaidRecordVo.getItemIdList();
            //            //  可以通过声音Id 查询声音对象；
            //            Result<TrackInfo> trackInfoResult = trackInfoFeignClient.getTrackInfoById(trackIdList.get(0));
            //            Assert.notNull(trackInfoResult, "查询购买声音结果为空");
            //            TrackInfo trackInfo = trackInfoResult.getData();
            //            Assert.notNull(trackInfo, "查询购买声音对象为空");
            //            for (Long trackId : trackIdList) {
            //                //  插入数据；
            //                UserPaidTrack userPaidTrack = new UserPaidTrack();
            //                userPaidTrack.setAlbumId(trackInfo.getAlbumId());
            //                userPaidTrack.setTrackId(trackId);
            //                userPaidTrack.setOrderNo(userPaidRecordVo.getOrderNo());
            //                userPaidTrack.setUserId(userPaidRecordVo.getUserId());
            //                userPaidTrackMapper.insert(userPaidTrack);
            //            }
        // } else {
            //  添加购买vip,续期 user_vip_service
            //  先获取到购买vip哪种配置 1,3,12 月；
        //            Long vipServiceConfigId = userPaidRecordVo.getItemIdList().get(0);
        //            VipServiceConfig vipServiceConfig = vipServiceConfigMapper.selectById(vipServiceConfigId);
        //            //  获取购买的月数
        //            Integer serviceMonth = vipServiceConfig.getServiceMonth();
        //            //  分情况：是否属于续期情况（当前这个用户属于vip也没有过期）
        //            UserInfo userInfo = this.userInfoMapper.selectById(userPaidRecordVo.getUserId());
        //            //  创建购买vip记录的对象
        //            UserVipService userVipService = new UserVipService();
        //            userVipService.setOrderNo(userPaidRecordVo.getOrderNo());
        //            userVipService.setUserId(userPaidRecordVo.getUserId());
        //            //  获取当前系统时间;
        //            Date currentTime = new Date();
        //            //  应该是没有续期的情况下;
        //            userVipService.setStartTime(currentTime);
        //            //  续期： start_time 开始时间; expire_time 过期时间 在原有的过期时间基础上，再加上购买的时长.
        //            if (1 == userInfo.getIsVip() && userInfo.getVipExpireTime().after(new Date())){
        //                //  续期 - 在原有的过期时间基础上，再加上购买的时长.
        //                currentTime = userInfo.getVipExpireTime();
        //            }
        //            //  当前系统时间+购买时间;
        //            Date expireTime = new LocalDateTime(currentTime).plusMonths(serviceMonth).toDate();
        //            userVipService.setExpireTime(expireTime);
        //            //  保存用户购买记录;
        //            userVipServiceMapper.insert(userVipService);
        //
        ////            int i = 1/0;
        //            //  判断是否属于vip 只关心 is_vip and exprie_time;
        //            userInfo.setIsVip(1);
        //            userInfo.setVipExpireTime(expireTime);
        //            //  更新数据
        //            this.userInfoMapper.updateById(userInfo);
        // }


    }

    @Override
    public List<Long> findUserPaidTrackList(Long albumId, Long userId) {
        //  select * from user_paid_track where user_id = 28 and album_id = 1429
        List<Long> trackIdList = userPaidTrackMapper.selectList(new LambdaQueryWrapper<UserPaidTrack>().eq(UserPaidTrack::getUserId, userId).eq(UserPaidTrack::getAlbumId, albumId)).stream().map(UserPaidTrack::getTrackId).collect(Collectors.toList());
        //  返回数据
        return trackIdList;
    }

    @Override
    public Boolean isPaidAlbum(Long albumId, Long userId) {
        //	albumId userId 在表中有且只有一条记录
        return userPaidAlbumMapper.selectOne(new LambdaQueryWrapper<UserPaidAlbum>().eq(UserPaidAlbum::getAlbumId, albumId).eq(UserPaidAlbum::getUserId, userId)) != null;
    }

    @Override
    public Map<Long, Integer> userIsPaidTrack(Long albumId, List<Long> trackIdList, Long userId) {
        //	map.put(trackId,1): 当前声音已购买，map.put(trackId,0): 当前声音未购买
        //	先判断当前这个用户是否购买过专辑; user_paid_album
        UserPaidAlbum userPaidAlbum = userPaidAlbumMapper.selectOne(new LambdaQueryWrapper<UserPaidAlbum>().eq(UserPaidAlbum::getAlbumId, albumId).eq(UserPaidAlbum::getUserId, userId));
        //	判断对象是否为空
        if (null != userPaidAlbum) {
            //	说这个用户购买过专辑,需要将需要购买的声音Id 设置为免费; trackId:0;
            //			HashMap<Long, Integer> map = new HashMap<>();
            //			for (Long trackId : trackIdList) {
            //				map.put(trackId,0);
            //			}
            //			return map;
            return trackIdList.stream().collect(Collectors.toMap(trackId -> trackId, trackId -> 1));
        }
        //	当前这个用户没有购买专辑, 查看是否购买过声音; user_paid_track
        //	查询当前这个用户，是否购买过当前专辑中需要付费的声音列表： select * from user_paid_track where user_id = 28 and album_id = 1429 and track_id in ( 48241,48249);
        LambdaQueryWrapper<UserPaidTrack> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserPaidTrack::getUserId, userId).eq(UserPaidTrack::getAlbumId, albumId).in(UserPaidTrack::getTrackId, trackIdList);
        List<Long> trackIdPaidList = userPaidTrackMapper.selectList(wrapper).stream().map(UserPaidTrack::getTrackId).collect(Collectors.toList());
        //	声明map 集合
        HashMap<Long, Integer> map = new HashMap<>();
        //	trackIdList -- 需要所有付费的声音Id;  userPaidTrackList 不需要付费的声音Id;
        for (Long trackId : trackIdList) {
            //	判断
            if (trackIdPaidList.contains(trackId)) {
                //	设置免费
                map.put(trackId, 1);
            } else {
                //	设置付费
                map.put(trackId, 0);
            }
        }
        //	返回map 集合
        return map;
    }

    @Override
    public void updateUser(UserInfoVo userInfoVo, Long userId) {
        //	更新用户信息
        UserInfo userInfo = new UserInfo();
        userInfo.setId(userId);
        userInfo.setNickname(userInfoVo.getNickname());
        userInfo.setAvatarUrl(userInfoVo.getAvatarUrl());
        this.userInfoMapper.updateById(userInfo);
    }

    @Override
    public UserInfoVo getUserInfo(Long userId) {
        //	创建对象
        UserInfoVo userInfoVo = new UserInfoVo();
        //	获取用户信息
        UserInfo userInfo = this.getById(userId);
        //	属性拷贝
        BeanUtils.copyProperties(userInfo, userInfoVo);
        return userInfoVo;
    }

    @Override
    public Map<String, Object> wxLogin(String code) {
        //	调用服务方法
        WxMaJscode2SessionResult wxMaJscode2SessionResult = null;
        try {
            wxMaJscode2SessionResult = wxMaService.jsCode2SessionInfo(code);
        } catch (WxErrorException e) {
            throw new RuntimeException(e);
        }
        //	获取openid;
        String openId = wxMaJscode2SessionResult.getOpenid();
        //	使用 openid 查询数据库，判断用户是否已经注册
        UserInfo userInfo = this.getOne(new LambdaQueryWrapper<UserInfo>().eq(UserInfo::getWxOpenId, openId));
        //	判断
        if (null == userInfo) {
            //	当前没有注册过;
            userInfo = new UserInfo();
            userInfo.setWxOpenId(openId);
            userInfo.setNickname("听友:" + System.currentTimeMillis());
            userInfo.setAvatarUrl("https://oss.aliyuncs.com/aliyun_id_photo_bucket/default_handsome.jpg");
            //	保存数据
            this.save(userInfo);
            //	注册就送钱! 1000块; openFeignClient; 发送消息; 发送的内容是由消费者决定的！
            rabbitService.sendMessage(MqConst.EXCHANGE_USER, MqConst.ROUTING_USER_REGISTER, userInfo.getId());

        }
        //	直接返回登录成功！
        String token = UUID.randomUUID().toString().replaceAll("-", "");
        //  从缓存中获取数据
        String userLoginKey = RedisConstant.USER_LOGIN_KEY_PREFIX + token;
        this.redisTemplate.opsForValue().set(userLoginKey, userInfo, RedisConstant.USER_LOGIN_KEY_TIMEOUT, TimeUnit.SECONDS);
        //	创建Map 集合
        Map<String, Object> map = new HashMap<>();
        map.put("token", token);
        //	返数据
        return map;
    }
}
