# Job Search API

[![CI][ci-badge]][ci-workflow]

A local backend REST API for tracking job postings.

## Current status

The first vertical slice is implemented for job postings:

- create a job posting;
- get a job posting by ID;
- list job postings in reverse creation order;
- persist data in PostgreSQL through Spring Data JPA and Hibernate;
- manage schema changes with Flyway and validate the JPA mapping at startup;
- return consistent Problem Details error responses;
- exercise the complete Spring MVC-to-database path with integration tests;
- expose the API through OpenAPI and Swagger UI;
- verify every pull request and `main` update in GitHub Actions.

Application workflow tracking is the next vertical slice and is not implemented yet.

## Technology stack

- Java 21
- Spring Boot 4.0.7
- Spring MVC and Bean Validation
- Spring Data JPA and Hibernate
- PostgreSQL 18.4
- Flyway
- Testcontainers
- OpenAPI and Swagger UI
- Maven Wrapper
- Docker Compose
- GitHub Actions

## API

| Method | Path | Result |
|---|---|---|
| `POST` | `/api/job-postings` | Creates a job posting and returns `201 Created` |
| `GET` | `/api/job-postings/{id}` | Returns one job posting or `404 Not Found` |
| `GET` | `/api/job-postings` | Returns all job postings, newest first |

Example create request:

```json
{
  "companyName": "Example Technologies Kft.",
  "roleTitle": "Java Backend Developer",
  "source": "Company careers",
  "sourceUrl": "https://careers.example.com/jobs/123",
  "externalId": "JOB-123",
  "location": "Budapest",
  "workMode": "HYBRID",
  "foundOn": "2026-07-30",
  "classification": "A",
  "reviewNote": null
}
```

At least one of `sourceUrl` and `externalId` is required. `reviewNote` is required when `classification` is `C`.
Classification `A` means a priority fit, `B` a possible fit, and `C` a posting to skip.

## Running locally

Requirements:

- JDK 21
- Docker Desktop or Docker Engine with Docker Compose

Start PostgreSQL:

```shell
docker compose up -d --wait postgres
```

Run the application on Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

Run it on Linux or macOS:

```shell
./mvnw spring-boot:run
```

By default, the application and database ports are bound to the local machine only. The API is available at
`http://127.0.0.1:8080`.

Explore the running API at:

- Swagger UI: `http://127.0.0.1:8080/swagger-ui.html`
- OpenAPI JSON: `http://127.0.0.1:8080/v3/api-docs`

Stop PostgreSQL without deleting its data:

```shell
docker compose down
```

The named Docker volume keeps local records across container and application
restarts. To deliberately delete that local database, run
`docker compose down --volumes`.

## Verification

Docker must be running because integration tests use a disposable PostgreSQL Testcontainer.

Windows:

```powershell
.\mvnw.cmd clean verify
```

Linux or macOS:

```shell
./mvnw clean verify
```

The test suite includes focused validation and domain tests, a JPA repository
test against PostgreSQL, and full Spring MVC integration tests for the API,
OpenAPI document, and Swagger UI.

## Design decisions

- Flyway owns schema changes; Hibernate uses `ddl-auto=validate` and never creates the schema.
- API request, application command, JPA entity, and API response are separate boundary models.
- Service methods define transaction boundaries, while controllers handle HTTP concerns only.
- UUID identifiers do not expose record counts; `Instant` records an unambiguous creation time.
- The database repeats critical invariants so invalid data is rejected even if the HTTP layer is bypassed.
- Source URLs are intentionally not unique: a reopened posting or a different external posting ID may be legitimate.
- Listing is intentionally unpaginated for the small local dataset; pagination
  and supporting indexes belong to a scale-driven change.
- Authentication, public deployment, duplicate detection, and job-posting updates are outside this slice.

## Privacy

Only synthetic test data belongs in Git. Real job applications, advert text,
recruiter messages, database files, dumps, exports, logs, and secrets must remain
local and untracked.

[ci-badge]: https://github.com/SereMark/JobSearchAPI/actions/workflows/ci.yml/badge.svg
[ci-workflow]: https://github.com/SereMark/JobSearchAPI/actions/workflows/ci.yml
