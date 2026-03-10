package com.example.todoai.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Application configuration: CORS, demo data seeding.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class AppConfig {

    // ── CORS ────────────────────────────────────────────────────────────

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(@NotNull CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins("*")
                        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                        .allowedHeaders("*");
            }
        };
    }

    // ── Create Demo Data ────────────────────────────────────────────────────────

//    @Bean
//    public CommandLineRunner seedDemoData(TodoService todoService) {
//        return args -> {
//            log.info("Seeding demo todo data...");
//
//            todoService.createTodo(
//                    "Finish Q4 project report",
//                    "Complete the quarterly analysis with charts",
//                    Todo.Priority.HIGH, Todo.Category.WORK);
//
//            todoService.createTodo(
//                    "Morning run 5km",
//                    "Stick to the training plan",
//                    Todo.Priority.MEDIUM, Todo.Category.HEALTH);
//
//            Todo shopping = todoService.createTodo(
//                    "Buy groceries",
//                    "Milk, eggs, bread, vegetables",
//                    Todo.Priority.MEDIUM, Todo.Category.SHOPPING);
//            todoService.updateStatus(shopping.getId(), Todo.Status.COMPLETED);
//
//            todoService.createTodo(
//                    "Learn Spring AI",
//                    "Read docs and build sample projects",
//                    Todo.Priority.HIGH, Todo.Category.LEARNING);
//
//            todoService.createTodo(
//                    "Call dentist for appointment",
//                    null,
//                    Todo.Priority.LOW, Todo.Category.PERSONAL);
//
//            todoService.createTodo(
//                    "Review pull requests",
//                    "3 PRs waiting for review on GitHub",
//                    Todo.Priority.HIGH, Todo.Category.WORK);
//
//            log.info("Demo data seeded — {} tasks created", todoService.getAllTodos().size());
//        };
//    }
}
