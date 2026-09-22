package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.Report;
import com.hmdp.mapper.ReportMapper;
import com.hmdp.service.IReportService;
import com.hmdp.utils.UserHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 举报服务实现
 */
@Service
public class ReportServiceImpl extends ServiceImpl<ReportMapper, Report> implements IReportService {

    @Override
    public Result submit(Integer targetType, Long targetId, String reason, String evidenceUrls) {
        if (targetType == null || targetType < 1 || targetType > 3) {
            return Result.fail("目标类型非法,仅支持 1博客 2评论 3商家");
        }
        if (targetId == null || targetId <= 0) {
            return Result.fail("目标ID非法");
        }
        if (reason == null || reason.trim().isEmpty()) {
            return Result.fail("举报原因不能为空");
        }
        Long userId = UserHolder.getUser().getId();
        Report report = new Report()
                .setReporterId(userId)
                .setTargetType(targetType)
                .setTargetId(targetId)
                .setReason(reason)
                .setEvidenceUrls(evidenceUrls)
                .setStatus(0)
                .setCreateTime(LocalDateTime.now())
                .setUpdateTime(LocalDateTime.now());
        save(report);
        return Result.ok(report.getId());
    }

    @Override
    public Result queryDetail(Long id) {
        Report report = getById(id);
        if (report == null) return Result.fail("举报不存在");
        Long userId = UserHolder.getUser().getId();
        if (!report.getReporterId().equals(userId)) return Result.fail("无权查看该举报");
        return Result.ok(report);
    }

    @Override
    public Result queryMyList(Integer current) {
        Long userId = UserHolder.getUser().getId();
        if (current == null || current < 1) current = 1;
        Page<Report> page = new Page<>(current, 10);
        query().eq("reporter_id", userId)
                .orderByDesc("create_time")
                .page(page);
        return Result.ok(page.getRecords(), page.getTotal());
    }

    @Override
    public Result revoke(Long id) {
        Report report = getById(id);
        if (report == null) return Result.fail("举报不存在");
        Long userId = UserHolder.getUser().getId();
        if (!report.getReporterId().equals(userId)) return Result.fail("无权操作该举报");
        if (report.getStatus() == null || report.getStatus() != 0) {
            return Result.fail("举报已进入处理流程,无法撤回");
        }
        removeById(id);
        return Result.ok(id);
    }

    @Override
    public Result audit(Long id, Integer status, String handleRemark) {
        Report report = getById(id);
        if (report == null) return Result.fail("举报不存在");
        if (report.getStatus() == null || report.getStatus() == 2 || report.getStatus() == 3) {
            return Result.fail("该举报已处理,不能重复审核");
        }
        if (status == null || status < 1 || status > 3) {
            return Result.fail("处理状态非法,仅支持 1已受理 2已驳回 3已处理");
        }
        report.setStatus(status);
        report.setHandleRemark(handleRemark);
        report.setHandlerId(UserHolder.getUser() == null ? null : UserHolder.getUser().getId());
        report.setUpdateTime(LocalDateTime.now());
        updateById(report);
        return Result.ok(id);
    }
}
