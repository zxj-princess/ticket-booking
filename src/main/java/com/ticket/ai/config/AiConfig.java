package com.ticket.ai.config;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore;
import dev.langchain4j.data.segment.TextSegment;
import com.ticket.ai.agent.TicketBookingAgent;
import com.ticket.ai.agent.TicketBookingTools;
import dev.langchain4j.service.AiServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI 配置类
 * 配置大模型、Embedding模型、向量存储、对话记忆和AI Agent服务
 */
@Configuration
public class AiConfig {

    private static final Logger log = LoggerFactory.getLogger(AiConfig.class);

    @Value("${ai.chat-model.api-key}")
    private String chatApiKey;

    @Value("${ai.chat-model.model-name}")
    private String chatModelName;

    @Value("${ai.chat-model.base-url}")
    private String chatBaseUrl;

    @Value("${ai.embedding-model.api-key}")
    private String embeddingApiKey;

    @Value("${ai.embedding-model.model-name}")
    private String embeddingModelName;

    @Value("${ai.embedding-model.base-url}")
    private String embeddingBaseUrl;

    @Value("${ai.vector-store.milvus.host:localhost}")
    private String milvusHost;

    @Value("${ai.vector-store.milvus.port:19530}")
    private int milvusPort;

    @Value("${ai.vector-store.milvus.collection-name:ticket_knowledge}")
    private String milvusCollectionName;

    @Value("${ai.vector-store.milvus.dimension:1536}")
    private int milvusDimension;

    @Value("${ai.vector-store.type:milvus}")
    private String vectorStoreType;

    /**
     * 配置聊天大模型（增加超时和重试配置，避免Connection reset）
     */
    @Bean
    public dev.langchain4j.model.chat.StreamingChatLanguageModel streamingChatModel() {
        return dev.langchain4j.model.openai.OpenAiStreamingChatModel.builder()
            .baseUrl(chatBaseUrl).apiKey(chatApiKey).modelName(chatModelName)
            .timeout(java.time.Duration.ofSeconds(90)).temperature(0.3).build();
    }

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        log.info("初始化聊天模型: {}", chatModelName);
        return OpenAiChatModel.builder()
                .baseUrl(chatBaseUrl)
                .apiKey(chatApiKey)
                .modelName(chatModelName)
                .timeout(java.time.Duration.ofSeconds(60))
                .maxRetries(3)
                .temperature(0.7)
                .build();
    }

    /**
     * 配置Embedding模型（增加超时配置）
     */
    @Bean
    public EmbeddingModel embeddingModel() {
        log.info("初始化Embedding模型: {}", embeddingModelName);
        return OpenAiEmbeddingModel.builder()
                .baseUrl(embeddingBaseUrl)
                .apiKey(embeddingApiKey)
                .modelName(embeddingModelName)
                .timeout(java.time.Duration.ofSeconds(60))
                .maxRetries(3)
                .build();
    }

    /**
     * 配置向量存储（使用Milvus向量数据库）
     */
    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        if ("memory".equalsIgnoreCase(vectorStoreType)) return new dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore<>();
        log.info("初始化Milvus向量存储: {}:{} / collection={} / dimension={}", milvusHost, milvusPort, milvusCollectionName, milvusDimension);
        return MilvusEmbeddingStore.builder()
                .host(milvusHost)
                .port(milvusPort)
                .collectionName(milvusCollectionName)
                .dimension(milvusDimension)
                .build();
    }

    /**
     * 配置对话记忆提供者
     * 基于 sessionId 为每个会话独立维护一份记忆窗口（最多保留20条消息）
     */
    @Bean
    public ChatMemoryProvider chatMemoryProvider() {
        log.info("初始化ChatMemoryProvider");
        final Map<Object, ChatMemory> memoryStore = new ConcurrentHashMap<>();
        return new ChatMemoryProvider() {
            @Override
            public ChatMemory get(Object memoryId) {
                return memoryStore.computeIfAbsent(memoryId,
                        id -> MessageWindowChatMemory.withMaxMessages(20));
            }
        };
    }

    /**
     * 配置AI Agent服务（带对话记忆 + 工具调用）
     */
    @Bean
    public TicketBookingAgent ticketBookingAgent(ChatLanguageModel chatLanguageModel,
                                                  TicketBookingTools tools,
                                                  ChatMemoryProvider chatMemoryProvider) {
        log.info("初始化AI Agent（带对话记忆+工具调用）");
        return AiServices.builder(TicketBookingAgent.class)
                .chatLanguageModel(chatLanguageModel)
                .chatMemoryProvider(chatMemoryProvider)
                .tools(tools)
                .build();
    }
}
