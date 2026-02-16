# Copilot Instructions — Continuum

This file captures the minimal, repo-specific knowledge an AI coding agent needs to be productive.

## Big picture (short)
- Continuum is a distributed workflow engine: frontend (Theia/React) + backend (Kotlin/Spring Boot workers) + durable orchestration (Temporal) + event bus (Kafka) + lightweight pub/sub (Mosquitto/MQTT).
- Data contracts are Avro schemas (single source of truth in `continuum-avro-schemas`). Frontend is a Turbo/Yarn monorepo under `continuum-frontend`.

## Where to look first (quick paths)
- Repo root README: [README.md](README.md)
- Local dev compose: [docker/docker-compose.yml](docker/docker-compose.yml)
- Avro schemas & Gradle plugin: [continuum-avro-schemas/build.gradle.kts](continuum-avro-schemas/build.gradle.kts)
- Frontend monorepo scripts: [continuum-frontend/package.json](continuum-frontend/package.json)
- Backend API module: [continuum-api-server](continuum-api-server)

## Concrete developer workflows & commands
- Full backend build & tests (root):
  - `./gradlew build` (runs all Gradle modules)
  - `./gradlew :continuum-api-server:test` (run a specific module)
- Frontend (monorepo):
  - `cd continuum-frontend && yarn` to install
  - `yarn build` — builds all packages via `turbo` (see `scripts` in `continuum-frontend/package.json`)
  - Dev (watch): `yarn dev` from `continuum-frontend` (Turbo dev mode)
- Local services (Temporal, Kafka, Mosquitto, Postgres):
  - `cd docker && docker-compose up -d`
  - Temporal admin tools are available as `temporal-admin-tools` container; use it to create search attributes (examples below).

## Temporal / admin examples
- Create required search attributes (used in CI/local dev):
  - docker exec examples (run from host):
    ```bash
    docker exec -it temporal-admin-tools temporal operator search-attribute create --name "Continuum:ExecutionStatus" --type "Int"
    docker exec -it temporal-admin-tools temporal operator search-attribute create --name "Continuum:WorkflowFileName" --type "Keyword"
    ```

## Project-specific conventions & patterns
- Avro-first data contracts: modify schemas in `continuum-avro-schemas/src/main/avro`, then run Gradle — generated code appears under `build/generated-main-avro-java` for consumers.
- Backend modules share utility code in `continuum-commons`; prefer reusing shared types rather than duplicating DTOs.
- Frontend uses workspace packages (see `workspaces` in `continuum-frontend/package.json`). To work on a single UI package use `--filter=@continuum/<package>` with `turbo` scripts.
- Node engine: `>=20` (see `engines` in `continuum-frontend/package.json`); package manager: `yarn@1.22.21`.

## Integration points to be aware of
- Temporal: durable workflow engine (RPC on port 7233 in `docker-compose`). `continuum-springboot-starter-worker` contains worker wiring.
- Kafka + Schema Registry: topics + Avro schemas for events; schema-registry container is defined in `docker/docker-compose.yml`.
- MQTT / Mosquitto: lightweight pub/sub for live UI/telemetry (container `mosquitto` in compose).
- S3/MinIO: object storage is expected for node artifacts (see README notes about S3/MinIO).

## Files that encode important configuration and examples
- Docker compose (local infra): [docker/docker-compose.yml](docker/docker-compose.yml)
- Avro Gradle config: [continuum-avro-schemas/build.gradle.kts](continuum-avro-schemas/build.gradle.kts)
- Frontend turbo scripts: [continuum-frontend/package.json](continuum-frontend/package.json)
- Frontend models: examples under `continuum-core/src/model` (e.g. Mime types and `Workflow.ts`) show shared types and node model patterns.

## Tasks AI agents are commonly asked to perform (how to proceed)
1. Small change in a backend module: run `./gradlew :<module>:build` and `./gradlew :<module>:test` locally. Run the service with `spring-boot:run` if needed.
2. Frontend UI tweak: `cd continuum-frontend && yarn dev` then use `turbo start --filter=@continuum/<package>` for single-package dev.
3. Add/change Avro schema: edit `src/main/avro/*.avdl|.avsc`, run `./gradlew :continuum-avro-schemas:build`, then update consumers' imports.

## What *not* to assume
- There is only one active worker in some setups — multi-worker orchestration is a planned goal. Don't assume dynamic worker discovery is fully implemented unless you confirm code in `continuum-springboot-starter-worker`.
- Tests and frontend e2e are not standardized across packages; run unit tests first before attempting broad CI changes.

## Quick checklist before PRs
- Run `./gradlew build` (or module-specific builds) and `yarn build` in `continuum-frontend`.
- If you changed Avro, verify generated classes under `continuum-avro-schemas/build/generated-main-avro-java` and update dependent modules' imports.
- If you touch infra assumptions, update `docker/docker-compose.yml` and README notes about Temporal search attributes.

---
If anything below is unclear or you want this expanded into per-module agent rules, tell me which modules or workflows to document next.
