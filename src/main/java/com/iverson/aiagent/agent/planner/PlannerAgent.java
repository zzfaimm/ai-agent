package com.iverson.aiagent.agent.planner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iverson.aiagent.agent.base.BaseAgent;
import com.iverson.aiagent.agent.registry.AgentRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;
import org.springframework.ai.chat.model.ChatModel;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * PlannerAgent（核心大脑）
 *
 * 作用：
 * 1. 根据用户输入决定执行哪些Agent
 * 2. 动态生成任务流程（而不是写死）
 */
@Component
@Slf4j
public class PlannerAgent extends BaseAgent {

    private final AgentRegistry agentRegistry;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PlannerAgent(ChatModel dashscopeChatModel, AgentRegistry agentRegistry) {
        this.agentRegistry = agentRegistry;
        this.setChatClient(ChatClient.builder(dashscopeChatModel).build());
    }

    public Plan plan(String userInput) {
        String systemPrompt = buildDynamicPrompt();
        String userPrompt = "用户需求：" + userInput + "\n请生成任务计划。";

        String response = getChatClient().prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .content();
        log.info(response);
        return parsePlan(response);
    }

    private String buildDynamicPrompt() {
        String agentInfo = agentRegistry.getAgentDefinitions().stream()
                .map(def -> String.format("""
                - 名称: %s
                  描述: %s
                  能力详情: %s
                  输入: %s
                  输出: %s
                """, def.getName(), def.getDescription(), def.getDetail(),
                        String.join(", ", def.getRequiredInputs()),
                        String.join(", ", def.getOutputs())))
                .collect(Collectors.joining("\n"));

        return """
            你是一个任务规划专家。根据用户需求，选择合适的Agent并安排执行顺序。

            ## 可用Agent
            %s

            ## 规划规则
            - 如果某个Agent的输入依赖前一个Agent的输出，请在dependsOn中指明。
            - 如果任务可以并行执行，请标注parallelGroups。
            - 只返回JSON，不要有其他文字。

            ## 返回格式示例
            {
              "tasks": [
                {"name": "analysis", "dependsOn": []},
                {"name": "copywriting", "dependsOn": ["analysis"]}
              ]
            }

            """.formatted(agentInfo);
    }

    private Plan parsePlan(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            List<Task> tasks = new ArrayList<>();
            JsonNode tasksNode = root.get("tasks");
            if (tasksNode.isArray()) {
                for (JsonNode t : tasksNode) {
                    String name = t.get("name").asText();
                    List<String> dependsOn = new ArrayList<>();
                    JsonNode dependsNode = t.get("dependsOn");
                    if (dependsNode.isArray()) {
                        for (JsonNode d : dependsNode) {
                            dependsOn.add(d.asText());
                        }
                    }
                    tasks.add(new Task(name, dependsOn));
                }
            }
            // 可选：解析parallelGroups
            return new Plan(tasks);
        } catch (Exception e) {
            log.error("Failed to parse plan: {}", response, e);
            // 降级：返回默认顺序
            List<Task> defaultTasks = List.of(
                    new Task("analysis", List.of()),
                    new Task("copywriting", List.of("analysis")),
                    new Task("seo", List.of("copywriting"))
            );
            return new Plan(defaultTasks);
        }
    }

    @Override
    public String step() {
        return null;
    }
}