package com.ticket.ai.rag;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class KnowledgeIngestion {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeIngestion.class);

    private static final int MAX_SEGMENT_SIZE = 500;
    private static final int MAX_OVERLAP = 50;

    @Autowired
    private EmbeddingStore<TextSegment> embeddingStore;

    @Autowired
    private EmbeddingModel embeddingModel;

    public void ingestFromClasspath(String directory) {
        try {
            log.info("正在从 classpath:{} 加载知识文件...", directory);

            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:" + directory + "/*.txt");

            if (resources.length == 0) {
                log.warn("classpath:{} 目录下未找到任何知识文件", directory);
                return;
            }

            log.info("找到 {} 个知识文件", resources.length);
            List<Document> documents = new ArrayList<>();

            for (Resource resource : resources) {
                try {
                    String content = readResource(resource);
                    if (content != null && !content.isBlank()) {
                        documents.add(Document.from(content));
                        log.debug("已加载文件: {}", resource.getFilename());
                    }
                } catch (Exception e) {
                    log.warn("加载文件 {} 失败: {}", resource.getFilename(), e.getMessage());
                }
            }

            if (documents.isEmpty()) {
                log.warn("没有有效的知识内容");
                return;
            }

            DocumentSplitter splitter = DocumentSplitters.recursive(MAX_SEGMENT_SIZE, MAX_OVERLAP);
            EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                    .documentSplitter(splitter)
                    .embeddingModel(embeddingModel)
                    .embeddingStore(embeddingStore)
                    .build();

            ingestor.ingest(documents);


            log.info("知识库导入完成，共导入 {} 个文档", documents.size());
        } catch (Exception e) {
            log.error("知识导入失败", e);
        }
    }

    private String readResource(Resource resource) throws Exception {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }
}