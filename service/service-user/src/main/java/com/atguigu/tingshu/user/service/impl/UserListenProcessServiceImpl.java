package com.atguigu.tingshu.user.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.rabbit.constant.MqConst;
import com.atguigu.tingshu.common.rabbit.service.RabbitService;
import com.atguigu.tingshu.common.util.MongoUtil;
import com.atguigu.tingshu.model.user.UserListenProcess;
import com.atguigu.tingshu.user.service.UserListenProcessService;
import com.atguigu.tingshu.vo.album.TrackStatMqVo;
import com.atguigu.tingshu.vo.user.UserListenProcessVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@SuppressWarnings({"all"})
public class UserListenProcessServiceImpl implements UserListenProcessService {

	@Autowired
	private MongoTemplate mongoTemplate;

	@Autowired
	private RedisTemplate redisTemplate;

	@Autowired
	private RabbitService rabbitService;

	@Override
	public Map<String, Object> getLatelyTrack(Long userId) {
		//	声明一个map 集合
		Map<String, Object> result = new HashMap<>();
		//	获取声音列表
		//	Query query = Query.query(Criteria.where("userId").is(userId)).with(Sort.by(Sort.Direction.DESC, "updateTime"));
		Query query = new Query().with(Sort.by(Sort.Direction.DESC, "updateTime"));
		UserListenProcess userListenProcess = this.mongoTemplate.findOne(query, UserListenProcess.class, MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_LISTEN_PROCESS, userId));
		//	判断
		if (null != userListenProcess){
			//	封装数据
			result.put("albumId",userListenProcess.getAlbumId());
			result.put("trackId",userListenProcess.getTrackId());
			//	返回数据
			return result;
		}
		//	返回数据
		return result;
	}

	@Override
	public void updateListenProcess(UserListenProcessVo userListenProcessVo, Long userId) {
		//	先判断用户是否播放过当前声音;
		Query query = new Query();
		query.addCriteria(Criteria.where("userId").is(userId).and("trackId").is(userListenProcessVo.getTrackId()));
		//	获取集合名称：
		String collectionName = MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_LISTEN_PROCESS, userId);
		UserListenProcess userListenProcess = mongoTemplate.findOne(query, UserListenProcess.class, collectionName);
		//	判断：
		if (null == userListenProcess){
			//	第一次播放该声音.
			userListenProcess = new UserListenProcess();
			userListenProcess.setUserId(userId);
			userListenProcess.setAlbumId(userListenProcessVo.getAlbumId());
			userListenProcess.setTrackId(userListenProcessVo.getTrackId());
			userListenProcess.setBreakSecond(userListenProcessVo.getBreakSecond());
			userListenProcess.setCreateTime(new Date());
			userListenProcess.setUpdateTime(new Date());
			//	保存数据;
			//	this.mongoTemplate.save(userListenProcess,collectionName);
		} else {
			//	说明该声音播放过,需要更新播放进度;
			userListenProcess.setBreakSecond(userListenProcessVo.getBreakSecond());
			userListenProcess.setUpdateTime(new Date());
		}
		//	更新数据;
		this.mongoTemplate.save(userListenProcess,collectionName);
		//	控制，当前同一个用户，在规定时间内(24h)，不能重复累加播放次数！ redis --- setnx
		String key = "user:"+userId+":track:"+userListenProcessVo.getTrackId();
		Boolean result = this.redisTemplate.opsForValue().setIfAbsent(key, 1, 24, TimeUnit.HOURS);
		if (result){
			//	设置成功，可以累加播放次数track_stat！ 间接要更新专辑的播放;album_stat
			TrackStatMqVo trackStatMqVo = new TrackStatMqVo();
			trackStatMqVo.setStatType(SystemConstant.TRACK_STAT_PLAY);
			//	业务编号，防止消息重复消费!
			trackStatMqVo.setBusinessNo(UUID.randomUUID().toString().replaceAll("-",""));
			trackStatMqVo.setTrackId(userListenProcessVo.getTrackId());
			trackStatMqVo.setAlbumId(userListenProcessVo.getAlbumId());
			trackStatMqVo.setCount(1);
			//	异步发送消息，更新专辑-声音播放量
			rabbitService.sendMessage(MqConst.EXCHANGE_TRACK, MqConst.ROUTING_TRACK_STAT_UPDATE, JSON.toJSONString(trackStatMqVo));
		}
	}

	@Override
	public BigDecimal getTrackBreakSecond(Long trackId, Long userId) {
		//	用户Id，声音Id获取播放进度
		Query query = new Query();
		query.addCriteria(Criteria.where("userId").is(userId).and("trackId").is(trackId));
		//	获取集合名称：
		String collectionName = MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_LISTEN_PROCESS, userId);
		UserListenProcess userListenProcess = mongoTemplate.findOne(query, UserListenProcess.class, collectionName);
		//	判断
		if (null != userListenProcess){
			//	返回数据
			return userListenProcess.getBreakSecond();
		}
		//	默认返回
		return new BigDecimal("0");
	}
}
