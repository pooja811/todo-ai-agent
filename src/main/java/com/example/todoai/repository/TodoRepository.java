package com.example.todoai.repository;

import com.example.todoai.model.Todo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TodoRepository extends JpaRepository<Todo, Long> {

    List<Todo> findByStatus(Todo.Status status);

    List<Todo> findByPriority(Todo.Priority priority);

    List<Todo> findByCategory(Todo.Category category);

    List<Todo> findByStatusAndPriority(Todo.Status status, Todo.Priority priority);

    @Query("SELECT t FROM Todo t WHERE LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(t.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Todo> searchByKeyword(@Param("keyword") String keyword);

    @Query("SELECT t FROM Todo t WHERE t.status != 'COMPLETED' ORDER BY " +
           "CASE t.priority WHEN 'HIGH' THEN 1 WHEN 'MEDIUM' THEN 2 WHEN 'LOW' THEN 3 END, " +
           "t.createdAt ASC")
    List<Todo> findActiveSortedByPriority();

    @Query("SELECT COUNT(t) FROM Todo t WHERE t.status = :status")
    long countByStatus(@Param("status") Todo.Status status);

    @Query("SELECT COUNT(t) FROM Todo t WHERE t.priority = 'HIGH' AND t.status != 'COMPLETED'")
    long countUrgentPending();
}
