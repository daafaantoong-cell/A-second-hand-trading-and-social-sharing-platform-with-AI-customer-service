package com.hmdp.tool;

import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.service.IBlogService;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

/**
 * 探店博客相关工具
 * 供 ShopConsultAgent / BlogAgent 调用，提供热门探店内容与详情查询
 */
@Slf4j
@Component
public class BlogTools {

    @Resource
    private IBlogService blogService;

    /**
     * 查询热门探店博客（按点赞数排序）
     *
     * @param current 页码，从 1 开始
     */
    @Tool("查询热门探店博客列表，按热度排序，返回探店内容、商家信息、点赞数等；current为页码")
    public String queryHotBlogs(Integer current) {
        try {
            Result result = blogService.queryHotBlog(current);
            if (!result.getSuccess()) {
                return "查询失败：" + result.getErrorMsg();
            }
            Object data = result.getData();
            if (data == null) {
                return "暂无热门探店内容";
            }
            return "热门探店：" + JSONUtil.toJsonStr(data);
        } catch (Exception e) {
            log.error("queryHotBlogs error", e);
            return "查询热门探店时出错：" + e.getMessage();
        }
    }

    /**
     * 根据博客 ID 查询探店详情
     */
    @Tool("根据探店博客ID查询详细内容，包括正文、图片、商家、点赞数等")
    public String queryBlogById(Long id) {
        try {
            Result result = blogService.queryBlogById(id);
            if (!result.getSuccess()) {
                return "查询失败：" + result.getErrorMsg();
            }
            Object data = result.getData();
            if (data == null) {
                return "未找到该探店内容";
            }
            return "探店详情：" + JSONUtil.toJsonStr(data);
        } catch (Exception e) {
            log.error("queryBlogById error, id={}", id, e);
            return "查询探店详情时出错：" + e.getMessage();
        }
    }
}
