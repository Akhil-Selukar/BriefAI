<div align="center">
    <h1>BriefAI</h1>
</div>

BriefAI is a full-stack Retrieval Augmented Generation (RAG) application that allows users to upload documents and ask questions about the documents.

The application processes PDF and Word documents, generates vector embeddings, performs semantic retrieval using PostgreSQL with pgvector, and uses a locally hosted Ollama model to generate contextual answers with source citations.

It also includes email based account verification using OTP, JWT authentication, isolated document retrieval based on logged in user, persistent conversations, conversation aware follow up questions and Dockerized application services.

## Features

### Document Q&A with RAG

- Upload PDF, DOC, and DOCX documents.
- Parse and chunk the document content.
- Generate embeddings of chunks.
- Store and search embeddings using PostgreSQL and pgvector.
- Generate answers only based on retrieved content using llama3.2:3b.
- Display citations for document sources actually referenced in the generated answer.

### Persistent Conversations

- Create and delete conversations.
- Persist users question, answer given by model and citations used in it.
- Use conversation history to support contextual follow up questions.
- Persist conversations to refer after relogin or application restat.

### Authentication & Security

- User registration with name, email, and password.
- Standard strong password validation rules.
- Email verification using a six digit OTP.
- Hashed OTP storage with configurable expiration period.
- Configurable resend OTP cooldown period and max invalid attempt to make the OTP invalid.
- Password hashing before storing into database.
- JWT based authentication.
- User level isolation for documents, conversations, and retrieval.

### Document Management

- Configurable per user document limit and maximum size of the document.
- Well defined document processing lifecycle<br>
  `Upload -> Processing -> Ready/Failed`

## High level application architecture

![High level architecture diagram](docs\images\High_level_architecture.png)

The frontend, backend and PostgreSQL database runs as docker services while Ollama intentionally runs on the host machine rather than inside the docker. The backend container communicates with it through `http://host.docker.internal:11434`. This avoids duplicating the relatively large Ollama runtime and model files inside docker while keeping the rest of the application reproducible through containers.
