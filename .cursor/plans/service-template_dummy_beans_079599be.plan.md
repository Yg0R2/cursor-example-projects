---
name: Service-template Dummy Beans
overview: Replace all five `*Placeholder` Kotlin objects in the service-template module with Spring-managed `Dummy*` beans and add a minimal integration test that verifies the application context starts successfully.
todos: []
isProject: false
---

# Service-template: Replace Placeholders with Dummy Beans and Add Integration Test

## Current state

- **Placeholders** (all are `object` singletons, not referenced elsewhere):
  - [service-template/api/.../ServiceApiPlaceholder.kt](service-template/api/src/main/kotlin/com/example/service/api/ServiceApiPlaceholder.kt)
  - [service-template/client/.../ServiceClientPlaceholder.kt](service-template/client/src/main/kotlin/com/example/service/client/ServiceClientPlaceholder.kt)
  - [service-template/persistence/.../ServicePersistencePlaceholder.kt](service-template/persistence/src/main/kotlin/com/example/service/persistence/ServicePersistencePlaceholder.kt)
  - [service-template/service/.../ServicePlaceholder.kt](service-template/service/src/main/kotlin/com/example/service/service/ServicePlaceholder.kt)
  - [service-template/web/.../ServiceWebPlaceholder.kt](service-template/web/src/main/kotlin/com/example/service/web/ServiceWebPlaceholder.kt)
- **Application**: [ServiceTemplateApplication.kt](service-template/application/src/main/kotlin/com/example/service/application/ServiceTemplateApplication.kt) uses `@SpringBootApplication(scanBasePackages = ["com.example.service"])`, so all subpackages are already scanned.
- **Tests**: No tests exist in service-template yet. [auth-service](auth-service/application/src/test/kotlin/com/example/auth/application/LoginLogoutIntegrationTest.kt) uses `@SpringBootTest` and MockMvc; for “service is up and running” we only need context load.

---

## 1. Replace each `*Placeholder` with a `Dummy`* bean

For each of the five files, replace the `object` with a **class** that is a Spring bean:


| Module      | Current                         | New class                 | Stereotype    |
| ----------- | ------------------------------- | ------------------------- | ------------- |
| api         | `ServiceApiPlaceholder`         | `DummyServiceApi`         | `@Component`  |
| client      | `ServiceClientPlaceholder`      | `DummyServiceClient`      | `@Component`  |
| persistence | `ServicePersistencePlaceholder` | `DummyServicePersistence` | `@Repository` |
| service     | `ServicePlaceholder`            | `DummyService`            | `@Service`    |
| web         | `ServiceWebPlaceholder`         | `DummyServiceWeb`         | `@Component`  |


- **Rename file** to match the new type (e.g. `ServiceApiPlaceholder.kt` → `DummyServiceApi.kt`).
- **Implementation**: Each class can be a simple empty (or minimal) class with the chosen stereotype so it is instantiated and registered as a bean. No need for methods unless you want a trivial one to assert injection in the integration test.

Example for API:

```kotlin
package com.example.service.api

import org.springframework.stereotype.Component

@Component
class DummyServiceApi
```

- **Kotlin + Spring**: If the convention plugins do not apply the Kotlin Spring plugin, the **application** module must make bean classes open for proxying. Add `id("org.jetbrains.kotlin.plugin.spring")` to [service-template/application/build.gradle.kts](service-template/application/build.gradle.kts) (same as in auth-service) so that `@Component`/`@Service`/`@Repository` classes are opened by the compiler.

---

## 2. Integration test: “service is up and running”

- **Location**: `service-template/application/src/test/kotlin/com/example/service/application/ServiceTemplateApplicationIntegrationTest.kt` (or similar name).
- **Purpose**: Single test that verifies the Spring context starts with all beans (including the five Dummy* beans). No HTTP, no MockMvc—only context load.
- **Test class**:
  - `@SpringBootTest` (no web environment needed for a context-only test; can use `webEnvironment = NONE` to keep it fast).
  - One test method, e.g. `contextLoads()`, that runs with the default empty body (success = context started).
- **Dependencies**: Add to [service-template/application/build.gradle.kts](service-template/application/build.gradle.kts):
  - `testImplementation("org.springframework.boot:spring-boot-starter-test")`
  - Ensure JUnit 5 is used: `tasks.named<Test>("test") { useJUnitPlatform() }` (if not already provided by the kotlin-conventions plugin).

---

## 3. Optional: Verify beans in the test

To strictly confirm the Dummy* beans are present, the integration test can inject them (e.g. via constructor or `@Autowired` fields) and assert they are non-null. This is optional if “service is up and running” is interpreted only as “context loads”.

---

## Summary of file changes


| Action                 | Path                                                                              |
| ---------------------- | --------------------------------------------------------------------------------- |
| Replace + rename       | `api/.../ServiceApiPlaceholder.kt` → `DummyServiceApi.kt`                         |
| Replace + rename       | `client/.../ServiceClientPlaceholder.kt` → `DummyServiceClient.kt`                |
| Replace + rename       | `persistence/.../ServicePersistencePlaceholder.kt` → `DummyServicePersistence.kt` |
| Replace + rename       | `service/.../ServicePlaceholder.kt` → `DummyService.kt`                           |
| Replace + rename       | `web/.../ServiceWebPlaceholder.kt` → `DummyServiceWeb.kt`                         |
| Add test deps + plugin | `application/build.gradle.kts`                                                    |
| Add test               | `application/src/test/kotlin/.../ServiceTemplateApplicationIntegrationTest.kt`    |


No changes are required to other modules or to `ServiceTemplateApplication.kt`; component scan already covers `com.example.service.`*.