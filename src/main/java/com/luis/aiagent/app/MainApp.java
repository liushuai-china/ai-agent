package com.luis.aiagent.app;

import com.luis.aiagent.advisor.MyLoggerAdvisor;
import com.luis.aiagent.chatmemory.FileBasedChatMemory;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.ResourceUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

@Component
@Slf4j
public class MainApp {
    private final ChatClient chatClient;


    @Resource
    private ChatModel dashScopeChatModel;

    @Resource
    private ResourceLoader resourceLoader;

    private static final String SYSTEM_PROMPT = "";

    record JavaReport(String title, List<String> suggestions){

    }

    public MainApp(){
        // 初始化基于文件的对话记忆
        String fileDir = System.getProperty("user.dir") + "/chat-memory";
        ChatMemory fileBasedChatMemory = new FileBasedChatMemory(fileDir);
        String prompt = ResourceUtils.getText(
                "classpath:prompt/java-system-prompt.txt"
        );
        //初始化基本内存的对话记忆
//        InMemoryChatMemory chatMemory = new InMemoryChatMemory();
        chatClient =  ChatClient.builder(dashScopeChatModel)
                .defaultSystem(prompt)
                .defaultAdvisors(
                        new MessageChatMemoryAdvisor(fileBasedChatMemory),
                        // 自定义日志 Advisor，可按需开启
                        new MyLoggerAdvisor()
                ).build();
    }

    /**
     * @param message 对话信息
     * @param chatId 对话ID，用于上下文记忆
     * @return
     */
    public String doChat(String message, String chatId) {
        ChatResponse response = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }


    public JavaReport doChatWithReport(String message, String chatId) {

        JavaReport javaReport = chatClient
                .prompt()
                .system(SYSTEM_PROMPT + "每次对话后都要生成面试报告，标题为{用户名}的面试报告，内容为建议列表")
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .call()
                .entity(JavaReport.class);
        log.info("javaReport: {}", javaReport);
        return javaReport;
    }


}
