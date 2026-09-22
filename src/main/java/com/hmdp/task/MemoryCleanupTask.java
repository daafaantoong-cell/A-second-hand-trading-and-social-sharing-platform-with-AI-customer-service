package com.hmdp.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hmdp.entity.ChatMemory;
import com.hmdp.mapper.ChatMemoryMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 会话记忆清理任务
 * 每天 03:00 清理 MySQL chat_memory 表中 30 天未更新的记录
 * (Redis 端靠 TTL 24h 自动清理,这里只管 MySQL)
 */
@Slf4j
@Component
public class MemoryCleanupTask {

    /** 保留天数:超过 30 天未更新的记录视为冷数据,清理 */
    private static final int RETENTION_DAYS = 30;

    @Autowired
    private ChatMemoryMapper chatMemoryMapper;

    /**
     * 每天凌晨 3 点执行
     * cron: 秒 分 时 日 月 周
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanup() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(RETENTION_DAYS);
        LambdaQueryWrapper<ChatMemory> wrapper = new LambdaQueryWrapper<>();
        wrapper.lt(ChatMemory::getUpdateTime, threshold);

        int deleted = chatMemoryMapper.delete(wrapper);
        log.info("[MemoryCleanup] 清理 {} 天前会话记忆,删除记录数: {}", RETENTION_DAYS, deleted);
    }
}
