# AGENTS.md

## Project Overview
- Todo application built with Spring Boot.

## Tech Stack
- Backend:
  - Java 17
  - Spring Boot
  - Spring Data JPA
  - MySQL
- Frontend delivery:
  - Vue.js
  - Vite
  - Vue Router
  - Pinia

## Source of Truth
- Database schema: `src/main/resources/sql/schema.sql`
- Functional specification: `docs/features.md`
  
## Architecture
- The project targets a simple monolithic architecture.
- Use package-by-domain structure.
- Must organize code primarily by domain and let each domain grow around its controller, service, repository, and domain model.
- Keep dependencies one-directional where practical: controller -> service -> repository/domain.

## Code Style
- Add concise Korean comments to core business logic.

## Implementation Guidance
- When implementing or changing a feature, complete the implementation and tests in the same work.
- Do not treat implementation as complete until the tests pass.
- Before finalizing code changes, check for obvious mistakes, regressions, or missing updates.

## Testing
- Add unit tests for the service layer and domain logic.
- For controller behavior, request validation, and response mapping, add web tests such as `@WebMvcTest`.
- Tests must include the essential happy paths and only the important edge cases and failure cases.
- Use mocking only for external APIs, infrastructure boundaries, or cases where isolation is otherwise difficult or unnecessary complexity would be introduced.
- Avoid using reflection in tests unless there is no reasonable alternative.
- Add `@DisplayName` to every test method and write the display names in Korean.
- If tests fail, review the cause first and proceed with refactoring only when it is necessary and justified.

## Commit Convention
- Commit messages must follow the Conventional Commits format.
- Use types such as `feat`, `fix`, `refactor`, `test`, `docs`, and `chore` appropriately.
- Write the commit message description in Korean.

## Deployment Strategy
- Keep production deployment simple for the current project size.
- Use a single Spring Boot application instance unless concrete scaling needs appear.
- Package the application with Docker for consistent runtime and deployment.
- For AWS, start with one EC2 instance for the application.
- For production data, prefer AWS RDS MySQL over running MySQL in a container on the same EC2 instance.
- Pass runtime configuration through environment variables or deployment secrets, not committed property files.
- If HTTPS or custom domain setup is needed, place Nginx or another reverse proxy in front of the app on the EC2 instance.

## Deployment Boundaries
- Production:
  - Run the Spring Boot app as a Docker container.
  - Keep the database outside the app container and treat it as persistent infrastructure.
  - Avoid introducing distributed systems, multiple app nodes, or container orchestration before real traffic or operational needs justify them.

## Notes
- `src/main/resources/application.properties` is intended for local or deployment-specific runtime values and should remain ignored by Git.
- Do not commit secrets or production credentials to the repository.
