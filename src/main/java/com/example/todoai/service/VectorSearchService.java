package com.example.todoai.service;

import com.example.todoai.model.Todo;
import com.example.todoai.repository.TodoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * High-level semantic search service that bridges the {@link EmbeddingService}
 * (vector layer) with the {@link TodoRepository} (relational layer).
 *
 * <p>After a similarity search, results are enriched by fetching the full
 * {@link Todo} entity from Postgres using the {@code todo_id} metadata field
 * stored in each vector document.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VectorSearchService {

    private final EmbeddingService embeddingService;
    private final TodoRepository   todoRepository;

    // ── Public API ───────────────────────────────────────────────────────

    /**
     * Find todos semantically similar to the given query.
     *
     * @param query     natural language description of what to find
     * @param topK      max number of results
     * @param threshold similarity threshold (0.0–1.0); 0.6 is a good default
     * @return list of matching {@link ScoredTodo}s ordered by similarity
     */
    public List<ScoredTodo> findSimilar(String query, int topK, double threshold) {
        List<Document> docs = embeddingService.semanticSearch(query, topK, threshold);
        return enrichDocuments(docs);
    }

    /**
     * Find semantically similar todos that are still active (PENDING or IN_PROGRESS).
     */
    public List<ScoredTodo> findSimilarActive(String query, int topK) {
        // Search across all, then filter by status in memory (avoids complex OR filter)
        List<Document> docs = embeddingService.semanticSearch(query, topK * 2, 0.5);
        return enrichDocuments(docs).stream()
                .filter(st -> st.todo().getStatus() != Todo.Status.COMPLETED)
                .limit(topK)
                .toList();
    }

    /**
     * Find semantically similar todos filtered by a specific status.
     */
    public List<ScoredTodo> findSimilarByStatus(String query, Todo.Status status, int topK) {
        List<Document> docs = embeddingService.semanticSearchByStatus(
                query, status.name(), topK);
        return enrichDocuments(docs);
    }

    /**
     * Find semantically similar todos filtered by priority.
     */
    public List<ScoredTodo> findSimilarByPriority(String query, Todo.Priority priority, int topK) {
        List<Document> docs = embeddingService.semanticSearchByPriority(
                query, priority.name(), topK);
        return enrichDocuments(docs);
    }

    // ── Internal helpers ─────────────────────────────────────────────────

    /**
     * Converts a list of raw vector {@link Document}s into {@link ScoredTodo}s
     * by looking up the full {@link Todo} entity from the DB.
     */
    private List<ScoredTodo> enrichDocuments(List<Document> docs) {
        List<ScoredTodo> results = new ArrayList<>();

        for (Document doc : docs) {
            Object todoIdObj = doc.getMetadata().get("todo_id");
            if (todoIdObj == null) {
                log.warn("Vector doc [{}] is missing todo_id metadata — skipping", doc.getId());
                continue;
            }

            Long todoId = toLong(todoIdObj);
            Optional<Todo> todoOpt = todoRepository.findById(todoId);

            if (todoOpt.isEmpty()) {
                log.warn("Vector doc points to non-existent todo [id={}] — stale embedding", todoId);
                continue;
            }

            double score = doc.getScore() != null ? doc.getScore() : 0.0;
            results.add(new ScoredTodo(todoOpt.get(), score, doc.getText()));
        }

        return results;
    }

    private Long toLong(Object value) {
        if (value instanceof Long l)    return l;
        if (value instanceof Integer i) return i.longValue();
        if (value instanceof Number n)  return n.longValue();
        return Long.parseLong(value.toString());
    }

    // ── Result record ────────────────────────────────────────────────────

    /**
     * A todo enriched with its similarity score from the vector search.
     *
     * @param todo          the full {@link Todo} entity
     * @param score         cosine similarity score (0.0 – 1.0, higher = more similar)
     * @param embeddedText  the text that was originally embedded
     */
    public record ScoredTodo(Todo todo, double score, String embeddedText) {}
}
