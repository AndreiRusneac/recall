## What this is

Recall is a subscription app that reduces no-shows for small local businesses, starting with
clinics in Cluj-Napoca. The owner sees which appointment reminders are due and sends each one
on WhatsApp with one tap (a pre-filled wa.me link). Automatic sending through the WhatsApp API
comes later. There are two plans, REMINDERS and BOOKING, as one app with a feature switch.

@docs/product.md
@docs/architecture.md

## Current state

Working: dashboard of the next 7 days, tap-to-send with consent check, scheduled job
(off by default, logging sender only), schema, demo data, Romanian reminder templates.
Not built yet: login, CSV import, recording sent messages in the `message` table, rules from
the `rule` table, booking page, real WhatsApp API sending.
Update this section when a pull request changes it.

## Known gotchas

- Spring Boot 4 renamed several starters (spring-boot-starter-webmvc, spring-boot-starter-flyway).
  Do not "fix" them back to the Boot 3 names.
- Messages to customers always go through the MessageSender interface in core.

# Working in this repo

Two people work here, each with Claude. These rules keep the work from colliding.

## Stack

- Java 21, Spring Boot 4.1, Gradle (Groovy DSL) multi-module build
- PostgreSQL 16, Flyway migrations, Spring Data JPA
- Thymeleaf server-rendered pages
- JUnit 5

## Commands

- Start the database: `docker compose up -d`
- Run the app: `./gradlew :app:bootRun --args='--spring.profiles.active=dev'`
- Run all checks: `./gradlew build`
- Run one module's tests: `./gradlew :core:test`

## Module boundaries

- `core` is pure Java. Never add Spring, JPA, HTTP or database code to it. Logic that decides
  something (is a reminder due, what does the message say, may this plan use this feature) goes here, with tests.
- `core` never reads the clock. Pass `Instant now` in.
- Only `persistence` contains entities, repositories and SQL.
- Only `messaging` talks to message providers. Everything else uses the `MessageSender` interface from `core`.
- `web` and `worker` never depend on each other.
- New library versions come from the Spring Boot BOM. Do not write version numbers in module build files.

## Database

- Never edit a merged migration. Add the next `V<n>__description.sql` in `persistence/src/main/resources/db/migration`.
- Agree schema changes with the other person before writing them.
- Demo data lives in `db/dev` and is loaded only with the `dev` profile.

## Rules that protect customers

- Never send to a customer whose consent is not `GRANTED`.
- Never put real names or phone numbers in code, tests, demo data or commits.

## Git

- Never commit to `main`. One branch per issue, merged by a pull request the other person reviews.
- Keep pull requests small: one issue, one module where possible.
- `./gradlew build` must pass before asking for review.
- Record any decision that changes the stack, schema or module boundaries as a short file in `docs/decisions/`.
