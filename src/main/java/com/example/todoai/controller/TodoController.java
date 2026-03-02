package com.example.todoai.controller;

import com.example.todoai.model.Todo;
import com.example.todoai.service.TodoAgentService;
import com.example.todoai.service.TodoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

// ══════════════════════════════════════════════════════════════════
// AI Agent Chat Controller  — POST /api/agent/chat
// ══════════════════════════════════════════════════════════════════

@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
class AgentController {

    private final TodoAgentService agentService;

    /**
     * Main chat endpoint. Send a message to the AI agent and receive a response.
     * Include a sessionId to maintain conversation context.
     */
    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        String sessionId = request.sessionId() != null
                ? request.sessionId()
                : UUID.randomUUID().toString();

        String response = agentService.chat(sessionId, request.message());
        return ResponseEntity.ok(new ChatResponse(sessionId, response));
    }

    /**
     * Clear conversation history for a session.
     */
    @DeleteMapping("/chat/{sessionId}")
    public ResponseEntity<Map<String, String>> clearHistory(@PathVariable String sessionId) {
        agentService.clearHistory(sessionId);
        return ResponseEntity.ok(Map.of(
                "message", "Conversation history cleared",
                "sessionId", sessionId));
    }

    // ── DTOs ────────────────────────────────────────────────────────────

    record ChatRequest(
            @NotBlank(message = "Message cannot be blank") String message,
            String sessionId
    ) {}

    record ChatResponse(String sessionId, String reply) {}
}

// ══════════════════════════════════════════════════════════════════
// Todo CRUD Controller  — /api/todos
// ══════════════════════════════════════════════════════════════════

@RestController
@RequestMapping("/api/todos")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
class TodoController {

    private final TodoService todoService;

    @GetMapping
    public List<Todo> getAll(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search) {

        if (search != null)   return todoService.searchTodos(search);
        if (status != null)   return todoService.getByStatus(Todo.Status.valueOf(status.toUpperCase()));
        if (priority != null) return todoService.getByPriority(Todo.Priority.valueOf(priority.toUpperCase()));
        if (category != null) return todoService.getByCategory(Todo.Category.valueOf(category.toUpperCase()));
        return todoService.getAllTodos();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Todo> getById(@PathVariable Long id) {
        return todoService.getTodoById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Todo create(@Valid @RequestBody CreateTodoRequest req) {
        return todoService.createTodo(req.title(), req.description(), req.priority(), req.category());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Todo> update(@PathVariable Long id,
                                       @RequestBody UpdateTodoRequest req) {
        try {
            return ResponseEntity.ok(todoService.updateTodo(
                    id, req.title(), req.description(), req.priority(), req.category()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Todo> updateStatus(@PathVariable Long id,
                                             @RequestBody Map<String, String> body) {
        try {
            Todo.Status status = Todo.Status.valueOf(body.get("status").toUpperCase());
            return ResponseEntity.ok(todoService.updateStatus(id, status));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            todoService.deleteTodo(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/completed")
    public ResponseEntity<Map<String, Integer>> clearCompleted() {
        int count = todoService.clearCompleted();
        return ResponseEntity.ok(Map.of("deleted", count));
    }

    @GetMapping("/stats")
    public TodoService.TodoStats stats() {
        return todoService.getStats();
    }

    // ── DTOs ────────────────────────────────────────────────────────────

    record CreateTodoRequest(
            @NotBlank String title,
            String description,
            Todo.Priority priority,
            Todo.Category category
    ) {}

    record UpdateTodoRequest(
            String title,
            String description,
            Todo.Priority priority,
            Todo.Category category
    ) {}
}
