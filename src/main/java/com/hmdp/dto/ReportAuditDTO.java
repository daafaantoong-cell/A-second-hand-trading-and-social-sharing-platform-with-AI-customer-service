package com.hmdp.dto;

import lombok.Data;

/**
 * 举报审核入参(后台用)
 */
@Data
public class ReportAuditDTO {
    /** 举报ID */
    private Long id;
    /** 处理状态: 1已受理 2已驳回 3已处理 */
    private Integer status;
    /** 处理备注 */
    private String handleRemark;
}
