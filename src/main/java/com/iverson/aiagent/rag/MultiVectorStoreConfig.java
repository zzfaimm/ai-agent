//package com.iverson.aiagent.rag;
//
//import org.springframework.ai.embedding.EmbeddingModel;
//import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.jdbc.core.JdbcTemplate;
//
//@Configuration
//public class MultiVectorStoreConfig {
//
//    @Bean
//    @Qualifier("dashscopeVectorStore")
//    public PgVectorStore dashscopeVectorStore(
//            @Qualifier("dashscopeEmbeddingModel") EmbeddingModel embeddingModel,
//            JdbcTemplate jdbcTemplate) {
//        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
//                .initializeSchema(true)
//                .build();
//    }
//
//    @Bean
//    @Qualifier("ollamaVectorStore")
//    public PgVectorStore ollamaVectorStore(
//            @Qualifier("ollamaEmbeddingModel") EmbeddingModel embeddingModel,
//            JdbcTemplate jdbcTemplate) {
//        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
//                .initializeSchema(true)
//                .build();
//    }
//}