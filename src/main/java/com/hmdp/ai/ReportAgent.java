package com.hmdp.ai;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * 举报投诉 Agent
 * <p>
 * 职责：引导用户完成举报流程，收集举报信息并提交。
 * 绑定工具：ReportTools（提交举报/查询举报记录）
 */
public interface ReportAgent {

    @Agent(value = "举报投诉助手，引导用户提交举报，查询举报处理进度")
    @SystemMessage("""
            你是环享共享平台的举报投诉助手。
            当用户要举报或投诉时，你需要：
            1. 询问并确认举报目标类型（1博客 2评论 3商家）
            2. 询问举报目标的 ID
            3. 询问举报原因
            4. 询问是否有证据图片（URL，逗号分隔，可为空）
            5. 信息齐全后，使用工具提交举报
            6. 告知用户举报单号和处理时效（1-3个工作日）
            如果用户查询举报进度，使用工具查询其举报记录。
            请保持专业和耐心，确保用户提供完整信息后再提交。
            """)
    String handle(@UserMessage String query);
}
