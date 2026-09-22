package com.hmdp.controller;

import com.hmdp.dto.ReportAuditDTO;
import com.hmdp.dto.ReportSubmitDTO;
import com.hmdp.dto.Result;
import com.hmdp.service.IReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

/**
 * 举报前端控制器
 */
@Tag(name = "举报服务")
@RestController
@RequestMapping("/report")
public class ReportController {

    @Resource
    private IReportService reportService;

    /**
     * 提交举报
     */
    @Operation(summary = "提交举报")
    @PostMapping
    public Result submit(@RequestBody ReportSubmitDTO dto) {
        return reportService.submit(dto.getTargetType(), dto.getTargetId(),
                dto.getReason(), dto.getEvidenceUrls());
    }

    /**
     * 举报详情
     */
    @Operation(summary = "举报详情")
    @GetMapping("/{id}")
    public Result detail(@PathVariable("id") Long id) {
        return reportService.queryDetail(id);
    }

    /**
     * 当前用户的举报列表(分页)
     */
    @Operation(summary = "我的举报列表")
    @GetMapping("/list")
    public Result myList(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        return reportService.queryMyList(current);
    }

    /**
     * 撤回未处理的举报
     */
    @Operation(summary = "撤回举报")
    @DeleteMapping("/{id}")
    public Result revoke(@PathVariable("id") Long id) {
        return reportService.revoke(id);
    }

    /**
     * 后台审核举报
     */
    @Operation(summary = "后台审核举报")
    @PutMapping("/audit")
    public Result audit(@RequestBody ReportAuditDTO dto) {
        return reportService.audit(dto.getId(), dto.getStatus(), dto.getHandleRemark());
    }
}
