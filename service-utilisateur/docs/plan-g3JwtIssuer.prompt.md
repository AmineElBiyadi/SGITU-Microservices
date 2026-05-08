# Plan: G3 becomes the JWT issuer (as per professor's feedback)

Currently G3 **validates** tokens issued by G10. The professor says G3 should be the **single source of truth** — including for authentication — meaning **G3 must issue the JWT**, and G10 should only validate and route it.

## Steps

1. **Add `JwtService`** in `security/` — responsible for generating signed JWT tokens (`subject=email`, `claims=roles`, `expiry=configurable`), using the existing `jwt.secret` from `application.properties`.

2. **Add `AuthController`** — expose `POST /api/auth/login` (public, no token required): receives `{ email, password }`, validates credentials against the DB via `UserServiceImpl`, and returns `{ token, userId, roles }` using `JwtService`.

3. **Update `SecurityConfig`** — add `POST /auth/login` to the `permitAll()` list.

4. **Remove or repurpose `GET /users/internal/credentials`** — G10 no longer needs to fetch the password hash since G3 handles login directly. Remove the endpoint (and `CredentialsResponseDTO`, `InternalKeyFilter`) or keep it locked down as a fallback.

5. **Update `class-diagram.puml`** and comments — reflect that G3 now issues JWTs; update the note on `JwtFilter` to say *"issues and validates"*; add `AuthController` and `JwtService`.

6. **Coordinate with G10** — tell them their login flow changes from *"call G3 `/internal/credentials` → validate password → issue JWT"* to *"forward login request to G3 `/auth/login` → receive JWT → return to client"*. G10 still validates the JWT signature on all subsequent requests using the shared `jwt.secret`.

## Further Considerations

1. **Keep or drop `InternalKeyFilter`?** — If no inter-service call still needs it (after removing `/internal/credentials`), it can be removed to simplify the security chain. Otherwise keep it for future internal endpoints.
2. **Token expiry config** — Add `jwt.expiration=86400` (seconds) to `application.properties` so it's tunable without touching code.
3. **Inform G10 immediately** — This is a breaking change for their current flow; they need to know before your next Docker Compose integration test.

## Context

- Service: G3 — Gestion des utilisateurs (User & Profile Management)
- Stack: Java 21, Spring Boot 3.x, Spring Security 6.x, JJWT, PostgreSQL
- Current `jwt.secret` is already set in `application.properties` and shared with G10
- Current `JwtFilter` already parses tokens with JJWT — reuse the same key logic in `JwtService` for generation
- `InternalKeyFilter` currently protects `/users/internal/**` via `X-Internal-Key` header
- `CredentialsResponseDTO` carries: `id`, `email`, `passwordHash`, `roles`, `active`
- G8 event integration (`UserEventPublisher`) is unaffected by this change

