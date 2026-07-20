# Job Search API

A backend REST API for tracking job applications and recruitment activities.

## Project status

The project is under active development.

The initial Spring Boot setup is complete. No business API endpoints have been implemented yet.

## Current technology stack

* Java 21
* Spring Boot 4.0.7
* Spring MVC
* Maven 3.9.16 via Maven Wrapper
* JUnit Jupiter 6

## Planned technology stack

* PostgreSQL
* Spring Data JPA and Hibernate
* Flyway
* Spring Security
* Bean Validation
* Testcontainers
* Docker Compose
* OpenAPI
* GitHub Actions

Planned technologies are introduced incrementally as part of complete vertical features.

## Requirements

* JDK 21

A separate Maven installation is not required because the project includes the Maven Wrapper.

## Building the project

On Windows:

```shell
.\mvnw.cmd clean verify
```

On Linux or macOS:

```shell
./mvnw clean verify
```

## Running the application

On Windows:

```shell
.\mvnw.cmd spring-boot:run
```

On Linux or macOS:

```shell
./mvnw spring-boot:run
```

The application starts on:

```text
http://localhost:8080
```

Because no API endpoints have been implemented yet, requesting the root URL currently returns `404 Not Found`.
