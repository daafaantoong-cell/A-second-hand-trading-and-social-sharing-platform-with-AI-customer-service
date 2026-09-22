package com.hmdp.dto;

import lombok.Data;

/**
 * 退款审核入参(后台用)
 */
@Data
public class RefundAuditDTO {
    /** 订单ID */
    private Long orderId;
    /** 审核结果: 6=同意退款(已退款) 4=驳回(恢复为已取消) */
    private Integer status;
    /** 审核备注 */
    private String remark;
}
