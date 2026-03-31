package com.iverson.aiagent.controller;


import com.iverson.aiagent.agent.multi.AiOrchestratorService;
import com.iverson.aiagent.controller.dto.AiRequest;
import com.iverson.aiagent.controller.dto.AiResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI接口控制层
 *
 * 面试重点：
 * 这是系统对外能力入口
 */
@RestController
@RequestMapping("/ai/agent")
public class ManyAgentController {
    private final AiOrchestratorService orchestratorService;

    public ManyAgentController(AiOrchestratorService orchestratorService) {
        this.orchestratorService = orchestratorService;
    }

    /**
     * 生成内容接口
     *
     * POST /api/ai/generate
     */
    @PostMapping("/generate")
    public AiResponse generate(@RequestBody AiRequest request) {

        String result = orchestratorService.run(request.getInput());

        return new AiResponse(result);
    }
}
