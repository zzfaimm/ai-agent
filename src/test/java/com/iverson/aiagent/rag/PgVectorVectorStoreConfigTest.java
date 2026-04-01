package com.iverson.aiagent.rag;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;

import java.util.List;

/**
 * @ClassName: PgVectorVectorStoreConfigTest
 * @Description: TODO(描述这个类的作用)
 * @Author: zhuze
 * @Date: 3/24/2026 7:32 PM
 * @Version: 1.0
 */
@SpringBootTest
@Rollback(false)
public class PgVectorVectorStoreConfigTest {

    @Resource
    VectorStore pgVectorVectorStore;

    @Test
    void test() {
        List<Document> results = pgVectorVectorStore.similaritySearch(SearchRequest.builder().query("如何恋爱").topK(5).build());
        Assertions.assertNotNull(results);
    }
}
