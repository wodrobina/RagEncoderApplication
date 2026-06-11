# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

- Build: `./gradlew build`
- Run tests: `./gradlew test`
- Run a single test class: `./gradlew test --tests <package.ClassName>`
- Run the application: `./gradlew bootRun`

## Architecture

This is a Spring Boot RAG (Retrieval-Augmented Generation) application that implements a pipeline for indexing and searching text via vector embeddings.

### Core Pipeline Components

1.  **API Layer (`eu.wodrobina.ragencoderapplication.api`)**:
    *   Provides REST endpoints through `RagController` for embedding generation, text indexing, and semantic search.
2.  **Chunking Engine (`eu.wodrobina.ragencoderapplication.chunking`)**:
    *   Handles the decomposition of raw text into smaller, manageable segments (chunks) via `TextChunker`.
3.  **Embedding Provider (`eu.wodrobina.ragencoderapplication.encoder`)**:
    *   Abstracts the logic for converting text strings into high-dimensional vectors.
    - Implementation: `OllamaEmbeddingProvider` communicates with a local Ollama instance to generate embeddings.
4.  **Indexing & Storage (`eu.wodrobina.ragencoderapplication.index`)**:
    *   Manages the lifecycle of vector documents.
    *   The `IndexingService` coordinates between chunking and embedding generation to populate the store.
    *   Uses a `VectorStore` interface (implemented by `InMemoryVectorStore`) for storing and retrieving vectors.

### Data Flow
Text -> `Chunker` -> `EmbeddingProvider` -> `IndexingService` -> `VectorStore` (via chunks + embeddings). Search follows a similar path: Query -> `EmbeddingProvider` -> `VectorStore.search()`.

The configuration (model names, chunk sizes, Ollama URL) is managed via `src/main/resources/application.yaml`.
