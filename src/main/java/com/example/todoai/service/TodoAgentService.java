package com.example.todoai.service;

import com.example.todoai.config.InMemoryChatMemory;
import com.example.todoai.tool.TodoTools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Core AI Agent service.
 * Uses Spring AI's ChatClient with:
 *  - A system prompt that defines the agent's persona
 *  - Tool calling (function calling) via TodoTools
 *  - In-memory conversation history per session
 */
@Service
@Slf4j
public class TodoAgentService {

    private static final String SYSTEM_PROMPT = """
            You are TaskBot, a friendly and efficient AI Todo List Agent.
            
            Your personality:
            - Proactive: Suggest related tasks or improvements when relevant
            - Concise: Give clear, actionable responses — not too long
            - Smart: Infer priorities and categories from context when not specified
            - Encouraging: Celebrate completed tasks and progress
            
            ALWAYS call the searchTodos tool BEFORE responding about any task,
            regardless of whether the user is asking a question or making a statement.
            If the user mentions completing, updating, or referencing ANY activity,
            search for it first using relevant keywords extracted from their message.
            
            Your capabilities:
            - Create, update, delete, and list todo tasks
            - Mark tasks as pending, in-progress, or completed
            - Search tasks by keyword
            - Filter by priority (HIGH/MEDIUM/LOW) or category
            - Show statistics and progress reports
            - Provide focus recommendations
            
            Rules:
            - Always use the available tools to interact with the task database
            - If asked about a specific task by name (not ID), search for it first
            - When creating tasks, infer the best priority and category from context
            - Format your text responses clearly — avoid excessive emoji spam
            - If the user seems overwhelmed, offer to prioritize or organize tasks
            - Never make up task data — always fetch from tools
            
            Today's capabilities:
            - Tasks can be PENDING, IN_PROGRESS, or COMPLETED
            - Priorities: HIGH (urgent), MEDIUM (normal), LOW (nice-to-have)
            - Categories: WORK, PERSONAL, HEALTH, SHOPPING, LEARNING, OTHER
            """;

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;


    public TodoAgentService(ChatModel chatModel, TodoTools todoTools) {
        this.chatMemory = new InMemoryChatMemory();
        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultTools(todoTools)                 // Register all @Tool methods
                .build();
    }

    /**
     * Process a user message and return the agent's response.
     *
     * @param conversationId unique session identifier for chat memory
     * @param userMessage    the user's input
     * @return the agent's text response
     */
    public String chat(String conversationId, String userMessage) {
        log.debug("Agent received [session={}]: {}", conversationId, userMessage);

        String response = chatClient.prompt()
                .user(userMessage)
                .advisors(advisor -> advisor.param(
                        ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();

        log.debug("Agent responded [session={}]: {}", conversationId, response);
        return response;
    }

    /**
     * Clear the conversation memory for a given session.
     */
    public void clearHistory(String conversationId) {
        chatMemory.clear(conversationId);
        log.debug("Cleared chat memory for session: {}", conversationId);
    }
}
