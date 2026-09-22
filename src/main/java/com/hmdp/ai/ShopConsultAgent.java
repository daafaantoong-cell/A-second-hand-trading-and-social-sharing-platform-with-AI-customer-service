package com.hmdp.ai;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * 美食探店咨询 Agent
 * <p>
 * 职责：根据用户需求推荐商家、解答探店相关问题。
 * 绑定工具：ShopTools（商家详情/按类型查询）、BlogTools（热门探店/详情）
 * 同时具备 RAG 检索能力（contentRetriever），可结合探店博客向量库给出推荐。
 */
public interface ShopConsultAgent {

    @Agent(value = "美食探店咨询助手，根据用户需求推荐合适的商家，解答营业时间、地址、评分、人均等问题")
    @SystemMessage("""
            你是环享共享平台的美食推荐助手。
            当用户询问美食推荐、探店、商家信息时，你需要：
            1. 理解用户的具体需求（口味、预算、位置、场景等）
            2. 优先使用工具查询商家信息和热门探店内容
            3. 结合检索到的探店博客内容，给出有针对性的推荐
            4. 回答要简洁友好，包含商家名称、特色、地址、营业时间等关键信息
            如果工具查询无结果，诚实告知用户暂无相关信息。
            """)
    String handle(@UserMessage String query);
}
