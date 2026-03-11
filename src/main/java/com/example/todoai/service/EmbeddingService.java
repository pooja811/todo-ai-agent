package com.example.todoai.service;

import com.example.todoai.model.Todo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class EmbeddingService {

    private final VectorStore vectorStore;
    private final EmbeddingModel embeddingModel;

    public EmbeddingService(VectorStore vectorStore, EmbeddingModel embeddingModel) {
        this.vectorStore = vectorStore;
        this.embeddingModel = embeddingModel;
    }

    // ── Embedding Text Builder ───────────────────────────────────────────

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


    // Stores a todo in pgvector via Spring AI's VectorStore
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
        log.debug("Embedded todo [id={}] → vectorId=[{}]", todo.getId(), doc.getId());
        return doc.getId();
    }

    // Deletes the vector store document for the given vector ID.
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