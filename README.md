# Job Search API

[![CI][ci-badge]][ci-workflow]

A local-first REST API for evaluating job opportunities before an application
workflow begins.

## Current status

The job-posting vertical slice is complete:

- create, retrieve, and fully update a job posting;
- classify opportunities as A, B, or C for either the Java or .NET target track;
- retain an optional plain-text advert snapshot without returning it in list views;
- filter listings by target track and classification;
- find possible duplicates by exact source URL or external ID without blocking
  legitimate reopened postings;
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
| `GET` | `/api/job-postings/{id}` | Returns one complete job posting or `404 Not Found` |
| `PUT` | `/api/job-postings/{id}` | Replaces all editable fields of a job posting |
| `GET` | `/api/job-postings` | Lists summaries, optionally filtered by `targetTrack` and `classification` |
| `GET` | `/api/job-postings/duplicate-candidates` | Finds exact source-reference matches as non-blocking warnings |

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
  "targetTrack": "JAVA",
  "classification": "A",
  "reviewNote": null,
  "descriptionSnapshot": "Build and maintain backend services with Java and Spring."
}
```

At least one of `sourceUrl` and `externalId` is required. `reviewNote` is required when `classification` is `C`.
Classification `A` means a priority fit, `B` a possible fit, and `C` a posting to skip.
`descriptionSnapshot` is optional and limited to 50,000 characters. Detail responses
return it; list and duplicate-candidate responses expose only
`hasDescriptionSnapshot`.

Example filtered list:

```text
GET /api/job-postings?targetTrack=JAVA&classification=A
```

Example duplicate check before creation or update:

```text
GET /api/job-postings/duplicate-candidates?sourceUrl=https://careers.example.com/jobs/123
```

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

The test suite includes focused request and domain tests, a v1-to-v2 migration
test, database-constraint tests against PostgreSQL, and full Spring MVC
integration tests for the API, OpenAPI document, and Swagger UI.

## Design decisions

- Flyway owns schema changes; Hibernate uses `ddl-auto=validate` and never creates the schema.
- API request, application command, JPA entity, and API response are separate boundary models.
- Service methods define transaction boundaries, while controllers handle HTTP concerns only.
- UUID identifiers do not expose record counts; `Instant` records an unambiguous creation time.
- The database repeats critical invariants so invalid data is rejected even if the HTTP layer is bypassed.
- `PUT` is a complete replacement of editable business fields; `createdAt` remains stable while `updatedAt` changes.
- Full advert snapshots appear only in detail responses to keep routine lists small and private by default.
- Source references are intentionally not unique. Duplicate lookup warns about exact matches, while the user decides whether a reopened posting is legitimate.
- Listing is intentionally unpaginated for the small local dataset; pagination
  and supporting indexes belong to a scale-driven change.
- Authentication, public deployment, and application workflow tracking are outside this slice.

## Privacy

Only synthetic test data belongs in Git. Real job applications, advert text,
recruiter messages, database files, dumps, exports, logs, and secrets must remain
local and untracked.

[ci-badge]: https://github.com/SereMark/JobSearchAPI/actions/workflows/ci.yml/badge.svg
[ci-workflow]: https://github.com/SereMark/JobSearchAPI/actions/workflows/ci.yml
