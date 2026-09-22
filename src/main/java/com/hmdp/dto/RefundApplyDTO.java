package com.hmdp.dto;

import lombok.Data;

/**
 * 申请退款入参
 */
@Data
public class RefundApplyDTO {
    /** 订单ID */
    private Long orderId;
    /** 退款原因 */
    private String reason;
}
