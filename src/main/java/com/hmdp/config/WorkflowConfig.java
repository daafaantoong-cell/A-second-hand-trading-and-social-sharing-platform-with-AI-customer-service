package com.hmdp.config;

import com.hmdp.ai.BlogAgent;
import com.hmdp.ai.OrderQueryAgent;
import com.hmdp.ai.ReportAgent;
import com.hmdp.ai.ShopConsultAgent;
import com.hmdp.tool.BlogTools;
import com.hmdp.tool.OrderTools;
import com.hmdp.tool.ReportTools;
import com.hmdp.tool.ShopTools;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.supervisor.SupervisorAgent;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.Resource;

/**
 * Workflow 编排配置
 * <p>
 * 架构：Supervisor 模式的 DAG 编排
 * <pre>
 *                    ┌─────────────────────┐
 *                    │   Supervisor Agent   │  ← 意图识别 + 路由决策
 *                    └──────────┬──────────┘
 *                               │ 条件路由
 *        ┌──────────┬───────────┼───────────┬──────────┐
 *        ▼          ▼           ▼           ▼          ▼
 *  ShopConsult  OrderQuery   Report      Blog      (结束)
 *    Agent       Agent       Agent       Agent
 *  绑定工具:    绑定工具:    绑定工具:    绑定工具:
 *  Shop+Blog   Order       Report      Shop+Blog
 *  +RAG检索
 * </pre>
 * <p>
 * 每个子 Agent 绑定自己专属的 @Tool 集合，避免工具泛滥导致 LLM 误调用；
 * ShopConsultAgent 额外注入 ContentRetriever 实现 RAG 增强推荐。
 */
@Slf4j
@Configuration
public class WorkflowConfig {

    @Resource
    private ChatModel openAiChatModel;

    @Resource
    private ShopTools shopTools;

    @Resource
    private BlogTools blogTools;

    @Resource
    private OrderTools orderTools;

    @Resource
    private ReportTools reportTools;

    @Resource
    private ContentRetriever blogContentRetriever;

    /**
     * 构建美食咨询 Agent：绑定商家+博客工具 + RAG 检索
     */
    @Bean
    public ShopConsultAgent shopConsultAgent() {
        return AgenticServices.agentBuilder(ShopConsultAgent.class)
                .chatModel(openAiChatModel)
                .tools(shopTools, blogTools)
                .contentRetriever(blogContentRetriever)
                .build();
    }

    /**
     * 构建订单查询 Agent：绑定订单工具
     */
    @Bean
    public OrderQueryAgent orderQueryAgent() {
        return AgenticServices.agentBuilder(OrderQueryAgent.class)
                .chatModel(openAiChatModel)
                .tools(orderTools)
                .build();
    }

    /**
     * 构建举报投诉 Agent：绑定举报工具
     */
    @Bean
    public ReportAgent reportAgent() {
        return AgenticServices.agentBuilder(ReportAgent.class)
                .chatModel(openAiChatModel)
                .tools(reportTools)
                .build();
    }

    /**
     * 构建探店创作 Agent：绑定商家+博客工具
     */
    @Bean
    public BlogAgent blogAgent() {
        return AgenticServices.agentBuilder(BlogAgent.class)
                .chatModel(openAiChatModel)
                .tools(shopTools, blogTools)
                .build();
    }

    /**
     * 构建 Supervisor 工作流（DAG 根节点）
     * <p>
     * Supervisor 内部维护 AgenticScope，根据用户输入自主决策调用哪个子 Agent，
     * 子 Agent 执行结果回传 Supervisor，由 Supervisor 决定继续路由或结束。
     * maxAgentsInvocations=3 限制最多调用子 Agent 次数，防止死循环。
     */
    @Bean
    public SupervisorAgent customerServiceWorkflow(
            ShopConsultAgent shopConsultAgent,
            OrderQueryAgent orderQueryAgent,
            ReportAgent reportAgent,
            BlogAgent blogAgent) {

        log.info("Building Supervisor Workflow with 4 sub-agents...");

        return AgenticServices.supervisorBuilder()
                .chatModel(openAiChatModel)
                .name("CustomerServiceSupervisor")
                .description("""
                        你是环享共享平台的智能客服主管。
                        你的职责是理解用户需求，判断意图，并将任务分配给合适的子 Agent 处理：
                        - 美食/商家咨询 → 交给 ShopConsultAgent
                        - 订单/券查询 → 交给 OrderQueryAgent
                        - 举报投诉 → 交给 ReportAgent
                        - 探店内容创作 → 交给 BlogAgent
                        - 无法归类的闲聊 → 直接友善回复，无需调用子 Agent
                        子 Agent 处理完毕后，将结果整理后回复用户。
                        """)
                .subAgents(shopConsultAgent, orderQueryAgent, reportAgent, blogAgent)
                .maxAgentsInvocations(3)
                .build();
    }
}
