package com.atguigu.tingshu.vo.user;

import lombok.Data;

import java.util.List;

@Data
public class UserPaidRecordVo {
    //  订单编号
    private String orderNo;
    //  用户Id
    private Long userId;
    //  购买项目类型  专辑 声音 vip
    private String itemType;
    //  购买项目类型Id {专辑Id, 声音Id, vipConfigId}
    private List<Long> itemIdList;
}
