package com.atguigu.tingshu.album.service.impl;

import com.atguigu.tingshu.album.mapper.AlbumAttributeValueMapper;
import com.atguigu.tingshu.album.mapper.AlbumInfoMapper;
import com.atguigu.tingshu.album.mapper.AlbumStatMapper;
import com.atguigu.tingshu.album.service.AlbumInfoService;
import com.atguigu.tingshu.common.cache.TsCache;
import com.atguigu.tingshu.common.constant.RedisConstant;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.rabbit.constant.MqConst;
import com.atguigu.tingshu.common.rabbit.service.RabbitService;
import com.atguigu.tingshu.model.album.AlbumAttributeValue;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.AlbumStat;
import com.atguigu.tingshu.query.album.AlbumInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumAttributeValueVo;
import com.atguigu.tingshu.vo.album.AlbumInfoVo;
import com.atguigu.tingshu.vo.album.AlbumListVo;
import com.atguigu.tingshu.vo.album.AlbumStatVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.weaver.ast.Var;
import org.jetbrains.annotations.Nullable;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class AlbumInfoServiceImpl extends ServiceImpl<AlbumInfoMapper, AlbumInfo> implements AlbumInfoService {

    @Autowired
    private AlbumInfoMapper albumInfoMapper;

    @Autowired
    private AlbumAttributeValueMapper albumAttributeValueMapper;

    @Autowired
    private AlbumStatMapper albumStatMapper;

    @Autowired
    private RabbitService rabbitService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;


    @Override
    @TsCache(prefix = "stat:info:")
    public AlbumStatVo getAlbumStatVo(Long albumId) {
        return albumInfoMapper.selectAlbumStat(albumId);
    }

    @Override
    public List<AlbumAttributeValue> getAlbumAttributeValueList(Long albumId) {
        //  查询数据
        return albumAttributeValueMapper.selectList(new LambdaQueryWrapper<AlbumAttributeValue>().eq(AlbumAttributeValue::getAlbumId, albumId));
    }

    @Override
    public List<AlbumInfo> findUserAllAlbumList(Long userId) {
        //  构建分页对象
        //  Page<AlbumInfo> albumInfoPage = new Page<>(1,30);
        //  Page<AlbumInfo> infoPage = albumInfoMapper.selectPage(albumInfoPage, new LambdaQueryWrapper<AlbumInfo>().eq(AlbumInfo::getUserId, userId).orderByDesc(AlbumInfo::getId));
        //  return infoPage.getRecords();
        //  select * from album_info where user_id = 1 and is_deleted = 0 order by id desc limit 30 ;
        LambdaQueryWrapper<AlbumInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AlbumInfo::getUserId, userId);
        wrapper.orderByDesc(AlbumInfo::getId).last(" limit 30 ");
        return albumInfoMapper.selectList(wrapper);
    }

    @Override
    public void updateAlbumInfo(Long albumId, AlbumInfoVo albumInfoVo) {
        //  根据专辑Id修改专辑数据;album_info
        //  属性拷贝：
        AlbumInfo albumInfo = new AlbumInfo();
        BeanUtils.copyProperties(albumInfoVo, albumInfo);
        albumInfo.setId(albumId);
        this.albumInfoMapper.updateById(albumInfo);
        //  获取属性与属性值的修改内容数据 album_attribute_value
        List<AlbumAttributeValueVo> albumAttributeValueVoList = albumInfoVo.getAlbumAttributeValueVoList();
        //  将原有数据删除，再新增!
        albumAttributeValueMapper.delete(new LambdaQueryWrapper<AlbumAttributeValue>().eq(AlbumAttributeValue::getAlbumId, albumId));
        if (!CollectionUtils.isEmpty(albumAttributeValueVoList)){
            //  循环遍历保存数据
            for (AlbumAttributeValueVo albumAttributeValueVo : albumAttributeValueVoList) {
                //  创建对象
                AlbumAttributeValue albumAttributeValue = new AlbumAttributeValue();
                //  属性拷贝：
                BeanUtils.copyProperties(albumAttributeValueVo, albumAttributeValue);
                //  给专辑属性Id 赋值 @TableId(type = IdType.AUTO) 表示能够获取到主键自增值！
                albumAttributeValue.setAlbumId(albumInfo.getId());
                albumAttributeValueMapper.insert(albumAttributeValue);
            }
        }

        //  判断当前专辑是否是上架下架
        if ("1".equals(albumInfo.getIsOpen())){
            //  上架
            rabbitService.sendMessage(MqConst.EXCHANGE_ALBUM,MqConst.ROUTING_ALBUM_UPPER,albumInfo.getId());
            //  获取布隆过滤器
            RBloomFilter<Object> bloomFilter = this.redissonClient.getBloomFilter(RedisConstant.ALBUM_BLOOM_FILTER);
            bloomFilter.add(albumInfo.getId());
        } else {
            //  下架
            rabbitService.sendMessage(MqConst.EXCHANGE_ALBUM,MqConst.ROUTING_ALBUM_LOWER,albumInfo.getId());
        }
    }

    @Override
    @TsCache(prefix = RedisConstant.ALBUM_INFO_PREFIX)
    public AlbumInfo getAlbumInfo(Long albumId) {
        //  先从缓存中获取；String Hash List Set ZSet;
        //        String key = RedisConstant.ALBUM_INFO_PREFIX + albumId;
        //        try {
        //            //  根据key来获取缓存数据;
        //            AlbumInfo albumInfo = (AlbumInfo) this.redisTemplate.opsForValue().get(key);
        //            //  判断
        //            if (null == albumInfo){
        //                //  缓存中没有数据,查询数据库，可能会造成缓存击穿，因此在此处加锁！
        //                String lockKey = RedisConstant.ALBUM_LOCK_SUFFIX + albumId;
        //                //  获取锁对象
        //                RLock lock = redissonClient.getLock(lockKey);
        //                //  上锁
        //                lock.lock();
        //                try {
        //                    //  为了充分利用缓存，查询数据库之前应该再查询一次缓存记录!
        //                    albumInfo = (AlbumInfo) this.redisTemplate.opsForValue().get(key);
        //                    if (null != albumInfo){
        //                        System.out.println("缓存有数据");
        //                        //  直接返回数据
        //                        return albumInfo;
        //                    }
        //                    //  调用查询数据库方法
        //                    albumInfo = getAlbumInfoDB(albumId);
        //                    System.out.println("已查询数据库");
        //                    //  从数据库中获取的数据一定有数据么? 不一定！
        //                    if (null == albumInfo){
        //                        //  先存储一个空对象;
        //                        this.redisTemplate.opsForValue().set(key,new AlbumInfo(),RedisConstant.ALBUM_TEMPORARY_TIMEOUT, TimeUnit.SECONDS);
        //                        //  返回数据
        //                        return new AlbumInfo();
        //                    }
        //                    //  数据库有数据，直接放入缓存
        //                    this.redisTemplate.opsForValue().set(key,albumInfo,RedisConstant.ALBUM_TIMEOUT, TimeUnit.DAYS);
        //                    //  返回数据
        //                    return albumInfo;
        //                } catch (Exception e) {
        //                    throw new RuntimeException(e);
        //                } finally {
        //                    //  解锁
        //                    lock.unlock();
        //                }
        //            } else {
        //                System.out.println("缓存有数据-----");
        //                //  返回缓存数据
        //                return albumInfo;
        //            }
        //        } catch (RuntimeException e) {
        //            log.error(e.getMessage());
        //            //  调用发送短信与邮件方法 通知管理员！
        //        }
        //  兜底：
        return getAlbumInfoDB(albumId);
    }

    @Nullable
    private AlbumInfo getAlbumInfoDB(Long albumId) {
        //  先获取到专辑对象
        AlbumInfo albumInfo = this.getById(albumId);
        //  获取属性与属性值集合数据
        if (null != albumInfo){
            albumInfo.setAlbumAttributeValueVoList(albumAttributeValueMapper.selectList(new LambdaQueryWrapper<AlbumAttributeValue>().eq(AlbumAttributeValue::getAlbumId, albumId)));
        }
        return albumInfo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeAlbumInfoById(Long albumId) {
        //  根据专辑Id删除专辑数据; 逻辑删除 is_deleted = 1;
        this.albumInfoMapper.deleteById(albumId);
        //  album_attribute_value 删除条件：album_id = ?;
        albumAttributeValueMapper.delete(new LambdaQueryWrapper<AlbumAttributeValue>().eq(AlbumAttributeValue::getAlbumId, albumId));
        //  album_stat:
        albumStatMapper.delete(new LambdaQueryWrapper<AlbumStat>().eq(AlbumStat::getAlbumId, albumId));
    }

    @Override
    public IPage<AlbumListVo> findUserAlbumPage(Page<AlbumListVo> albumListVoPage, AlbumInfoQuery albumInfoQuery) {
        return albumInfoMapper.selectUserAlbumPage(albumListVoPage,albumInfoQuery);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveAlbumInfo(AlbumInfoVo albumInfoVo, Long userId) {
        //	1.	album_info
        AlbumInfo albumInfo = new AlbumInfo();
        //	缺少数据，属性赋值：
        BeanUtils.copyProperties(albumInfoVo, albumInfo);
        //	创建专辑的时候，应该知道是谁创建的专辑
        albumInfo.setUserId(userId);
        //	免费试听集数:
        if (!albumInfo.getPayType().equals(SystemConstant.ALBUM_PAY_TYPE_FREE)) {
            //  设置免费试听集数
            albumInfo.setTracksForFree(5);
        }
        //	专辑对应的状态
        albumInfo.setStatus(SystemConstant.ALBUM_STATUS_PASS);
        //  执行了保存专辑的操作
        albumInfoMapper.insert(albumInfo);

        //  2.  album_attribute_value
        List<AlbumAttributeValueVo> albumAttributeValueVoList = albumInfoVo.getAlbumAttributeValueVoList();
        if (!CollectionUtils.isEmpty(albumAttributeValueVoList)){
            //  循环遍历保存数据
            for (AlbumAttributeValueVo albumAttributeValueVo : albumAttributeValueVoList) {
                //  创建对象
                AlbumAttributeValue albumAttributeValue = new AlbumAttributeValue();
                //  属性拷贝：
                BeanUtils.copyProperties(albumAttributeValueVo, albumAttributeValue);
                //  给专辑属性Id 赋值 @TableId(type = IdType.AUTO) 表示能够获取到主键自增值！
                albumAttributeValue.setAlbumId(albumInfo.getId());
                //  insert into album_attribute_value values(?,?,?,?,?);
                //  insert into album_attribute_value values(?,?,?,?,?);
                //  insert into album_attribute_value values(?,?,?,?,?);
                //  insert into album_attribute_value values(?,?,?,?,?);
                //  insert into album_attribute_value values(?,?,?,?,?);
                albumAttributeValueMapper.insert(albumAttributeValue);
            }
            //  insert into album_attribute_value values(?,?,?,?,?) (?,?,?,?,?) (?,?,?,?,?) (?,?,?,?,?) (?,?,?,?,?);
        }

        //  3. album_stat
        this.saveAlbumStat(albumInfo.getId(), SystemConstant.ALBUM_STAT_PLAY);
        this.saveAlbumStat(albumInfo.getId(), SystemConstant.ALBUM_STAT_SUBSCRIBE);
        this.saveAlbumStat(albumInfo.getId(), SystemConstant.ALBUM_STAT_BROWSE);
        this.saveAlbumStat(albumInfo.getId(), SystemConstant.ALBUM_STAT_COMMENT);

        //  判断当前专辑字段isOpen 是否为上架
        //  判断当前专辑是否是上架下架
        if ("1".equals(albumInfo.getIsOpen())){
            //  上架
            rabbitService.sendMessage(MqConst.EXCHANGE_ALBUM,MqConst.ROUTING_ALBUM_UPPER,albumInfo.getId());
            //  获取布隆过滤器
            RBloomFilter<Object> bloomFilter = this.redissonClient.getBloomFilter(RedisConstant.ALBUM_BLOOM_FILTER);
            bloomFilter.add(albumInfo.getId());
        }


    }

    private void saveAlbumStat(Long albumId, String statPlay) {
        //  创建对象
        AlbumStat albumStat = new AlbumStat();
        albumStat.setAlbumId(albumId);
        albumStat.setStatType(statPlay);
        albumStat.setStatNum(new Random().nextInt(10000));
        albumStatMapper.insert(albumStat);
    }
}
