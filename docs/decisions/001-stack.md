# 001: Java and Spring Boot, one deployable

Date: 2026-11-12

## Decision

Java 21, Spring Boot, a Gradle multi-module build, PostgreSQL with Flyway, and Thymeleaf pages.
The dashboard and the scheduler run in one application.

## Why

- Both of us know Java, so one language covers the whole product.
- Server-rendered pages avoid a separate frontend build.
- One deployable keeps hosting cheap and simple at pilot scale.

## Consequences

- If the dashboard needs rich interactivity later, `web` can expose a JSON API to a separate frontend.
- If sending volume grows, `worker` can be split into its own process, because it only depends on
  `core`, `persistence` and `verticals`.
