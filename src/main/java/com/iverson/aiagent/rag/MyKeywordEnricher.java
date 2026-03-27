package com.iverson.aiagent.rag;

import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.KeywordMetadataEnricher;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @ClassName: MyKeywordEnricher
 * @Description: TODO(描述这个类的作用)
 * @Author: zhuze
 * @Date: 3/24/2026 8:24 PM
 * @Version: 1.0
 */
@Component
class MyKeywordEnricher {
    @Resource
    private ChatModel dashscopeChatModel;

    List<Document> enrichDocuments(List<Document> documents) {
        KeywordMetadataEnricher enricher = new KeywordMetadataEnricher(this.dashscopeChatModel, 5);
        return enricher.apply(documents);
    }
}

//    @Bean
//    VectorStore loveAppVectorStore(EmbeddingModel dashscopeEmbeddingModel) {
//        SimpleVectorStore simpleVectorStore = SimpleVectorStore.builder(dashscopeEmbeddingModel)
//                .build();
//        // 加载文档
//        List<Document> documents = loveAppDocumentLoader.loadMarkdowns();
//        // 自动补充关键词元信息
//        List<Document> enrichedDocuments = myKeywordEnricher.enrichDocuments(documents);
//        simpleVectorStore.add(enrichedDocuments);
//        return simpleVectorStore;
//    }

