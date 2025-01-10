package com.atguigu.tingshu.account.client;

import com.atguigu.tingshu.account.client.impl.UserAccountDegradeFeignClient;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.vo.account.AccountDeductVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * <p>
 * 产品列表API接口
 * </p>
 *
 */
@FeignClient(value = "service-account", fallback = UserAccountDegradeFeignClient.class)
public interface UserAccountFeignClient {


    /**
     * 检查及扣减账户余额
     *
     * @param accountDeductVo
     * @return
     */
    @PostMapping("api/account/userAccount/checkAndDeduct")
    Result checkAndDeduct(@RequestBody AccountDeductVo accountDeductVo);

}