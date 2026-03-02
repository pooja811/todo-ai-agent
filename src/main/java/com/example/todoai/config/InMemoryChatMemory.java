package com.example.todoai.config;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryChatMemory implements ChatMemory {

    private final Map<String, List<Message>> store = new ConcurrentHashMap<>();

    @Override
    public void add(String conversationId, List<Message> messages) {
        store.computeIfAbsent(conversationId, id -> new ArrayList<>())
                .addAll(messages);
    }

    @Override
    public List<Message> get(String conversationId) {
        return store.getOrDefault(conversationId, List.of());
    }

    @Override
    public void clear(String conversationId) {
        store.remove(conversationId);
    }
}