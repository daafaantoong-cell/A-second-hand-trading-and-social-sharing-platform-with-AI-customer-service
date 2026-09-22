package com.hmdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.dto.Result;
import com.hmdp.entity.VoucherOrder;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IVoucherOrderService extends IService<VoucherOrder> {

    /**
     * 秒杀下单
     */
    Result seckillVoucher(Long voucherId);

    /**
     * 当前登录用户的订单列表(分页)
     * @param status 订单状态可空, 1未付/2已付/3已核销/4已取消/5退款中/6已退款
     */
    Result queryMyOrders(Integer status, Integer current);

    /**
     * 订单详情(带越权校验: 只能看自己的订单)
     */
    Result queryOrderDetail(Long orderId);

    /**
     * 申请退款: 订单状态 -> 退款中(5)
     * @param orderId 订单ID
     * @param reason 退款原因
     */
    Result applyRefund(Long orderId, String reason);

    /**
     * 查询退款详情(越权校验)
     */
    Result queryRefundDetail(Long orderId);

    /**
     * 当前用户的退款申请列表(分页)
     */
    Result queryRefundList(Integer current);

    /**
     * 撤销退款申请: 退款中(5) -> 已支付(2)
     */
    Result cancelRefund(Long orderId);

    /**
     * 后台审核退款: 退款中(5) -> 已退款(6) 或 已取消(4)
     */
    Result auditRefund(Long orderId, Integer status, String remark);
}
