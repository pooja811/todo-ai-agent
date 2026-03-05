package com.example.todoai.tool;

import com.example.todoai.model.Todo;
import com.example.todoai.service.TodoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Spring AI Tool definitions exposed to the LLM.
 * Each method annotated with @Tool becomes a callable function
 * that openAI can invoke during a conversation.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TodoTools {

    private final TodoService todoService;

    // ── Create ──────────────────────────────────────────────────────────

    @Tool(description = """
            Create a new todo task. Use this when the user wants to add, create, or schedule a task.
            Priority should be HIGH for urgent items, MEDIUM for normal tasks, LOW for nice-to-haves.
            Category options: WORK, PERSONAL, HEALTH, SHOPPING, LEARNING, OTHER.
            """)
    public String createTodo(
            @ToolParam(description = "Clear, concise task title") String title,
            @ToolParam(description = "Optional detailed description of the task") String description,
            @ToolParam(description = "Priority: HIGH, MEDIUM, or LOW") String priority,
            @ToolParam(description = "Category: WORK, PERSONAL, HEALTH, SHOPPING, LEARNING, or OTHER") String category) {

        try {
            Todo.Priority p = parsePriority(priority);
            Todo.Category c = parseCategory(category);
            Todo todo = todoService.createTodo(title, description, p, c);
            return "✅ Task created! [ID: %d] \"%s\" — %s priority, %s category"
                    .formatted(todo.getId(), todo.getTitle(), todo.getPriority(), todo.getCategory());
        } catch (Exception e) {
            log.error("Error creating todo", e);
            return "❌ Failed to create task: " + e.getMessage();
        }
    }

    // ── List ────────────────────────────────────────────────────────────

    @Tool(description = """
            List all todos. Returns a formatted list of all tasks.
            Use when the user asks to see, show, list, or display their tasks.
            """)
    public String listAllTodos() {
        List<Todo> todos = todoService.getAllTodos();
        if (todos.isEmpty()) return "📭 No tasks found. Your list is empty!";
        return formatTodoList("📋 All Tasks", todos);
    }

    @Tool(description = """
            List todos filtered by status.
            Status options: PENDING (not started), IN_PROGRESS (being worked on), COMPLETED (done).
            Use when the user asks for active, pending, in-progress, or completed tasks.
            """)
    public String listTodosByStatus(
            @ToolParam(description = "Status filter: PENDING, IN_PROGRESS, or COMPLETED") String status) {
        try {
            Todo.Status s = Todo.Status.valueOf(status.toUpperCase().replace(" ", "_"));
            List<Todo> todos = todoService.getByStatus(s);
            if (todos.isEmpty()) return "📭 No %s tasks found.".formatted(status);
            return formatTodoList("📋 %s Tasks".formatted(capitalize(status)), todos);
        } catch (IllegalArgumentException e) {
            return "❌ Invalid status. Use: PENDING, IN_PROGRESS, or COMPLETED";
        }
    }

    @Tool(description = """
            List todos by priority level. Shows HIGH, MEDIUM, or LOW priority tasks.
            Use when the user asks about urgent, high-priority, or low-priority tasks.
            """)
    public String listTodosByPriority(
            @ToolParam(description = "Priority: HIGH, MEDIUM, or LOW") String priority) {
        try {
            Todo.Priority p = parsePriority(priority);
            List<Todo> todos = todoService.getByPriority(p);
            if (todos.isEmpty()) return "📭 No %s priority tasks found.".formatted(priority);
            return formatTodoList("🎯 %s Priority Tasks".formatted(capitalize(priority)), todos);
        } catch (IllegalArgumentException e) {
            return "❌ Invalid priority. Use: HIGH, MEDIUM, or LOW";
        }
    }

    @Tool(description = """
            Get active (non-completed) tasks sorted by priority — HIGH first.
            Use when user asks 'what should I focus on', 'what's most important', or wants a prioritized view.
            """)
    public String getTopPriorityTasks() {
        List<Todo> todos = todoService.getActiveSortedByPriority();
        if (todos.isEmpty()) return "🎉 Nothing pending! All tasks are completed.";
        return formatTodoList("🔥 Active Tasks by Priority", todos);
    }

    // ── Update ──────────────────────────────────────────────────────────

    @Tool(description = """
            Mark a todo as completed. Use when user says 'done', 'finish', 'complete', 'mark as done'.
            Requires the task ID — if unknown, call listAllTodos first to find it.
            """)
    public String completeTodo(
            @ToolParam(description = "The numeric ID of the task to mark as completed") Long id) {
        try {
            Todo todo = todoService.updateStatus(id, Todo.Status.COMPLETED);
            return "✅ Marked as completed: \"%s\"".formatted(todo.getTitle());
        } catch (IllegalArgumentException e) {
            return "❌ Task not found with ID: " + id;
        }
    }

    @Tool(description = """
            Mark a todo as in-progress. Use when user says 'start', 'working on', 'begin'.
            """)
    public String startTodo(
            @ToolParam(description = "The numeric ID of the task to start") Long id) {
        try {
            Todo todo = todoService.updateStatus(id, Todo.Status.IN_PROGRESS);
            return "🚀 Started: \"%s\" is now In Progress!".formatted(todo.getTitle());
        } catch (IllegalArgumentException e) {
            return "❌ Task not found with ID: " + id;
        }
    }

    @Tool(description = """
            Change the priority of an existing todo task.
            Use when user wants to make a task more or less urgent.
            """)
    public String updatePriority(
            @ToolParam(description = "The numeric ID of the task") Long id,
            @ToolParam(description = "New priority: HIGH, MEDIUM, or LOW") String priority) {
        try {
            Todo.Priority p = parsePriority(priority);
            Todo todo = todoService.updatePriority(id, p);
            return "📌 Updated priority of \"%s\" to %s".formatted(todo.getTitle(), p);
        } catch (IllegalArgumentException e) {
            return "❌ " + e.getMessage();
        }
    }

    @Tool(description = """
            Update an existing todo's title, description, priority, or category.
            Use when user wants to edit or modify a task. Pass null for fields that should not change.
            """)
    public String updateTodo(
            @ToolParam(description = "The numeric ID of the task to update") Long id,
            @ToolParam(description = "New title (or null to keep existing)") String title,
            @ToolParam(description = "New description (or null to keep existing)") String description,
            @ToolParam(description = "New priority: HIGH, MEDIUM, LOW (or null)") String priority,
            @ToolParam(description = "New category: WORK, PERSONAL, HEALTH, SHOPPING, LEARNING, OTHER (or null)") String category) {
        try {
            Todo.Priority p = priority != null ? parsePriority(priority) : null;
            Todo.Category c = category != null ? parseCategory(category) : null;
            Todo todo = todoService.updateTodo(id, title, description, p, c);
            return "✏️ Updated task [ID: %d]: \"%s\"".formatted(todo.getId(), todo.getTitle());
        } catch (IllegalArgumentException e) {
            return "❌ " + e.getMessage();
        }
    }

    // ── Delete ──────────────────────────────────────────────────────────

    @Tool(description = """
            Delete/remove a specific todo by ID. Use when user says 'delete', 'remove', 'get rid of'.
            If the user mentions a task name but not ID, search first to find the ID.
            """)
    public String deleteTodo(
            @ToolParam(description = "The numeric ID of the task to delete") Long id) {
        try {
            todoService.getTodoById(id).ifPresentOrElse(
                    t -> log.debug("Deleting: {}", t.getTitle()),
                    () -> { throw new IllegalArgumentException("Todo not found: " + id); }
            );
            String title = todoService.getTodoById(id).map(Todo::getTitle).orElse("Unknown");
            todoService.deleteTodo(id);
            return "🗑️ Deleted task: \"%s\"".formatted(title);
        } catch (IllegalArgumentException e) {
            return "❌ Task not found with ID: " + id;
        }
    }

    @Tool(description = """
            Remove all completed tasks at once. Use when user says 'clear completed', 'remove done tasks',
            'clean up', or 'archive finished tasks'.
            """)
    public String clearCompletedTodos() {
        int count = todoService.clearCompleted();
        if (count == 0) return "ℹ️ No completed tasks to clear.";
        return "🧹 Cleared %d completed task%s!".formatted(count, count == 1 ? "" : "s");
    }

    // ── Search ──────────────────────────────────────────────────────────

    @Tool(description = """
            Search todos by keyword in title or description. Use when user says 'find', 'search',
            'look for', or mentions a topic to find in tasks.
            """)
    public String searchTodos(
            @ToolParam(description = "Keyword to search for in task titles and descriptions") String keyword) {
        List<Todo> results = todoService.searchTodos(keyword);
        if (results.isEmpty()) return "🔍 No tasks found matching: \"%s\"".formatted(keyword);
        return formatTodoList("🔍 Search Results for \"%s\"".formatted(keyword), results);
    }

    // ── Stats ────────────────────────────────────────────────────────────

    @Tool(description = """
            Get a summary and statistics of the todo list. Use when user asks for a summary,
            overview, stats, how many tasks, or progress report.
            """)
    public String getTodoStats() {
        TodoService.TodoStats stats = todoService.getStats();
        double completionRate = stats.total() > 0
                ? (double) stats.completed() / stats.total() * 100 : 0;

        return """
                📊 Todo Summary
                ───────────────────────────
                Total Tasks     : %d
                ⏳ Pending       : %d
                🚀 In Progress   : %d
                ✅ Completed     : %d
                🔴 Urgent (high) : %d
                ───────────────────────────
                Completion Rate : %.0f%%
                """.formatted(stats.total(), stats.pending(), stats.inProgress(),
                stats.completed(), stats.urgentPending(), completionRate);
    }

    // ── Formatting helpers ───────────────────────────────────────────────

    private String formatTodoList(String header, List<Todo> todos) {
        String lines = todos.stream()
                .map(t -> "  [%d] %s %s ─ %s (%s) [%s]".formatted(
                        t.getId(),
                        statusIcon(t.getStatus()),
                        t.getTitle(),
                        t.getPriority(),
                        t.getCategory(),
                        t.getStatus()))
                .collect(Collectors.joining("\n"));
        return "%s (%d)\n───────────────────────────────────────\n%s"
                .formatted(header, todos.size(), lines);
    }

    private String statusIcon(Todo.Status status) {
        return switch (status) {
            case PENDING     -> "⏳";
            case IN_PROGRESS -> "🚀";
            case COMPLETED   -> "✅";
        };
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }

    private Todo.Priority parsePriority(String priority) {
        if (priority == null) return Todo.Priority.MEDIUM;
        return switch (priority.toUpperCase().trim()) {
            case "HIGH", "URGENT", "CRITICAL" -> Todo.Priority.HIGH;
            case "LOW", "MINOR", "NICE"       -> Todo.Priority.LOW;
            default                            -> Todo.Priority.MEDIUM;
        };
    }

    private Todo.Category parseCategory(String category) {
        if (category == null) return Todo.Category.OTHER;
        try {
            return Todo.Category.valueOf(category.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            return Todo.Category.OTHER;
        }
    }
}
