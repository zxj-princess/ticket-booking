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

    @org.springframework.beans.factory.annotation.Value("${ai.vector-store.type:milvus}") private String vectorStoreType;
    @Autowired private org.springframework.jdbc.core.JdbcTemplate db;
    @Autowired private com.ticket.booking.BookingSchema schema;

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
                log.warn("classpath:{} 目录下未找到任何知识文件，将清理本导入器登记的旧片段", directory);
            }

            log.info("找到 {} 个知识文件", resources.length);
            List<Document> documents = new ArrayList<>();

            for (Resource resource : resources) {
                try {
                    String content = readResource(resource);
                    if (content != null && !content.isBlank()) {
                        documents.add(Document.from(content, dev.langchain4j.data.document.Metadata.from(java.util.Map.of("source",resource.getFilename(), "version",java.util.UUID.nameUUIDFromBytes(content.getBytes(StandardCharsets.UTF_8)).toString(), "indexedAt",java.time.LocalDate.now().toString()))));
                        log.debug("已加载文件: {}", resource.getFilename());
                    }
                } catch (Exception e) {
                    throw new IllegalStateException("无法读取知识文档："+resource.getFilename(),e);
                }
            }

            if (documents.isEmpty()) {
                log.warn("没有有效的知识内容");
            }

            DocumentSplitter splitter = DocumentSplitters.recursive(MAX_SEGMENT_SIZE, MAX_OVERLAP);
            java.util.Set<String> current = new java.util.HashSet<>();
            for(Document document:documents) {
                int index=0;
                for(TextSegment segment:splitter.split(document)) {
                    String id=java.util.UUID.nameUUIDFromBytes((document.metadata().getString("source")+":"+(index++)+":"+document.metadata().getString("version")).getBytes(StandardCharsets.UTF_8)).toString();
                    segment.metadata().put("citation", "S"+id.replace("-", ""));
                    current.add(id);
                    if("memory".equalsIgnoreCase(vectorStoreType) || db.queryForObject("SELECT COUNT(*) FROM ticket_knowledge_segment WHERE id=?",Integer.class,id)==0) {
                        embeddingStore.addAll(java.util.List.of(id),java.util.List.of(embeddingModel.embed(segment).content()),java.util.List.of(segment));
                        db.update("INSERT IGNORE INTO ticket_knowledge_segment(id) VALUES(?)",id);
                    }
                }
            }
            for(String old:db.queryForList("SELECT id FROM ticket_knowledge_segment",String.class)) {
                if(!current.contains(old)) {embeddingStore.remove(old);db.update("DELETE FROM ticket_knowledge_segment WHERE id=?",old);}
            }

            log.info("知识库导入完成，共导入 {} 个文档", documents.size());
        } catch (Exception e) {
            throw new IllegalStateException("知识导入失败", e);
        }
    }

    private String readResource(Resource resource) throws Exception {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }
}