# Kinloop Backend (`kinloop-be`)

Spring Boot API for Kinloop, a platform that helps parents discover and manage
developmentally appropriate activities for their children.

The service includes JWT authentication, email verification, child onboarding,
questionnaires, consent management, daily activity plans, feedback-based
recommendations, and optional Anthropic-powered feedback classification.

## Tech stack

- Java 21
- Spring Boot 3.3
- Maven
- PostgreSQL 16
- Spring Data JPA and Flyway
- Spring Security and JWT
- Testcontainers

## Quick start

### 1. Prerequisites

Install:

- JDK 21
- Maven 3.9+
- PostgreSQL, or Docker for the database container shown below

The repository does not include the Maven Wrapper, so `mvn` must be available on
your `PATH`.

### 2. Start PostgreSQL

If PostgreSQL is already installed, create a database and user and skip to the
next step. To start PostgreSQL 16 with Docker:

```bash
docker run --name kinloop-postgres \
  -e POSTGRES_DB=kinloop \
  -e POSTGRES_USER=kinloop \
  -e POSTGRES_PASSWORD=kinloop \
  -p 5432:5432 \
  -d postgres:16-alpine
```

For later runs, restart the existing container with:

```bash
docker start kinloop-postgres
```

### 3. Set the environment variables

Run these commands in the same terminal that will start the application:

```bash
export DB_URL='jdbc:postgresql://localhost:5432/kinloop'
export DB_USERNAME='kinloop'
export DB_PASSWORD='kinloop'
export JWT_SECRET="$(openssl rand -base64 32)"
export LLM_ENABLED='false'
```

`JWT_SECRET` must contain at least 32 bytes of key material. Keep a stable,
private value outside local development; changing it invalidates issued tokens.

Spring Boot does not load a `.env` file automatically. Export the variables in
your shell or configure them in your IDE/run environment.

### 4. Run the application

```bash
mvn spring-boot:run
```

The API starts at `http://localhost:8080`. On startup, Flyway automatically
applies the migrations in `src/main/resources/db/migration`; do not create the
tables manually.

## Environment variables

| Variable | Required | Default | Purpose |
| --- | --- | --- | --- |
| `DB_URL` | Yes | None | PostgreSQL JDBC URL, for example `jdbc:postgresql://localhost:5432/kinloop`. |
| `DB_USERNAME` | Yes | None | PostgreSQL username. |
| `DB_PASSWORD` | Yes | None | PostgreSQL password. |
| `JWT_SECRET` | Yes | None | Secret used to sign JWTs; use at least 32 bytes. Plain text and Base64 values are accepted. |
| `PORT` | No | `8080` | HTTP port for the application. |
| `CORS_ALLOWED_ORIGINS` | No | `http://localhost:3000` | Comma-separated list of allowed browser origins. |
| `LLM_ENABLED` | No | `true` | Enables Anthropic feedback classification. Set to `false` for local development without an API key. |
| `ANTHROPIC_API_KEY` | When LLM is enabled | Empty | Anthropic API key. |
| `LLM_MODEL` | No | `claude-haiku-4-5` | Anthropic model used for feedback classification. |

LLM failures do not fail the feedback API; classification is skipped and the
ordinary feedback-learning path continues.

## Build and test

Run the test suite:

```bash
mvn test
```

Docker is needed for the PostgreSQL Testcontainers integration tests. Those
tests are skipped when Docker is unavailable; unit tests still run.

Create the executable JAR:

```bash
mvn clean package
java -jar target/kinloop-be-0.0.1-SNAPSHOT.jar
```

The same environment variables are required when running the JAR.

Build the application image:

```bash
docker build -t kinloop-be .
```

## API overview

Authentication endpoints under `/api/auth/**` are public. All other endpoints
require an `Authorization: Bearer <token>` header.

Main endpoint groups:

- `/api/auth` - registration, email verification, and login
- `/api/profile` - current parent profile
- `/api/consents` - consent records
- `/api/children` - child profiles
- `/api/children/{childId}/questionnaire` - onboarding questionnaire
- `/api/children/{childId}/daily-plan` - daily recommendations and feedback
- `/api/children/{childId}/activity-history` - completed activity history
- `/api/home` - home-screen status and feedback questions

In the current local email implementation, verification links are written to the
application log instead of being sent. Copy the `token` from the logged link and
open it against the local server:

```text
http://localhost:8080/api/auth/verify?token=<token>
```

## Project structure

```text
src/main/java/com/kinloop/backend
├── config/       # Security and application configuration
├── controller/   # REST controllers
├── dto/          # API request and response models
├── entity/       # JPA entities and enums
├── exception/    # API error handling
├── mapper/       # Entity/DTO mapping
├── repository/   # Spring Data repositories
├── security/     # JWT authentication
└── service/      # Business logic, matching, and LLM integration

src/main/resources
├── application.yml
├── db/migration/ # Flyway schema and data migrations
└── prompts/      # LLM prompts
```
