package com.example.todoai.config;

import com.example.todoai.model.Todo;
import com.example.todoai.service.TodoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
//import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Application configuration: CORS, chat memory, and demo data seeding.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class AppConfig {

    // ── Chat Memory ─────────────────────────────────────────────────────

//    @Bean
//    public ChatMemory chatMemory() {
//        return new InMemoryChatMemory();
//    }

    // ── CORS ────────────────────────────────────────────────────────────

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins("*")
                        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                        .allowedHeaders("*");
            }
        };
    }

    // ── Demo Data ────────────────────────────────────────────────────────

    @Bean
    public CommandLineRunner seedDemoData(TodoService todoService) {
        return args -> {
            log.info("Seeding demo todo data...");

            todoService.createTodo(
                    "Finish Q4 project report",
                    "Complete the quarterly analysis with charts",
                    Todo.Priority.HIGH, Todo.Category.WORK);

            todoService.createTodo(
                    "Morning run 5km",
                    "Stick to the training plan",
                    Todo.Priority.MEDIUM, Todo.Category.HEALTH);

            Todo shopping = todoService.createTodo(
                    "Buy groceries",
                    "Milk, eggs, bread, vegetables",
                    Todo.Priority.MEDIUM, Todo.Category.SHOPPING);
            todoService.updateStatus(shopping.getId(), Todo.Status.COMPLETED);

            todoService.createTodo(
                    "Learn Spring AI",
                    "Read docs and build sample projects",
                    Todo.Priority.HIGH, Todo.Category.LEARNING);

            todoService.createTodo(
                    "Call dentist for appointment",
                    null,
                    Todo.Priority.LOW, Todo.Category.PERSONAL);

            todoService.createTodo(
                    "Review pull requests",
                    "3 PRs waiting for review on GitHub",
                    Todo.Priority.HIGH, Todo.Category.WORK);

            log.info("Demo data seeded — {} tasks created", todoService.getAllTodos().size());
        };
    }
}
