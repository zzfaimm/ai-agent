package com.iverson.aiagent.tool;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @ClassName: FileOperationToolTest
 * @Description: TODO(描述这个类的作用)
 * @Author: zhuze
 * @Date: 3/24/2026 5:40 PM
 * @Version: 1.0
 */
@SpringBootTest
class FileOperationToolTest {

    @Test
    void readFile() {
        FileOperationTool fileOperationTool = new FileOperationTool();
        String fileName = "AI大模型应用.txt";
        String result = fileOperationTool.readFile(fileName);
        System.out.println(result.toString());
        Assertions.assertNotNull(result);
    }

    @Test
    void writeFile() {

        FileOperationTool fileOperationTool = new FileOperationTool();
        String fileName = "AI大模型应用.txt";
        String content = "这个文档是关于AI大模型应用开发的知识点";
        String result = fileOperationTool.writeFile(fileName, content);
        Assertions.assertNotNull(result);
    }
}