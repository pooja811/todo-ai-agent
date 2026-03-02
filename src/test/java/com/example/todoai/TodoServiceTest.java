package com.example.todoai;

import com.example.todoai.model.Todo;
import com.example.todoai.service.TodoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class TodoServiceTest {

    @Autowired
    TodoService todoService;

    @Test
    void createAndRetrieveTodo() {
        Todo todo = todoService.createTodo(
                "Test task", "Description",
                Todo.Priority.HIGH, Todo.Category.WORK);

        assertThat(todo.getId()).isNotNull();
        assertThat(todo.getTitle()).isEqualTo("Test task");
        assertThat(todo.getPriority()).isEqualTo(Todo.Priority.HIGH);
        assertThat(todo.getStatus()).isEqualTo(Todo.Status.PENDING);
    }

    @Test
    void completeTask() {
        Todo todo = todoService.createTodo("Complete me", null, null, null);
        Todo updated = todoService.updateStatus(todo.getId(), Todo.Status.COMPLETED);
        assertThat(updated.getStatus()).isEqualTo(Todo.Status.COMPLETED);
    }

    @Test
    void searchByKeyword() {
        todoService.createTodo("Buy apples", "From the market", null, null);
        todoService.createTodo("Buy oranges", "Fresh ones", null, null);

        List<Todo> results = todoService.searchTodos("Buy");
        assertThat(results).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void deleteNonExistentThrows() {
        assertThatThrownBy(() -> todoService.deleteTodo(99999L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void statsAreAccurate() {
        todoService.createTodo("Stats test task", null, Todo.Priority.HIGH, null);
        TodoService.TodoStats stats = todoService.getStats();
        assertThat(stats.total()).isGreaterThan(0);
    }
}
