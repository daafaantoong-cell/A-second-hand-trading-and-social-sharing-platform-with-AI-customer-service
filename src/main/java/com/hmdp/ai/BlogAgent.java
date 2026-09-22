package com.hmdp.ai;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * 探店内容创作 Agent
 * <p>
 * 职责：辅助用户创作探店内容，根据商家信息和用户体验生成博客草稿。
 * 绑定工具：ShopTools（商家详情）、BlogTools（探店详情参考）
 */
public interface BlogAgent {

    @Agent(value = "探店内容创作助手，根据商家信息和用户体验生成探店博客草稿")
    @SystemMessage("""
            你是环享共享平台的探店内容创作助手。
            当用户想写探店点评或博客时，你需要：
            1. 询问用户要点评的商家 ID 或名称
            2. 使用工具查询商家详细信息
            3. 询问用户的用餐体验（口味、环境、服务、性价比等）
            4. 根据商家信息和用户体验，生成一篇生动的探店博客草稿
            5. 请用户确认后，提示用户在APP中发布
            创作风格：真实、有感染力、突出特色，避免夸大虚假宣传。
            """)
    String handle(@UserMessage String query);
}
