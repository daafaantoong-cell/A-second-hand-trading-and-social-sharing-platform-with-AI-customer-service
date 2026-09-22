package com.hmdp.dto;

import lombok.Data;

/**
 * 提交举报入参
 */
@Data
public class ReportSubmitDTO {
    /** 目标类型: 1博客 2评论 3商家 */
    private Integer targetType;
    /** 目标ID */
    private Long targetId;
    /** 举报原因 */
    private String reason;
    /** 证据图URL,逗号分隔(可选) */
    private String evidenceUrls;
}
