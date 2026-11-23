# ChatSphere Project Working Rules

These rules are strictly observed across all development phases of the ChatSphere project:

1. **Phase-by-Phase Discipline**: Work on ONE phase at a time. When a phase is done, stop and wait for the user's next prompt. Never start the next phase autonomously.
2. **Pre-Implementation Approval**: Before writing code, present a concise implementation plan (listing files to create or change) and wait for user approval.
3. **Context Inspection**: Inspect existing files, package names, imports, and API contracts before creating or editing anything. Never create duplicates.
4. **Complete Implementations**: Every file must be complete. No TODOs, no placeholder methods, no fake data, and no mock APIs that return success without performing the actual work.
5. **Technical Conventions**:
   - **Backend**: Java 21, latest stable Spring Boot 3.x, Maven wrapper (`mvnw`), base package `com.chatsphere`.
   - **Services**: Use a single `@Service` class per service. Do NOT create interface + `Impl` pairs.
   - **Architecture**: Constructor injection only. Thin controllers. All business logic in services.
   - **Data Transfer & Mapping**: DTOs implemented as Java records. Mappers written manually (no MapStruct). Lombok is reserved strictly for JPA entities.
   - **API Safety & IDs**: Never expose JPA entities from APIs. Use `publicId` (UUID) in all URLs and client payloads, never internal database IDs.
   - **Responses & Errors**: All responses must use the `ApiResponse` structure defined in the specification. Centralized error handling handled exclusively through one `@RestControllerAdvice`.
   - **Database Evolution**: Database changes solely through Flyway migrations (one new versioned migration file per change; never edit an already-applied migration). Create only the tables required for the active phase.
   - **Frontend**: Vite + React JavaScript, Bootstrap 5, Axios service layer, authentication context, custom hooks, and modular component hierarchy (no monolithic `App.jsx`).
6. **Phase Completion Checklist** (Mandatory at the end of every phase):
   a. Run `./mvnw clean verify` (or `mvnw.cmd clean verify` on Windows) and resolve every error.
   b. Run `npm run build` in the frontend and resolve every error.
   c. Start the application and test new endpoints against real MySQL instances (via curl or automated tests).
   d. Write or update tests covering the phase.
   e. Update `docs/development-progress.md` with: completed features, pending features, known bugs, and real test execution results.
   f. Create a git commit with a clear, descriptive message.
   g. Explain in simple words the 5 most important classes of the phase and why they exist.
   h. List exactly what the user should verify manually, then STOP.
7. **Empirical Verification**: Never claim something works without running and verifying it. If something fails or remains uncertain, report it plainly.
8. **Secrets Management**: Never hardcode secrets. Always use environment variables and keep `.env.example` continuously updated.
