package com.venkat.agent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class AgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentApplication.class, args);
    }

    @Bean
    ChatMemory chatMemory() {
       System.out.println("== In chatMemory () ==");
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(20)
                .build();
    }

    @Bean
    ChatClient chatClient(ChatClient.Builder builder, ChatMemory chatMemory) {
        System.out.println("== In chatClient () ==");
        return builder
                .defaultSystem("""
                        You solve problems step by step using the available tools.
                        Call a tool when you need a calculation, a word count, or the current time.
                        When you have enough information, answer directly and concisely.
                        """)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        new SimpleLoggerAdvisor(),new GroundingVerificationAdvisor())
                .build();
    }

    @Bean
    ApplicationRunner runner(ChatClient chatClient,
            CalculatorTool calculator, WordCountTool wordCount, CurrentTimeTool currentTime) {
                System.out.println("== In runner () ==");
        return args -> {
            String[] questions = args.getSourceArgs();
            if (questions.length == 0) questions = new String[] { "What time is it?" };
            String conversationId = System.getenv().getOrDefault("CONVERSATION_ID", "user-1");
            int turn = 1;
    for (String q : questions) {
        System.out.println("\n=== TURN " + (turn++) + ": " + q + " ===");
            String answer = chatClient.prompt()
                    .user(q)
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                    .tools(calculator, wordCount,currentTime)
                    .call()
                    .content();
            System.out.println(answer);
        }
    }; 
  }
}
