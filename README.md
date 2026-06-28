# Kinloop Backend (`kinloop-be`)

Backend foundation for Kinloop, a platform that connects parents with children's workshops and play groups.

## Tech Stack

- Java 21
- Spring Boot 3.x
- Maven
- PostgreSQL
- Spring Data JPA
- Flyway
- Jakarta Validation
- Lombok

## Current Scope

This repository currently provides only project initialization and infrastructure setup.

Implemented:
- Spring Boot project skeleton
- Layered package structure (empty, ready for implementation)
- Maven configuration and required dependencies
- Basic PostgreSQL, JPA, and Flyway configuration
- Flyway migration directory

Not implemented yet:
- Business logic
- REST endpoints
- Entities/repositories/services
- Authentication and authorization
- LLM/recommendation integration

## Project Structure

```text
src/main/java/com/kinloop/backend
├── config/
├── controller/
├── service/
│   └── impl/
├── repository/
├── entity/
├── dto/
├── mapper/
├── exception/
├── validation/
└── util/
```

## Run

```bash
mvn spring-boot:run
```

## Test

```bash
mvn test
```
