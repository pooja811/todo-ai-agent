package com.example.todoai.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class AgentLoopService {

    private final ChatClient chatClient;

    public AgentLoopService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public String runAgent(String goal) {

        int maxSteps = 5;
        String context = "";

        for (int step = 1; step <= maxSteps; step++) {

            String prompt = """
            You are an AI agent.
            Goal: %s
            Context: %s

            Decide next action or say DONE.
            """.formatted(goal, context);

            String decision = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            if (decision.contains("DONE")) {
                return context;
            }

            context += "\nStep " + step + ": " + decision;
        }

        return "Agent stopped due to step limit.";
    }
}
