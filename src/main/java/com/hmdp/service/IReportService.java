package com.hmdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.dto.Result;
import com.hmdp.entity.Report;

/**
 * 举报服务接口
 */
public interface IReportService extends IService<Report> {

    /**
     * 提交举报
     * @param targetType 目标类型 1博客 2评论 3商家
     * @param targetId 目标ID
     * @param reason 举报原因
     * @param evidenceUrls 证据图URL,逗号分隔(可空)
     */
    Result submit(Integer targetType, Long targetId, String reason, String evidenceUrls);

    /** 举报详情(越权校验) */
    Result queryDetail(Long id);

    /** 当前用户的举报列表(分页) */
    Result queryMyList(Integer current);

    /** 撤回未处理的举报 */
    Result revoke(Long id);

    /** 后台审核举报 */
    Result audit(Long id, Integer status, String handleRemark);
}
