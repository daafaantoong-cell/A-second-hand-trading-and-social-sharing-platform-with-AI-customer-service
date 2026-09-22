package com.hmdp.tool;

import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.service.IReportService;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

/**
 * 举报相关工具
 * 供 ReportAgent 调用，提供举报提交与历史举报查询
 */
@Slf4j
@Component
public class ReportTools {

    @Resource
    private IReportService reportService;

    /**
     * 提交举报（举报人身份由 Service 内部从 UserHolder 获取）
     *
     * @param targetType   举报目标类型：1博客 2评论 3商家
     * @param targetId     举报目标 ID
     * @param reason       举报原因
     * @param evidenceUrls 证据图 URL，逗号分隔（可空）
     */
    @Tool("提交举报；targetType: 1博客 2评论 3商家；reason为举报原因；evidenceUrls为证据图URL逗号分隔（可为空）")
    public String submitReport(Integer targetType, Long targetId,
                               String reason, String evidenceUrls) {
        try {
            Result result = reportService.submit(targetType, targetId, reason, evidenceUrls);
            if (!result.getSuccess()) {
                return "举报提交失败：" + result.getErrorMsg();
            }
            return "举报提交成功，举报单号：" + JSONUtil.toJsonStr(result.getData())
                    + "。我们会在1-3个工作日内审核处理，请耐心等待。";
        } catch (Exception e) {
            log.error("submitReport error, targetType={}, targetId={}", targetType, targetId, e);
            return "提交举报时出错：" + e.getMessage();
        }
    }

    /**
     * 查询当前登录用户的举报列表
     *
     * @param current 页码，从 1 开始
     */
    @Tool("查询当前用户的举报记录列表，包括举报对象、原因、处理状态等")
    public String queryMyReports(Integer current) {
        try {
            Result result = reportService.queryMyList(current);
            if (!result.getSuccess()) {
                return "查询失败：" + result.getErrorMsg();
            }
            Object data = result.getData();
            if (data == null) {
                return "暂无举报记录";
            }
            return "举报记录：" + JSONUtil.toJsonStr(data);
        } catch (Exception e) {
            log.error("queryMyReports error", e);
            return "查询举报记录时出错：" + e.getMessage();
        }
    }
}
