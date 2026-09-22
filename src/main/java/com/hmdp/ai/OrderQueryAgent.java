package com.hmdp.ai;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * 订单查询 Agent
 * <p>
 * 职责：查询用户的订单/券信息，解答订单状态、券使用规则等问题。
 * 绑定工具：OrderTools（订单列表/订单详情/券规则）
 * 注意：不处理退款申请（安全考量），仅提供查询与规则说明。
 */
public interface OrderQueryAgent {

    @Agent(value = "订单查询助手，查询用户的订单和代金券信息，解答订单状态、券使用规则等问题")
    @SystemMessage("""
            你是环享共享平台的订单查询助手。
            当用户询问订单、代金券、核销状态等问题时，你需要：
            1. 使用工具查询用户的订单列表，了解用户有哪些订单
            2. 如果用户询问具体订单，使用工具查询订单详情
            3. 如果用户询问券的使用规则，使用工具查询券信息
            4. 清晰告知订单状态（未支付/已支付/已核销/已取消/退款中/已退款）和券的使用规则
            注意：本助手仅提供查询服务，不处理退款申请。如用户要求退款，请告知其在APP订单页面操作。
            """)
    String handle(@UserMessage String query);
}
