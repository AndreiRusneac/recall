# Recall (working name)

Appointment reminders for small local businesses, starting with clinics. The business sees which
reminders are due and sends each one on WhatsApp with one tap. Automatic sending comes later.

## Run it locally

You need JDK 21 or newer and Docker.

```
docker compose up -d
./gradlew :app:bootRun --args='--spring.profiles.active=dev'
```

Open http://localhost:8080. The `dev` profile loads a demo clinic with three appointments:
one reminder is due, one is not due yet, and one customer has opted out.

Run the tests with `./gradlew build`. Docker must be running: the `persistence` tests start a
throwaway PostgreSQL with Testcontainers.

Using Colima instead of Docker Desktop? Testcontainers does not read `docker context`, so add this
to your shell profile (for example `~/.zshrc`), or the tests fail with "Could not find a valid Docker environment":

```
export DOCKER_HOST="unix://$HOME/.colima/default/docker.sock"
export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock
```

## Modules

| Module | What it holds | Owner |
|---|---|---|
| `core` | Pure Java rules: when a reminder is due, message text, plans and features. No Spring. | Person A |
| `persistence` | JPA entities, repositories, Flyway migrations. | Person A |
| `messaging` | Implementations of `MessageSender`. Today only a logging fake. | Person A |
| `worker` | Scheduled job for automatic sending. Off by default. | Person A |
| `web` | Dashboard pages and controllers (Thymeleaf). | Person B |
| `verticals` | Default rules and wording per kind of business. | Person B |
| `app` | The main class and configuration. The only runnable module. | Shared |

Dependencies point inwards: `web` and `worker` use `core`, `persistence` and `verticals`;
`core` uses nothing.

## What works today

- The dashboard lists the next 7 days of appointments for the first business in the database.
- "Send on WhatsApp" records the reminder as sent and opens WhatsApp with the text filled in.
- Customers without consent are never offered a send button.
- With `recall.worker.enabled=true`, the scheduled job "sends" due reminders through the logging fake.

## First issues

1. Login with Spring Security, and show the logged-in user's business instead of the first one.
2. CSV import for customers and appointments.
3. Record each send in the `message` table.
4. Read rules from the `rule` table, falling back to the vertical defaults.
5. Integration tests against a real Postgres with Testcontainers.
6. Romanian message templates.

See `docs/` for the product notes, architecture and decisions, and `CLAUDE.md` for working conventions.
