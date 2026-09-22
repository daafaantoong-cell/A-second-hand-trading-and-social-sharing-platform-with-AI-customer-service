package com.hmdp.tool;

import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Voucher;
import com.hmdp.service.IVoucherOrderService;
import com.hmdp.service.IVoucherService;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

/**
 * 订单查询相关工具
 * 供 OrderQueryAgent 调用，提供用户订单列表、订单详情、券规则查询
 * 注意：退款功能暂不开放（避免安全审计复杂度），仅支持查询
 */
@Slf4j
@Component
public class OrderTools {

    @Resource
    private IVoucherOrderService voucherOrderService;

    @Resource
    private IVoucherService voucherService;

    /**
     * 查询当前登录用户的订单列表（用户身份由 Service 内部从 UserHolder 获取）
     *
     * @param status 订单状态（可空）：1未支付 2已支付 3已核销 4已取消 5退款中 6已退款
     */
    @Tool("查询当前用户的订单列表，可按状态筛选；status可选值：1未支付 2已支付 3已核销 4已取消 5退款中 6已退款")
    public String queryMyOrders(Integer status) {
        try {
            Result result = voucherOrderService.queryMyOrders(status, 1);
            if (!result.getSuccess()) {
                return "查询失败：" + result.getErrorMsg();
            }
            Object data = result.getData();
            if (data == null) {
                return "暂无订单记录";
            }
            return "订单列表：" + JSONUtil.toJsonStr(data);
        } catch (Exception e) {
            log.error("queryMyOrders error", e);
            return "查询订单列表时出错：" + e.getMessage();
        }
    }

    /**
     * 查询订单详情（含越权校验，只能查当前登录用户自己的订单）
     *
     * @param orderId 订单 ID
     */
    @Tool("根据订单ID查询订单详情，包括券信息、状态、下单/支付/核销时间等")
    public String queryOrderDetail(Long orderId) {
        try {
            Result result = voucherOrderService.queryOrderDetail(orderId);
            if (!result.getSuccess()) {
                return "查询失败：" + result.getErrorMsg();
            }
            Object data = result.getData();
            if (data == null) {
                return "未找到该订单";
            }
            return "订单详情：" + JSONUtil.toJsonStr(data);
        } catch (Exception e) {
            log.error("queryOrderDetail error, orderId={}", orderId, e);
            return "查询订单详情时出错：" + e.getMessage();
        }
    }

    /**
     * 根据券 ID 查询券的详细规则（标题、抵扣金额、使用规则、有效期等）
     */
    @Tool("根据券ID查询券的详细规则，包括标题、抵扣金额、支付金额、使用规则、有效期等")
    public String queryVoucherInfo(Long voucherId) {
        try {
            Voucher voucher = voucherService.getById(voucherId);
            if (voucher == null) {
                return "未找到该券";
            }
            return String.format(
                    "券信息：标题=%s, 副标题=%s, 使用规则=%s, 支付金额=%d元, 抵扣金额=%d元, 类型=%d, 状态=%d",
                    voucher.getTitle(), voucher.getSubTitle(), voucher.getRules(),
                    voucher.getPayValue(), voucher.getActualValue(),
                    voucher.getType(), voucher.getStatus()
            );
        } catch (Exception e) {
            log.error("queryVoucherInfo error, voucherId={}", voucherId, e);
            return "查询券信息时出错：" + e.getMessage();
        }
    }
}
