# javapp-api

A minimal HTTP backend built with Java 21, Spring Boot, and Gradle Kotlin DSL. It exists as a clean starting point for the `com.aireck.javapp` service: one Spring Boot entry point, one REST controller, and a contract test.

## Requirements

- JDK 21
- No separate Gradle install — use the committed wrapper (`./gradlew`)

## Quick start

```shell
git clone git@github.com:aireck2/javapp-api.git
cd javapp-api
./gradlew bootRun
```

The application listens on `http://localhost:8080` by default.

```shell
curl --include http://localhost:8080/hello
```

```text
HTTP/1.1 200
Content-Type: text/plain;charset=UTF-8

Hello, world!
```

## Commands

| Command | Purpose |
| --- | --- |
| `./gradlew bootRun` | Run the application locally |
| `./gradlew test` | Run the test suite |
| `./gradlew clean build` | Build and test from scratch |
| `./gradlew spotlessApply` | Format Java sources with Google Java Format |
| `./gradlew spotlessCheck` | Verify formatting without changing files |

## Project structure

```text
src/main/java/com/aireck/javapp/
  JavappApiApplication.java        # Spring Boot entry point
  hello/api/HelloController.java   # GET /hello
src/main/resources/
  application.properties           # Application configuration
src/test/java/com/aireck/javapp/
  hello/api/HelloControllerTest.java
docs/
  specs/  plans/  tasks/           # Project documentation
build.gradle.kts                   # Build, dependencies, formatting
```

## Configuration

Configuration lives in `src/main/resources/application.properties`. To serve on a different port without changing the file:

```shell
./gradlew bootRun --args='--server.port=8081'
```

## API

### `GET /hello`

Returns a plain-text greeting.

- **Status:** `200 OK`
- **Content type:** `text/plain`
- **Body:** `Hello, world!`

## Documentation

Background and design notes live in `docs/`:

- [`docs/specs/backend-foundation.md`](docs/specs/backend-foundation.md) — scope, tech stack, and success criteria
- [`docs/plans/backend-foundation.md`](docs/plans/backend-foundation.md) — implementation sequence and risks
- [`docs/tasks/backend-foundation.md`](docs/tasks/backend-foundation.md) — task breakdown
