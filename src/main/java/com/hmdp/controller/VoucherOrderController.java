package com.hmdp.controller;


import com.hmdp.dto.RefundApplyDTO;
import com.hmdp.dto.RefundAuditDTO;
import com.hmdp.dto.Result;
import com.hmdp.service.IVoucherOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author 虎哥
 */
@Tag(name = "订单服务")
@RestController
@RequestMapping("/voucher-order")
public class VoucherOrderController {

    @Resource
    private IVoucherOrderService voucherOrderService;

    /**
     * 秒杀下单
     */
    @Operation(summary = "秒杀下单")
    @PostMapping("seckill/{id}")
    public Result seckillVoucher(@PathVariable("id") Long voucherId) {
        return voucherOrderService.seckillVoucher(voucherId);
    }

    /**
     * 当前登录用户的订单列表(分页+状态筛选)
     */
    @Operation(summary = "我的订单列表")
    @GetMapping("/my")
    public Result myOrders(
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "current", defaultValue = "1") Integer current) {
        return voucherOrderService.queryMyOrders(status, current);
    }

    /**
     * 订单详情
     */
    @Operation(summary = "订单详情")
    @GetMapping("/{orderId}")
    public Result orderDetail(@PathVariable("orderId") Long orderId) {
        return voucherOrderService.queryOrderDetail(orderId);
    }

    /**
     * 申请退款
     */
    @Operation(summary = "申请退款")
    @PostMapping("/refund/apply")
    public Result applyRefund(@RequestBody RefundApplyDTO dto) {
        return voucherOrderService.applyRefund(dto.getOrderId(), dto.getReason());
    }

    /**
     * 退款详情(按订单)
     */
    @Operation(summary = "退款详情")
    @GetMapping("/refund/{orderId}")
    public Result refundDetail(@PathVariable("orderId") Long orderId) {
        return voucherOrderService.queryRefundDetail(orderId);
    }

    /**
     * 当前用户的退款申请列表(分页)
     */
    @Operation(summary = "我的退款申请列表")
    @GetMapping("/refund/list")
    public Result refundList(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        return voucherOrderService.queryRefundList(current);
    }

    /**
     * 撤销退款申请
     */
    @Operation(summary = "撤销退款申请")
    @PostMapping("/refund/cancel/{orderId}")
    public Result cancelRefund(@PathVariable("orderId") Long orderId) {
        return voucherOrderService.cancelRefund(orderId);
    }

    /**
     * 后台审核退款
     */
    @Operation(summary = "后台审核退款")
    @PutMapping("/refund/audit")
    public Result auditRefund(@RequestBody RefundAuditDTO dto) {
        return voucherOrderService.auditRefund(dto.getOrderId(), dto.getStatus(), dto.getRemark());
    }
}
