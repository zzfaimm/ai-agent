package com.iverson.aiagent.agent;


import com.iverson.aiagent.agent.impl.MyManus;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @ClassName: MyManusTest
 * @Description: TODO(描述这个类的作用)
 * @Author: zhuze
 * @Date: 3/26/2026 10:35 PM
 * @Version: 1.0
 */
@SpringBootTest
class MyManusTest {


    @Resource
    private MyManus myManus;

    @Test
    void run() {
        String userPrompt = """  
            帮我写一个电商文案，关于卖打火机的，需要配图，文案和图片需要保存到PDF和本地，文件需要是中文""";
        String answer = myManus.run(userPrompt);
        Assertions.assertNotNull(answer);
    }



}