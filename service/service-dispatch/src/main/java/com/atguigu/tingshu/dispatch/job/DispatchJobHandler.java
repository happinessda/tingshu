package com.atguigu.tingshu.dispatch.job;

import com.atguigu.tingshu.dispatch.mapper.XxlJobLogMapper;
import com.atguigu.tingshu.model.dispatch.XxlJobLog;
import com.atguigu.tingshu.search.client.SearchFeignClient;
import com.atguigu.tingshu.user.client.UserInfoFeignClient;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DispatchJobHandler {

    @Autowired
    private SearchFeignClient searchFeignClient;

    @Autowired
    private XxlJobLogMapper xxlJobLogMapper;

    @Autowired
    private UserInfoFeignClient userInfoFeignClient;

    /**
     * 更新排行榜
     */
    @XxlJob("updateLatelyAlbumRankingJob")
    public void updateLatelyAlbumRankingJob() {
        System.out.println("执行任务...更新排行榜");
        //  创建一个对象
        XxlJobLog xxlJobLog = new XxlJobLog();
        xxlJobLog.setJobId(XxlJobHelper.getJobId());
        long startTime = System.currentTimeMillis();
        try {
            //  更新排行榜
            searchFeignClient.updateLatelyAlbumRanking();
            xxlJobLog.setStatus(1);
        } catch (Exception e) {
            //  执行出现异常
            xxlJobLog.setStatus(0);
            xxlJobLog.setError(e.getMessage());
            e.getMessage();
        }
        long endTime = System.currentTimeMillis();
        xxlJobLog.setTimes((int) (endTime - startTime));
        xxlJobLogMapper.insert(xxlJobLog);
    }

    /**
     * 更新排行榜
     */
    @XxlJob("updateVipExpireStatusJob")
    public void updateVipExpireStatusJob() {
        System.out.println("执行任务...更新vip");
        //  创建一个对象
        XxlJobLog xxlJobLog = new XxlJobLog();
        xxlJobLog.setJobId(XxlJobHelper.getJobId());
        long startTime = System.currentTimeMillis();
        try {
            //  更新排行榜
            userInfoFeignClient.updateVipExpireStatus();
            xxlJobLog.setStatus(1);
        } catch (Exception e) {
            //  执行出现异常
            xxlJobLog.setStatus(0);
            xxlJobLog.setError(e.getMessage());
            e.getMessage();
        }
        long endTime = System.currentTimeMillis();
        xxlJobLog.setTimes((int) (endTime - startTime));
        xxlJobLogMapper.insert(xxlJobLog);
    }
}