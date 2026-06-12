# AGENTS.md

This file provides guidance for AI coding agents working in this repository.

## Project context

This is a Spring Boot RAG application for indexing text and performing semantic search using vector embeddings.

Core flow:

Text -> Chunker -> EmbeddingProvider -> IndexingService -> VectorStore
Query -> EmbeddingProvider -> VectorStore.search()

Main architectural areas:

- `api` — REST entry points and request/response DTOs.
- `chunking` — text splitting logic.
- `encoder` — embedding generation abstraction and implementations.
- `index` — indexing orchestration and vector storage.
- `config` / `resources` — Spring configuration and application settings.

## Commands

Use the Gradle wrapper.

./gradlew build
./gradlew test
./gradlew test --tests "fully.qualified.TestClassName"
./gradlew bootRun

After changing production code, run at least:

./gradlew test

Run `./gradlew build` when the change affects wiring, dependencies, application startup, or public API behavior.

## Architecture and design rules

Write domain-oriented code.

- Keep business rules in domain/application services, not in controllers, Spring configuration, or infrastructure classes.
- Model domain concepts explicitly with meaningful names.
- Prefer small cohesive classes over procedural utility-heavy code.
- Keep REST DTOs separate from domain objects when the API contract differs from the domain model.
- Avoid leaking framework concerns into domain code.
- Prefer constructor injection.
- Prefer immutable value objects where practical.
- Make invalid states hard to represent.

Use DDD-inspired boundaries.

- Treat interfaces such as embedding providers, chunkers, vector stores, and repositories as ports.
- Treat implementations such as Ollama clients, in-memory stores, HTTP adapters, and Spring configuration as infrastructure.
- Do not let infrastructure decisions dominate the domain API.
- Keep orchestration explicit in application services.

Use known design patterns when they clarify the code.

Good fits for this project include:

- Strategy — for chunking algorithms, embedding providers, and search scoring.
- Repository / Store — for vector persistence.
- Factory — for constructing domain objects with validation.
- Adapter — for external services such as Ollama.
- Facade / Application Service — for indexing and search use cases.
- Specification or Policy — for configurable domain rules.

Do not introduce patterns only to make the code look more “designed”. Use them when they reduce coupling, clarify intent, or isolate variability.

## Testing rules

Use JUnit 5 or JUnit 6.

Every test should use `@DisplayName`.

Prefer behavior-oriented test names and display names.

Tests should follow a `given / when / then` structure.

Prefer this style:

@Test
@DisplayName("indexes text by chunking it and storing generated embeddings")
void indexesTextByChunkingItAndStoringGeneratedEmbeddings() {
givenTextToIndex();
givenEmbeddingProviderReturnsVectors();

    whenTextIsIndexed();

    thenChunksAreStoredWithEmbeddings();
}

The `given`, `when`, and `then` lines should be method calls that describe the scenario. Avoid large anonymous setup blocks inside test methods.

Prefer stubs and fakes over mocks.

- Use simple hand-written stubs when behavior is deterministic.
- Use fakes for in-memory stores or providers when they make the test closer to real behavior.
- Avoid mocking domain objects.
- Avoid mocking value objects.
- Avoid mocking simple collaborators when a stub is clearer.
- Use mocks only when verifying interaction with an external boundary is the actual behavior under test.

Test behavior, not implementation details.

- Assert observable outcomes.
- Avoid testing private methods directly.
- Avoid brittle assertions tied to internal ordering unless ordering is part of the contract.
- Keep tests independent and deterministic.
- Do not require a running Ollama instance for unit tests.

## Spring and integration tests

Use Spring context tests only when Spring wiring is part of the behavior under test.

Prefer plain unit tests for domain and application logic.

When an integration test needs infrastructure, make the dependency explicit and keep the test isolated.

Do not introduce network calls in unit tests.

## Code style

Use Java 21 features when they improve readability.

Prefer clear names over comments.

Add comments only to explain non-obvious decisions, trade-offs, or domain rules.

Keep public APIs small and intentional.

Do not add dependencies unless they are justified by the task.

Do not add global mutable state.

## Error handling

Validate inputs close to the boundary.

Use meaningful exceptions for domain and application errors.

Do not swallow exceptions silently.

Do not expose internal exception details through API responses unless explicitly intended.

## Configuration

Keep configurable values in `application.yaml` or typed configuration properties.

Do not hardcode model names, URLs, chunk sizes, or infrastructure-specific values in domain code.

## Before finishing a task

Before presenting a change, check:

- production code compiles;
- relevant tests pass;
- new behavior is covered by tests;
- tests use `@DisplayName`;
- tests follow the given/when/then style;
- domain logic is not placed in controllers or infrastructure;
- no unnecessary mocks were introduced;
- no unrelated refactoring was included.