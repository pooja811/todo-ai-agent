package com.example.todoai.service;

import com.example.todoai.model.Todo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RagService {

    private final ChatModel chatModel;
    private final VectorSearchService vectorSearchService;

    private static final String RAG_PROMPT_TEMPLATE = """
            You are TaskBot, an AI assistant for a todo list app.
            
            Answer the user's question using ONLY the todo tasks provided below.
            Do not make up tasks. If the context doesn't contain relevant tasks, say so.
            
            === RETRIEVED TASKS (context) ===
            %s
            =================================
            
            User Question: %s
            
            Answer:
            """;

    /**
     * RAG pipeline:
     * 1. Embed the user question
     * 2. Retrieve semantically similar todos from vector store
     * 3. Inject them into the prompt as context
     * 4. Let the LLM generate a grounded answer
     */
    public String askWithContext(String userQuestion) {
        log.debug("RAG query: {}", userQuestion);

        // Step 1 & 2 — Retrieve relevant todos
        List<VectorSearchService.ScoredTodo> retrieved =
                vectorSearchService.findSimilar(userQuestion, 5, 0.5);

        if (retrieved.isEmpty()) {
            return "I couldn't find any tasks related to your question.";
        }

        // Step 3 — Format retrieved todos as context
        String context = buildContext(retrieved);
        log.debug("RAG context:\n{}", context);

        // Step 4 — Build augmented prompt and call LLM
        String augmentedPrompt = RAG_PROMPT_TEMPLATE
                .formatted(context, userQuestion);

        String response = ChatClient.builder(chatModel)
                .build()
                .prompt()
                .user(augmentedPrompt)
                .call()
                .content();

        log.debug("RAG response: {}", response);
        return response;
    }

    /**
     * Formats retrieved todos into a readable context block
     * that gets injected into the prompt.
     */
    private String buildContext(List<VectorSearchService.ScoredTodo> results) {
        return results.stream()
                .map(st -> {
                    Todo t = st.todo();
                    return """
                            - ID: %d
                              Title: %s
                              Description: %s
                              Priority: %s | Category: %s | Status: %s
                              Similarity: %.0f%%
                            """.formatted(
                            t.getId(),
                            t.getTitle(),
                            t.getDescription() != null ? t.getDescription() : "N/A",
                            t.getPriority(),
                            t.getCategory(),
                            t.getStatus(),
                            st.score() * 100);
                })
                .collect(Collectors.joining("\n"));
    }
}