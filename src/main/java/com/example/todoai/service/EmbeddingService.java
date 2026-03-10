package com.example.todoai.service;

import com.example.todoai.model.Todo;
import com.theokanning.openai.embedding.EmbeddingRequest;
import com.theokanning.openai.service.OpenAiService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;

import java.util.List;
import java.util.Map;
@Slf4j
@Service
public class EmbeddingService {

    @Value("${OPENAI_API_KEY}")
    private String openAiKey;

    private OpenAiService openAiService;

    public EmbeddingService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @PostConstruct
    public void init() {
        this.openAiService = new OpenAiService(openAiKey);
    }

    public float[] getEmbedding(String text) {
        EmbeddingRequest request = EmbeddingRequest.builder()
                .model("text-embedding-ada-002")
                .input(List.of(text))
                .build();

        List<Double> vector = openAiService
                .createEmbeddings(request)
                .getData()
                .getFirst()
                .getEmbedding();

        // Convert Double[] to float[]
        float[] result = new float[vector.size()];
        for (int i = 0; i < vector.size(); i++) {
            result[i] = vector.get(i).floatValue();
        }
        return result;
    }

    private final VectorStore vectorStore;

    // ── Embedding text builder ───────────────────────────────────────────

    /**
     * Builds the text that will be embedded for a given todo.
     * Combines title + description for richer semantic representation.
     */
    public String buildEmbeddingText(Todo todo) {
        StringBuilder sb = new StringBuilder();
        sb.append("Task: ").append(todo.getTitle());
        if (todo.getDescription() != null && !todo.getDescription().isBlank()) {
            sb.append(". Details: ").append(todo.getDescription());
        }
        sb.append(". Priority: ").append(todo.getPriority());
        sb.append(". Category: ").append(todo.getCategory());
        sb.append(". Status: ").append(todo.getStatus());
        return sb.toString();
    }

    // ── Store / Update ───────────────────────────────────────────────────

    /**
     * Creates a new embedding document for the todo and stores it in the vector store.
     *
     * @return the generated document ID (to be saved back on the Todo entity)
     */
    public String embedTodo(Todo todo) {
        String text = buildEmbeddingText(todo);

        Document doc = new Document(
                text,
                Map.of(
                        "todo_id",  todo.getId(),
                        "priority", todo.getPriority().name(),
                        "category", todo.getCategory().name(),
                        "status",   todo.getStatus().name()
                )
        );

        vectorStore.add(List.of(doc));
        log.debug("Embedded todo [id={}] as vector doc [vectorId={}]", todo.getId(), doc.getId());
        return doc.getId();
    }

    /**
     * Replaces an existing embedding when the todo is updated.
     * Deletes the old vector document (if any) and creates a fresh one.
     *
     * @return the new document ID
     */
    public String reEmbedTodo(Todo todo) {
        // Remove old embedding if it exists
        if (todo.getVectorId() != null) {
            deleteTodoEmbedding(todo.getVectorId());
        }
        return embedTodo(todo);
    }

    /**
     * Deletes the vector store document for the given vector ID.
     */
    public void deleteTodoEmbedding(String vectorId) {
        if (vectorId == null) return;
        try {
            vectorStore.delete(List.of(vectorId));
            log.debug("Deleted vector embedding [vectorId={}]", vectorId);
        } catch (Exception e) {
            log.warn("Could not delete vector embedding [vectorId={}]: {}", vectorId, e.getMessage());
        }
    }

    // ── Semantic Search ──────────────────────────────────────────────────

    /**
     * Performs a similarity search against all stored todo embeddings.
     *
     * @param query      natural language query
     * @param topK       maximum number of results to return
     * @param threshold  minimum similarity score (0.0 – 1.0)
     * @return list of matching {@link Document}s sorted by relevance
     */
    public List<Document> semanticSearch(String query, int topK, double threshold) {
        log.debug("Semantic search: query='{}', topK={}, threshold={}", query, topK, threshold);

        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .similarityThreshold(threshold)
                .build();

        List<Document> results = vectorStore.similaritySearch(request);
        log.debug("Semantic search returned {} results", results.size());
        return results;
    }

    /**
     * Performs a semantic search filtered by a specific status.
     *
     * @param query  natural language query
     * @param status only return embeddings matching this status
     * @param topK   max results
     */
    public List<Document> semanticSearchByStatus(String query, String status, int topK) {
        FilterExpressionBuilder b = new FilterExpressionBuilder();

        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .similarityThreshold(0.5)
                .filterExpression(b.eq("status", status).build())
                .build();

        return vectorStore.similaritySearch(request);
    }

    /**
     * Performs a semantic search filtered by priority.
     *
     * @param query    natural language query
     * @param priority HIGH / MEDIUM / LOW
     * @param topK     max results
     */
    public List<Document> semanticSearchByPriority(String query, String priority, int topK) {
        FilterExpressionBuilder b = new FilterExpressionBuilder();

        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .similarityThreshold(0.5)
                .filterExpression(b.eq("priority", priority).build())
                .build();

        return vectorStore.similaritySearch(request);
    }
}