# Recall (working name)

Appointment reminders for small local businesses, starting with clinics. The business sees which
reminders are due and sends each one on WhatsApp with one tap. Automatic sending comes later.

## Run it locally

You need JDK 21 or newer and Docker.

```
docker compose up -d
./gradlew :app:bootRun --args='--spring.profiles.active=dev'
```

Open http://localhost:8080 and log in with `demo@recall.test` / `demo-recall-2026` (dev profile only).
The `dev` profile loads a demo clinic with three appointments:
one reminder is due, one is not due yet, and one customer has opted out.

Run the tests with `./gradlew build`. Docker must be running: the `persistence` tests start a
throwaway PostgreSQL with Testcontainers.

Using Colima instead of Docker Desktop? Testcontainers does not read `docker context`, so add this
to your shell profile (for example `~/.zshrc`), or the tests fail with "Could not find a valid Docker environment":

```
export DOCKER_HOST="unix://$HOME/.colima/default/docker.sock"
export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock
```

## Running it for a pilot clinic

The `Dockerfile` builds the whole app into one image. Any host that runs containers and offers a
PostgreSQL 16 database will do. Set these environment variables:

| Variable | Meaning |
|---|---|
| `DATABASE_URL` | for example `jdbc:postgresql://host:5432/recall` |
| `DATABASE_USER`, `DATABASE_PASSWORD` | the database login |
| `PORT` | set by most hosts; defaults to 8080 |

Do **not** set the `dev` profile in production: it loads demo data and a demo login with a published password.
The database is created by Flyway on first start.

There is no sign-up page. Give a clinic its account with:

```
DATABASE_URL_PSQL="postgresql://user:password@host:5432/recall" \
  scripts/create-clinic.sh "Clinic name" owner@clinic.example "a-strong-password"
```

Serve it over https (most hosts do this for you). Passwords and the login session travel over the connection.

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

- Login. The dashboard lists the next 7 days of appointments for the logged-in user's business.
- "Send on WhatsApp" records the reminder as sent and opens WhatsApp with the text filled in.
- Customers without consent are never offered a send button.
- `/appointments/new` adds one appointment by hand. Each row can be marked confirmed or cancelled, and the top shows the last 30 days.
- `/import` loads appointments from a CSV file (an example is at `/exemplu.csv`).
- With `recall.worker.enabled=true`, the scheduled job "sends" due reminders through the logging fake.

## First issues

1. Login with Spring Security, and show the logged-in user's business instead of the first one. (done)
2. CSV import for customers and appointments. (done)
3. Record each send in the `message` table.
4. Read rules from the `rule` table, falling back to the vertical defaults.
5. Integration tests against a real Postgres with Testcontainers.
6. Romanian message templates.

See `docs/` for the product notes, architecture and decisions, and `CLAUDE.md` for working conventions.
