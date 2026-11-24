package com.ticket.ai.rag;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class KnowledgeService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeService.class);

    @Autowired
    private EmbeddingStore<TextSegment> embeddingStore;

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private KnowledgeIngestion knowledgeIngestion;

    private volatile boolean initialized = false;

    @PostConstruct
    public void autoInit() {
        initKnowledge();
    }

    public synchronized void initKnowledge() {
        if (initialized) {
            log.info("知识库已初始化，跳过重复加载");
            return;
        }
        try {
            log.info("开始初始化知识库...");
            knowledgeIngestion.ingestFromClasspath("knowledge");
            initialized = true;
            log.info("知识库初始化完成");
        } catch (Exception e) {
            log.error("知识库初始化失败", e);
        }
    }

    public String searchKnowledge(String query) {
        try {
            if (!initialized) {
                initKnowledge();
            }

            Embedding queryEmbedding = embeddingModel.embed(query).content();
            List<EmbeddingMatch<TextSegment>> matches = embeddingStore.findRelevant(queryEmbedding, 3, 0.6);

            if (matches == null || matches.isEmpty()) {
                return "知识库中未找到与「" + query + "」相关的信息。";
            }

            return matches.stream()
                    .map(match -> match.embedded().text())
                    .collect(Collectors.joining("\n\n"));
        } catch (Exception e) {
            log.error("知识库检索失败", e);
            return "知识库检索失败：" + e.getMessage();
        }
    }

    public record Source(String id,String title,String version,String indexedAt,String text) {}
    public List<Source> searchSources(String query) {
        if(!initialized) initKnowledge();
        if(!initialized) throw new IllegalStateException("知识库暂不可用");
        return embeddingStore.findRelevant(embeddingModel.embed(query).content(),3,0.6).stream()
            .filter(m -> m.embedded()!=null && m.embedded().metadata().getString("citation")!=null)
            .map(m -> {var t=m.embedded();return new Source(t.metadata().getString("citation"),t.metadata().getString("source"),t.metadata().getString("version"),t.metadata().getString("indexedAt"),t.text());}).toList();
    }

    public EmbeddingStore<TextSegment> getEmbeddingStore() {
        return embeddingStore;
    }

    public boolean isInitialized() {
        return initialized;
    }
}