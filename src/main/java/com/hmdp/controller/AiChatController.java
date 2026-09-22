package com.hmdp.controller;

import com.hmdp.ai.IntentClassifier;
import com.hmdp.dto.Result;
import com.hmdp.service.AiChatService;
import com.hmdp.service.BlogEmbeddingService;
import com.hmdp.utils.UserHolder;
import dev.langchain4j.agentic.supervisor.SupervisorAgent;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@Slf4j
@RestController
@RequestMapping("/chat")
public class AiChatController {

    @Autowired
    private OpenAiChatModel openAiChatModel;
    @Autowired
    private AiChatService aiChatService;
    @Autowired
    private BlogEmbeddingService blogEmbeddingService;

    /** Workflow 驱动的智能客服入口（Supervisor 模式 DAG 编排） */
    @Autowired
    private SupervisorAgent customerServiceWorkflow;

    /** 意图识别节点（独立可调试） */
    @Autowired
    private IntentClassifier intentClassifier;

    @PostMapping("/normal")
    public String normalChat(@RequestParam String text) {
        return openAiChatModel.chat(text);
    }

    /**
     * 流式对话 (支持多会话隔离) —— 原 RAG 闲聊入口，保留作为 Fallback 对比
     * @param sessionId 会话 ID (前端生成 UUID,可选;不传则用 default 单会话)
     * @param text      用户输入
     */
    @PostMapping("/flux")
    public Flux<String> fluxChat(
            @RequestParam(required = false) String sessionId,
            @RequestParam String text) {
        Long userId = UserHolder.getUser().getId();
        String memoryId = userId + ":" + (sessionId == null || sessionId.isBlank() ? "default" : sessionId);
        return aiChatService.fluxChat(memoryId, text);
    }

    /**
     * Workflow 驱动的智能客服（核心入口）
     * <p>
     * 流程：Supervisor Agent 识别意图 → 路由到对应子 Agent → 子 Agent 调用 @Tool 执行业务 → 汇总回复
     * 覆盖：美食咨询、订单查询、举报投诉、探店创作；其余走 Supervisor 直接回复（闲聊兜底）
     *
     * @param text 用户输入
     * @return 客服回复文本
     */
    @PostMapping("/workflow")
    public String workflowChat(@RequestParam String text) {
        log.info("[Workflow] user query: {}", text);
        String response = customerServiceWorkflow.invoke(text);
        log.info("[Workflow] response: {}", response);
        return response;
    }

    /**
     * 意图分类调试接口（仅用于开发调试，验证意图识别节点准确率）
     */
    @PostMapping("/intent")
    public String classifyIntent(@RequestParam String text) {
        return intentClassifier.classify(text);
    }

    /**
     * 全量初始化 RAG 向量库（管理端）
     * 临时方案：登录用户 id == 1 视为管理员。
     * TODO：生产环境补 RBAC（如基于角色/权限码的拦截器）
     */
    @PostMapping("/rag/init")
    public Result ragInit() {
        Long userId;
        try {
            userId = UserHolder.getUser().getId();
        } catch (Exception e) {
            return Result.fail("未登录或登录态失效");
        }
        if (userId == null || userId != 1L) {
            return Result.fail("无权限,仅管理员可触发全量初始化");
        }
        int total = blogEmbeddingService.initAll();
        return Result.ok("全量初始化完成,共写入 " + total + " 条 blog 向量");
    }
}
