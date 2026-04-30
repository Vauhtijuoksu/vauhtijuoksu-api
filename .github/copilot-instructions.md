# GitHub Copilot Instructions for vauhtijuoksu-api

This document helps Copilot understand the structure and conventions of this Kotlin/Gradle project.

## Project Overview

**vauhtijuoksu-api** is a Kotlin-based REST API server built with:
- **Framework**: Vert.x (async, event-driven HTTP framework)
- **Language**: Kotlin
- **Build Tool**: Gradle
- **Database**: PostgreSQL with Flyway migrations
- **Authentication**: Basic auth (htpasswd) and OAuth2
- **Deployment**: Kubernetes (via kind locally, Helm for charts)

### Multi-Project Structure
```
server/          → Main REST API server (Vert.x)
models/          → Data models (DTOs, shared interfaces)
database-api/    → Database abstraction interfaces
database/        → Database implementation & migrations
api-doc/         → OpenAPI/Swagger documentation generation
feature-tests/   → Integration tests running against k8s cluster
deployment/      → K8s manifests, Helm values, cluster config
test-data/       → Test data generators
buildSrc/        → Gradle plugin conventions & utilities
```

## Build, Test, and Lint Commands

### Running Tests
```bash
# Fast unit/integration tests (excludes slow feature tests)
./gradlew test -x feature-tests:test

# Full test suite including feature tests (requires k8s cluster)
./gradlew build

# Run tests for a specific subproject
./gradlew :server:test

# Run a single test class
./gradlew :server:test --tests "fi.vauhtijuoksu.vauhtijuoksuapi.server.MyTestClass"

# Run a single test method
./gradlew :server:test --tests "fi.vauhtijuoksu.vauhtijuoksuapi.server.MyTestClass.myTestMethod"
```

### Code Quality
```bash
# Lint with detekt (static analysis)
./gradlew detekt

# Format with ktlint (auto-fix via spotless)
./gradlew spotlessApply

# Check formatting without applying
./gradlew spotlessCheck
```

### Building & Documentation
```bash
# Build everything
./gradlew build

# Generate API documentation (Swagger UI)
./gradlew :api-doc:build
# Output: api-doc/build/swagger-ui/index.html

# Print project version (from git tag)
./scripts/version.sh
```

### Local Development (K8s Cluster)
```bash
# Start a local kind cluster with API server
./gradlew runInCluster
# API available at: http://api.localhost (or https with self-signed cert)
# Default credentials: vauhtijuoksu:vauhtijuoksu

# Start mock API server (based on OpenAPI spec)
./gradlew localMockApi
# Available at: http://mockapi.localhost

# Teardown cluster
./gradlew tearDownCluster
```

### Test Reporting
```bash
# Coverage report from feature tests (generated automatically during build)
# Location: build/reports/jacoco/featureTestReport/html/index.html

# Jacotest report for a subproject
./gradlew :server:jacocoTestReport
# Location: server/build/reports/jacoco/test/html/index.html
```

## High-Level Architecture

### API Layer (server/)
- **Main entry**: `Server.kt` starts Vert.x HTTP server
- **Routing**: REST endpoints registered via `Router` extensions (Vert.x pattern)
- **Auth**: `AuthModule.kt` configures both Basic and OAuth2 via Guice DI
- **CORS**: Handlers configured per route via `DependencyInjectionConstants`
- **Roles**: Enum-based role system (`Roles.kt`), enforced via `RoleBasedAuthorization`

### Data Layer
- **Database**: PostgreSQL accessed via Vert.x PG client (async, non-blocking)
- **Migrations**: Flyway in `database/src/main/resources/db/migration/`
- **Interfaces**: `database-api/` defines data access contracts
- **Implementation**: `database/` implements interfaces using PG client templates

### Models
- **DTOs**: Located in `models/`, use for API request/response serialization
- **Serialization**: Configured via Jackson (with Kotlin module) and kotlinx-serialization
- **JSON**: Jackson ObjectMapper handles DateTime (JSR-310) and Kotlin types

### Feature Tests
- **Pattern**: Use `@FeatureTest` annotation + JUnit 5 `@Test` methods
- **HTTP Client**: `WebClient` injected via `FeatureTestUtils` parameter resolver
- **Context**: `VertxTestContext` for async assertions
- **Credentials**: Basic auth via `UsernamePasswordCredentials` 
- **Test Ordering**: Use `@TestMethodOrder` for sequential test execution

### Deployment & Infrastructure
- **Kind Cluster**: Local K8s via `deployment/kind-cluster/kind-cluster-config.yaml`
- **Ingress Controller**: Traefik (installed via Helm during cluster setup)
- **Helm Charts**: Values in `deployment/kind-cluster/*-values.yaml` and `deployment/kind-cluster/traefik-values.yaml`
- **Docker**: Multi-stage Dockerfile in `server/` for production image
- **Secrets**: OAuth and DB credentials managed via K8s secrets

## Key Conventions

### Gradle Build System
1. **Convention Plugins** (in buildSrc/):
   - `vauhtijuoksu-api.common-conventions` → base config (ktlint, kotlin)
   - `vauhtijuoksu-api.kotlin-conventions` → adds detekt, test config
   - `vauhtijuoksu-api.implementation-conventions` → adds Jacoco coverage

2. **Project Application**:
   - `models/` and `database-api/` use `id("vauhtijuoksu-api.kotlin-conventions")`
   - `server/` and `database/` use `id("vauhtijuoksu-api.implementation-conventions")`
   - Custom plugins allow zero-boilerplate configuration across subprojects

3. **Dependency Management**:
   - All versions defined in `gradle/libs.versions.toml` (TOML catalog)
   - BOM (Bill of Materials) used for consistency: Jackson, JUnit, Mockito, TestContainers
   - Versions aligned across ecosystem (Kotlin stdlib, Jackson, Vert.x)

### Code Style & Linting
1. **Formatting**: ktlint via Spotless (run `./gradlew spotlessApply` to auto-fix)
2. **Static Analysis**: Detekt with strict `maxIssues: 0` policy
3. **Target**: JVM 21 (configured in `vauhtijuoksu-api.kotlin-conventions`)
4. **Reproducibility**: JAR file ordering and timestamps normalized for consistency

### Kotlin Patterns
1. **Singletons**: Use `private constructor()` + `companion object` (see `ApiConstants.kt`)
2. **Dependency Injection**: Google Guice for constructor and provider-based injection
3. **Async**: Vert.x Futures and coroutines (not callbacks/nested futures)
4. **Serialization**: Jackson for JSON (with date/time support), kotlinx-serialization for some models
5. **Extension Functions**: Used in convention plugins (e.g., `bashCommand()`)

### Testing Strategy
1. **Unit Tests**: Use Mockito for mocking, JUnit 5 for assertions
2. **Integration Tests**: TestContainers for PostgreSQL in `server/` tests
3. **Feature Tests**: Full stack integration against k8s cluster, async via `VertxTestContext`
4. **Ordering**: Feature tests can use `@Order` and `@TestMethodOrder` to enforce sequence
5. **Credentials**: Mock OAuth via `mock-oauth2-server` library

### Database
1. **Migrations**: Flyway with SQL files (naming: `V{number}__{description}.sql`)
2. **Queries**: Vert.x SQL templates (parameterized queries, type-safe)
3. **Async**: All DB calls non-blocking via `Future` composition
4. **Auth**: OAuth secrets in K8s, copied to file for OAuth2 provider config

### API Design
1. **Response Codes**: Constants in `ApiConstants.kt` (200, 201, 204, 400, 404, 500)
2. **Error Ranges**: `USER_ERROR_CODES = 400..<500` for client error checks
3. **Authorization**: Role-based checks via `RoleBasedAuthorization` in handlers
4. **CORS**: Different handlers for public vs authenticated routes (via DI)

### Naming Conventions
- **Package**: `fi.vauhtijuoksu.vauhtijuoksuapi.{module}` (e.g., `.server`, `.featuretest`)
- **Classes**: PascalCase, constants via `companion object`
- **Files**: Match primary class name
- **Test Classes**: Suffix with `Test` (e.g., `BasicTest.kt`)
- **Test Methods**: Descriptive action-based (e.g., `testServerResponds()`)

## Important Notes
- **Versioning**: Git tags are the source of truth (used by version.sh script)
