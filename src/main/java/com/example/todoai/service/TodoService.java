package com.example.todoai.service;

import com.example.todoai.model.Todo;
import com.example.todoai.repository.TodoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TodoService {

    private final TodoRepository todoRepository;
    @Autowired
    private EmbeddingService embeddingService;

    // ── CRUD ────────────────────────────────────────────────────────────

    public Todo createTodo(String title, String description,
                           Todo.Priority priority, Todo.Category category) {
        Todo todo = Todo.builder()
                .title(title)
                .description(description)
                .priority(priority != null ? priority : Todo.Priority.MEDIUM)
                .category(category != null ? category : Todo.Category.OTHER)
                .status(Todo.Status.PENDING)
             //  .embedding(embeddingService.getEmbedding(title + " " + description))
                .build();
        Todo saved = todoRepository.save(todo);
        log.debug("Created todo [id={}]: {}", saved.getId(), saved.getTitle());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Todo> getAllTodos() {
        return todoRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Todo> getTodoById(Long id) {
        return todoRepository.findById(id);
    }

    public Todo updateStatus(Long id, Todo.Status status) {
        Todo todo = findOrThrow(id);
        todo.setStatus(status);
        log.debug("Updated todo [id={}] status → {}", id, status);
        return todoRepository.save(todo);
    }

    public Todo updatePriority(Long id, Todo.Priority priority) {
        Todo todo = findOrThrow(id);
        todo.setPriority(priority);
        log.debug("Updated todo [id={}] priority → {}", id, priority);
        return todoRepository.save(todo);
    }

    public Todo updateTodo(Long id, String title, String description,
                           Todo.Priority priority, Todo.Category category) {
        Todo todo = findOrThrow(id);
        if (title != null && !title.isBlank())        todo.setTitle(title);
        if (description != null)                      todo.setDescription(description);
        if (priority != null)                         todo.setPriority(priority);
        if (category != null)                         todo.setCategory(category);
        return todoRepository.save(todo);
    }

    public void deleteTodo(Long id) {
        if (!todoRepository.existsById(id)) {
            throw new IllegalArgumentException("Todo not found: " + id);
        }
        todoRepository.deleteById(id);
        log.debug("Deleted todo [id={}]", id);
    }

    public int clearCompleted() {
        List<Todo> completed = todoRepository.findByStatus(Todo.Status.COMPLETED);
        todoRepository.deleteAll(completed);
        log.debug("Cleared {} completed todos", completed.size());
        return completed.size();
    }

    // ── Queries ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Todo> getByStatus(Todo.Status status) {
        return todoRepository.findByStatus(status);
    }

    @Transactional(readOnly = true)
    public List<Todo> getByPriority(Todo.Priority priority) {
        return todoRepository.findByPriority(priority);
    }

    @Transactional(readOnly = true)
    public List<Todo> getByCategory(Todo.Category category) {
        return todoRepository.findByCategory(category);
    }

    // Semantic search: embed the user's query, find similar todos
    public List<Todo> semanticSearch(String userQuery, int topK) {
        float[] queryEmbedding = embeddingService.getEmbedding(userQuery);

        // pgvector expects a string like "[0.1, 0.2, ...]"
        String vectorStr = Arrays.toString(queryEmbedding);
        return todoRepository.findSimilar(vectorStr, topK);
    }

    // Agent calls this instead of the old keyword search
    public List<Todo>  searchTodos(String userMessage) {
        // Embed the raw user message — no keyword extraction needed!
        log.info("Searching with keyword: {}", userMessage);
        // List<Todo> results = todoRepository.searchByKeyword(keyword);
        List<Todo> results = semanticSearch(userMessage, 5);
        log.info("Found: {} results", results.size());
        return results;
    }

    @Transactional(readOnly = true)
    public List<Todo> getActiveSortedByPriority() {
        return todoRepository.findActiveSortedByPriority();
    }

    @Transactional(readOnly = true)
    public TodoStats getStats() {
        long total     = todoRepository.count();
        long pending   = todoRepository.countByStatus(Todo.Status.PENDING);
        long inProg    = todoRepository.countByStatus(Todo.Status.IN_PROGRESS);
        long completed = todoRepository.countByStatus(Todo.Status.COMPLETED);
        long urgent    = todoRepository.countUrgentPending();
        return new TodoStats(total, pending, inProg, completed, urgent);
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private Todo findOrThrow(Long id) {
        return todoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Todo not found: " + id));
    }

    // ── Stats Record ────────────────────────────────────────────────────

    public record TodoStats(long total, long pending, long inProgress,
                            long completed, long urgentPending) {}
}
