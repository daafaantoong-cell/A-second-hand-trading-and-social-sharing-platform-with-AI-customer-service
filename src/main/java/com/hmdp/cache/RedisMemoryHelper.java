package com.hmdp.cache;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Component
public class RedisMemoryHelper {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    public List<ChatMessage> get(Object key) {
        String StrKey=key.toString();
        Object MemoryListObject=stringRedisTemplate.opsForValue().get(StrKey);
        if(MemoryListObject==null){
            return Collections.emptyList();
        }
        String MemoryListString=MemoryListObject.toString();
        List<ChatMessage> MemoryList= ChatMessageDeserializer.messagesFromJson(MemoryListString);
        return MemoryList;
    }

    public void set(Object key, List<ChatMessage> MemoryList) {
        String StrKey=key.toString();
        String MemoryListJSON= ChatMessageSerializer.messagesToJson(MemoryList);
        stringRedisTemplate.opsForValue().set(StrKey, MemoryListJSON);
    }

    /**
     * 带过期时间写入
     * @param key         缓存 key
     * @param MemoryList  消息列表
     * @param ttlSeconds  TTL 秒数 (24h = 86400)
     */
    public void setWithTtl(Object key, List<ChatMessage> MemoryList, long ttlSeconds) {
        String StrKey=key.toString();
        String MemoryListJSON= ChatMessageSerializer.messagesToJson(MemoryList);
        stringRedisTemplate.opsForValue().set(StrKey, MemoryListJSON, Duration.ofSeconds(ttlSeconds));
    }

    public void delete(Object key) {
        String StrKey=key.toString();
        stringRedisTemplate.delete(StrKey);
    }
}
