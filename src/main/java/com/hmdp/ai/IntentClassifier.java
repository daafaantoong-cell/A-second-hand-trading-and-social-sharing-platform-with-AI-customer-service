package com.hmdp.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;

/**
 * 意图识别节点
 * <p>
 * 职责：对用户 Query 进行分类，输出意图标签，作为 DAG 路由的依据。
 * 不直接调用工具，仅做分类，保证路由逻辑可观测、可调试。
 * <p>
 * 意图枚举：
 * SHOP_CONSULT  美食/商家咨询（推荐、探店、营业时间、地址等）
 * ORDER_QUERY   订单/券查询（我的订单、券状态、核销、退款进度等）
 * REPORT        举报投诉（举报、投诉、违规、侵权等）
 * BLOG_CREATE   探店内容创作（写点评、发博客、帮我写探店等）
 * FALLBACK      其他（闲聊、开放问答，走 RAG 兜底）
 */
@AiService(
        wiringMode = AiServiceWiringMode.EXPLICIT,
        chatModel = "openAiChatModel"
)
public interface IntentClassifier {

    @SystemMessage("""
            你是一个客服意图分类器。请根据用户输入，判断其意图并只输出一个意图标签，不要输出任何其他内容。

            可选意图标签及分类规则：
            - SHOP_CONSULT：用户咨询美食、商家、探店推荐、营业时间、地址、评分、人均消费等与商家相关的问题
            - ORDER_QUERY：用户查询订单、代金券、核销状态、退款进度等与订单/券相关的问题
            - REPORT：用户要举报、投诉、反馈违规内容（如虚假宣传、侵权、不良信息等）
            - BLOG_CREATE：用户想写探店点评、发布博客、需要内容创作帮助
            - FALLBACK：无法归入以上四类的闲聊或开放问答

            注意：只输出标签本身，例如：SHOP_CONSULT
            """)
    String classify(@UserMessage String userQuery);
}
