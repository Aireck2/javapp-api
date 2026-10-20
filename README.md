# javapp-api

Minimal Java backend built with Spring Boot and Gradle Kotlin DSL.

## Requirements

- JDK 21
- Gradle is not required separately; use the included Gradle wrapper.

## Build and run

```shell
./gradlew clean build
./gradlew test
./gradlew bootRun
```

The application listens on port `8080` by default. The starter endpoint is:

```shell
curl --include http://localhost:8080/hello
```

It returns HTTP 200, `text/plain`, and `Hello, world!`.
