package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.entity.Blog;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.BlogMapper;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.BlogEmbeddingService;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;

/**
 * Blog 向量化实现
 * 设计要点：
 * 1. 直接注入 BlogMapper/ShopMapper，避免与 IBlogService 形成循环依赖
 * 2. 文本构造：title + "。" + content；metadata 携带 shopId/shopName/area/avgPrice/score/blogId
 * 3. 全量初始化分批 BATCH_SIZE=50 条，避免一次性打爆 DashScope embedding 配额
 * 4. content 长度 < 20 字符的不入库（语义信号过弱）
 * 5. 同步失败只记日志不抛异常，避免 RAG 故障影响 blog 主链路写入
 */
@Slf4j
@Service
public class BlogEmbeddingServiceImpl implements BlogEmbeddingService {

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private EmbeddingStore<TextSegment> embeddingStore;

    @Autowired
    private BlogMapper blogMapper;

    @Autowired
    private ShopMapper shopMapper;

    /** 批量大小：每批 50 条 embedding 调用 */
    private static final int BATCH_SIZE = 50;

    /** content 最小长度阈值：低于此长度不入库 */
    private static final int MIN_CONTENT_LENGTH = 20;

    @Override
    public int initAll() {
        // 1. 清空向量库
        try {
            embeddingStore.removeAll();
            log.info("[BlogEmbedding] 已清空向量库");
        } catch (Exception e) {
            log.warn("[BlogEmbedding] 清空向量库失败,继续后续写入", e);
        }

        // 2. 分页扫描 tb_blog
        int total = 0;
        long current = 1L;
        while (true) {
            Page<Blog> page = blogMapper.selectPage(new Page<>(current, BATCH_SIZE), null);
            List<Blog> records = page.getRecords();
            if (records == null || records.isEmpty()) {
                break;
            }

            // 2.1 过滤过短内容
            List<Blog> valid = records.stream()
                    .filter(b -> b.getContent() != null && b.getContent().length() >= MIN_CONTENT_LENGTH)
                    .collect(Collectors.toList());

            if (!valid.isEmpty()) {
                // 2.2 批量查 shop
                Set<Long> shopIds = valid.stream()
                        .map(Blog::getShopId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
                Map<Long, Shop> shopMap = shopIds.isEmpty()
                        ? Collections.emptyMap()
                        : shopMapper.selectBatchIds(shopIds).stream()
                                .collect(Collectors.toMap(Shop::getId, s -> s));

                // 2.3 批量 embedding + addAll
                List<Embedding> embeddings = new ArrayList<>(valid.size());
                List<TextSegment> segments = new ArrayList<>(valid.size());
                for (Blog blog : valid) {
                    Shop shop = blog.getShopId() == null ? null : shopMap.get(blog.getShopId());
                    TextSegment seg = buildSegment(blog, shop);
                    try {
                        Embedding emb = embeddingModel.embed(seg).content();
                        embeddings.add(emb);
                        segments.add(seg);
                    } catch (Exception e) {
                        log.warn("[BlogEmbedding] embedding 失败 blogId={},跳过", blog.getId(), e);
                    }
                }
                if (!embeddings.isEmpty()) {
                    embeddingStore.addAll(embeddings, segments);
                    total += embeddings.size();
                }
            }

            if (!page.hasNext()) {
                break;
            }
            current++;
        }
        log.info("[BlogEmbedding] 全量初始化完成,共写入 {} 条", total);
        return total;
    }

    @Override
    public void addOne(Blog blog) {
        if (blog == null || blog.getId() == null
                || blog.getContent() == null
                || blog.getContent().length() < MIN_CONTENT_LENGTH) {
            return;
        }
        try {
            Shop shop = blog.getShopId() == null ? null : shopMapper.selectById(blog.getShopId());
            TextSegment seg = buildSegment(blog, shop);
            Embedding emb = embeddingModel.embed(seg).content();
            embeddingStore.add(emb, seg);
        } catch (Exception e) {
            log.warn("[BlogEmbedding] 增量写入失败 blogId={}", blog.getId(), e);
        }
    }

    @Override
    public void removeOne(Long blogId) {
        if (blogId == null) {
            return;
        }
        try {
            // 1.0.1-beta6 的 EmbeddingStore.removeAll 用 Filter（不是 Predicate）
            // RedisEmbeddingStore 实现了按 metadata 过滤删除
            Filter filter = metadataKey("blogId").isEqualTo(blogId.toString());
            embeddingStore.removeAll(filter);
        } catch (Exception e) {
            // 不支持或失败时记日志，下次 initAll 会重建
            log.warn("[BlogEmbedding] 删除 blogId={} 失败,可忽略 (后续 initAll 会重建)", blogId, e);
        }
    }

    /**
     * 构造 TextSegment：文本 = title + content；metadata 携带 shop 信息便于 LLM 引用
     */
    private TextSegment buildSegment(Blog blog, Shop shop) {
        String title = blog.getTitle() == null ? "" : blog.getTitle();
        String content = blog.getContent() == null ? "" : blog.getContent();
        String text = title + "。" + content;

        dev.langchain4j.data.document.Metadata md = dev.langchain4j.data.document.Metadata.from(
                "blogId", blog.getId().toString());
        if (blog.getShopId() != null) {
            md.put("shopId", blog.getShopId().toString());
        }
        if (shop != null) {
            if (shop.getName() != null) md.put("shopName", shop.getName());
            if (shop.getArea() != null) md.put("area", shop.getArea());
            if (shop.getAvgPrice() != null) md.put("avgPrice", shop.getAvgPrice().toString());
            if (shop.getScore() != null) md.put("score", shop.getScore().toString());
        }
        return TextSegment.from(text, md);
    }
}
