package com.iverson.aiagent.agent.simpleagent.base;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;

/**
 * @ClassName: ReActAgent
 * @Description: TODO(描述这个类的作用)
 * @Author: zhuze
 * @Date: 3/26/2026 6:13 PM
 * @Version: 1.0
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class SimpleReActAgent extends SimpleBaseAgent {
    /**
     *处理当前状态并决定下一步行动
     * @param
     * @return 是否需要执行行动，true表示需要执行，false表示不需要执行
     */
    public abstract boolean think();

    /**
     *执行决定要执行的行动
     * @param
     * @return 行动执行的结果
     */
    public abstract String act();


    /**
     * 执行单个步骤：思考和行动
     * @param
     * @return 步骤执行结果
     */

    @Override
    public String step(){
        try {
            boolean shouldAct = think();
            if (!shouldAct){
                log.info("思考完成 - 无需行动");
                AssistantMessage assistantMessage = (AssistantMessage)getMessageList().getLast();
                return assistantMessage.getText();
            }
            return act();
        } catch (Exception e) {
            // 记录异常日志
            e.printStackTrace();
            return "步骤执行失败 ： " + e.getMessage();
        }
    }

}
