package com.hmdp.service;

import com.hmdp.entity.Blog;

/**
 * Blog 向量化服务
 * 将与店铺关联的 blog 测评 (title + content) 向量化并写入 Redis Stack，
 * 供 AiChatService 通过 ContentRetriever 做语义层面商家推荐。
 */
public interface BlogEmbeddingService {

    /**
     * 全量初始化：清空向量库 → 分页扫描 tb_blog → 批量写入
     * 调用场景：管理端手动触发 / 维护期重建索引
     * @return 实际写入条数
     */
    int initAll();

    /**
     * 单条 blog 增量写入向量库
     * 调用场景：blog 发布成功后同步写入
     */
    void addOne(Blog blog);

    /**
     * 按 blogId 从向量库删除对应条目
     * 调用场景：blog 更新（先删后增）/ blog 删除
     */
    void removeOne(Long blogId);
}
