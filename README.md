# 🤖 Todo AI Agent — Spring AI + Open AI

A production-ready AI-powered Todo List Agent built with **Spring Boot 3**, **Spring AI**, and **Open AI**. The AI agent uses gpt's tool-calling (function calling) capability to intelligently manage your tasks through natural language.

## ✨ Features

- **Natural Language Interface** — Chat with OpenAI to manage tasks
- **Spring AI Tool Calling** — 12 AI tools registered via `@Tool` annotations
- **Conversation Memory** — Per-session chat history via `InMemoryChatMemory`
- **Full CRUD REST API** — Traditional endpoints alongside the AI agent
- **H2 In-Memory DB** — Zero setup, JPA/Hibernate
- **Beautiful UI** — Included HTML/JS frontend at `/`

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    HTTP Clients                          │
│           (Browser UI / curl / Postman)                  │
└──────────────────────┬──────────────────────────────────┘
                       │
         ┌─────────────┴─────────────┐
         │                           │
   ┌─────▼──────┐             ┌──────▼──────┐
   │  /api/agent│             │  /api/todos │
   │   (AI Chat)│             │   (CRUD)    │
   └─────┬──────┘             └──────┬──────┘
         │                           │
   ┌─────▼──────────────┐      ┌─────▼──────────┐
   │  TodoAgentService   │      │  TodoService   │
   │  Spring AI          │      │  JPA / H2      │
   │  ChatClient         │      └────────────────┘
   │  + InMemoryChatMemory│
   └─────┬──────────────┘
         │  Tool Calls
   ┌─────▼──────────────┐
   │    TodoTools        │
   │  @Tool methods      │◄──── Open AI LLM decides
   │  (12 tools)         │      which tools to call
   └─────┬──────────────┘
         │
   ┌─────▼──────────────┐
   │   TodoService       │
   │   TodoRepository    │
   └────────────────────┘
```

---

## 🚀 Quick Start

### Prerequisites
- Java 21+
- Maven 3.9+
-OPEN AI API key → [console.anthropic.com](https://console.anthropic.com)

### Run

```bash
# 1. Set your API key
export OPENAI_API_KEY=sk-ant-your-key-here

# 2. Build and run
mvn spring-boot:run

# 3. Open browser
open http://localhost:8081
```

### Or with Maven Wrapper
```bash
./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-DANTHROPIC_API_KEY=$OPENAI_API_KEY"
```

---

## 📡 API Reference

### AI Agent Chat

**POST** `/api/agent/chat`
```json
{
  "message": "Add buy milk as a high priority personal task",
  "sessionId": "optional-session-id"
}
```
Response:
```json
{
  "sessionId": "session-abc123",
  "reply": "✅ Task created! [ID: 7] \"Buy milk\" — HIGH priority, PERSONAL category"
}
```

**DELETE** `/api/agent/chat/{sessionId}` — Clear conversation history

---

### Natural Language Examples

| What you say | What the agent does |
|---|---|
| `"Add finish report as urgent work task"` | Creates HIGH priority WORK task |
| `"What should I focus on today?"` | Returns tasks sorted by priority |
| `"Mark task 3 as done"` | Completes task ID 3 |
| `"Start working on the Spring AI task"` | Searches & sets status IN_PROGRESS |
| `"Show me my stats"` | Returns completion summary |
| `"Clear all completed tasks"` | Deletes COMPLETED todos |
| `"Find all health tasks"` | Searches by category |
| `"Make the report task high priority"` | Updates priority |

---

### Todo CRUD Endpoints

| Method | Path | Description |
|---|---|---|
| GET | `/api/todos` | All todos (filter: `?status=`, `?priority=`, `?search=`) |
| GET | `/api/todos/{id}` | Get by ID |
| POST | `/api/todos` | Create todo |
| PUT | `/api/todos/{id}` | Update todo |
| PATCH | `/api/todos/{id}/status` | Update status only |
| DELETE | `/api/todos/{id}` | Delete todo |
| DELETE | `/api/todos/completed` | Clear completed |
| GET | `/api/todos/stats` | Statistics |

---

## 🔧 Configuration

`src/main/resources/application.yml`

```properties
# Model selection
spring.ai.openai.chat.options.model=gpt-4o-mini

# Tune response creativity
spring.ai.openai.chat.options.temperature=0.7

# H2 console (dev only)
spring.h2.console.enabled=true   # http://localhost:8080/h2-console
```

---

## 🛠️ AI Tools Registered

The `TodoTools` class exposes these tools to Claude via `@Tool`:

| Tool Method | Description |
|---|---|
| `createTodo` | Add a new task |
| `listAllTodos` | Show all tasks |
| `listTodosByStatus` | Filter by PENDING/IN_PROGRESS/COMPLETED |
| `listTodosByPriority` | Filter by HIGH/MEDIUM/LOW |
| `getTopPriorityTasks` | Active tasks sorted by urgency |
| `completeTodo` | Mark a task complete |
| `startTodo` | Mark a task in-progress |
| `updatePriority` | Change task priority |
| `updateTodo` | Edit task details |
| `deleteTodo` | Remove a task |
| `clearCompletedTodos` | Bulk delete completed |
| `searchTodos` | Full-text keyword search |
| `getTodoStats` | Summary statistics |

---

## 📦 Tech Stack

| Layer | Technology |
|---|---|
| Framework | Spring Boot 3.3 |
| AI | Spring AI 1.0 + OPEN AI |
| Database | H2 (in-memory) / JPA / Hibernate |
| Validation | Jakarta Validation |
| Boilerplate | Lombok |
| Java | 21 (records, text blocks, switch expressions) |
| Frontend | Vanilla HTML/CSS/JS (served from `/static`) |

---

## 🧪 Testing

```bash
mvn test
```

---

## 📄 License
MIT
