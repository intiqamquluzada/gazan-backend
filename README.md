# Qazan Backend

> Spring Boot 3.4 · Java 21 · PostgreSQL · JWT · 4-language i18n (AZ / EN / RU / TR)

This service backs the **Qazan** mobile loyalty app. It owns user accounts, multi-role authentication (customer / business owner / admin), profile management, and is structured so that loyalty domain modules (companies, programs, cards, promotions, scans) can be added without touching the foundations.

---

## Architecture

Modular monolith with feature folders. Each feature module follows the same internal layout — domain, application, api — so the codebase scales horizontally without architectural drift.

```
az.qazan.backend
├── config/            ← cross-cutting Spring configuration
├── common/
│   ├── api/           ← envelope types (ApiResponse)
│   ├── audit/         ← BaseEntity + JPA auditing
│   ├── exception/     ← ErrorCode, AppException tree, GlobalExceptionHandler
│   ├── i18n/          ← MessageService
│   └── security/      ← JWT primitives + AppUserPrincipal + @CurrentUser
├── auth/
│   ├── domain/        ← RefreshToken + repository
│   ├── application/   ← AuthService (register, login, refresh, logout)
│   └── api/           ← AuthController + DTOs
└── user/
    ├── domain/        ← User, Role, AppLocale, repository
    ├── application/   ← UserService, UserMapper
    └── api/           ← UserController + DTOs (UserResponse, UpdateProfileRequest, ChangePasswordRequest)
```

**Why this shape**

- *domain* knows nothing about HTTP. *application* coordinates domain mutations inside a transaction. *api* is the only layer touching `@RestController`, validation, and DTOs.
- Cross-feature boundaries cross only through service classes — never repositories. Adding a new feature means a new top-level package, not edits to existing ones.
- Authentication is **stateless JWT**. Refresh tokens are *opaque* random strings persisted in `refresh_tokens` so individual sessions can be revoked. Every refresh **rotates** — the presented token is invalidated and a fresh pair is issued.
- Errors are localized. Every domain exception carries an `ErrorCode`; the global handler turns it into `ApiError` whose `message` is resolved via Spring's `MessageSource` against the request locale.

---

## API at a glance

| Method | Path                              | Role          | What it does                              |
| ------ | --------------------------------- | ------------- | ----------------------------------------- |
| POST   | `/api/v1/auth/register`           | public        | Create account, returns token pair        |
| POST   | `/api/v1/auth/login`              | public        | Issue token pair                          |
| POST   | `/api/v1/auth/refresh`            | public        | Rotate refresh token                      |
| POST   | `/api/v1/auth/logout`             | public        | Revoke a single refresh token             |
| POST   | `/api/v1/auth/logout-all`         | authenticated | Revoke every active session for the user  |
| GET    | `/api/v1/users/me`                | authenticated | Read profile                              |
| PUT    | `/api/v1/users/me`                | authenticated | Edit profile (name, phone, avatar, locale, business name) |
| POST   | `/api/v1/users/me/change-password`| authenticated | Change password (current + new)           |
| DELETE | `/api/v1/users/me`                | authenticated | Soft-deactivate the account               |

Interactive docs at **`http://localhost:8080/docs`** when running.

### Auth response shape

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "accessExpiresAt": "2026-05-06T08:30:00Z",
  "refreshToken": "Xa9mB2-0p...",
  "user": {
    "id": "9c3e...",
    "email": "ali@example.com",
    "fullName": "Ali Aghazada",
    "role": "CUSTOMER",
    "locale": "AZ",
    "active": true,
    "createdAt": "2026-05-06T08:15:00Z"
  }
}
```

### Error response shape

```json
{
  "code": "AUTH_INVALID_CREDENTIALS",
  "message": "E-poçt və ya şifrə yanlışdır.",
  "status": 401,
  "path": "/api/v1/auth/login",
  "timestamp": "2026-05-06T08:15:00Z",
  "fields": null
}
```

The `code` is stable — the mobile client switches on it. The `message` reflects the request's resolved locale.

---

## Internationalization

The mobile app sets **`Accept-Language`** on every request. The locale resolver picks the first supported tag from `az,en,ru,tr` and falls back to `app.default-locale` (default `az`).

Translations live in `src/main/resources/i18n/messages_<lang>.properties`. Adding a new key is a one-line change in each of the four bundles. Validation messages in DTO annotations reference the same keys (e.g. `{validation.email.format}`), so a single source of truth covers both error responses and bean-validation feedback.

---

## Running locally

### 1. Start Postgres

```bash
docker compose up -d
```

### 2. Run the app

```bash
./gradlew bootRun
```

First boot runs Flyway migrations; the schema is empty otherwise.

### 3. Smoke test

```bash
# Register
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -H "Accept-Language: az" \
  -d '{
    "email":"ali@example.com",
    "password":"password123",
    "fullName":"Ali Aghazada",
    "role":"CUSTOMER",
    "locale":"AZ"
  }'

# Save the access token from the response, then:
curl http://localhost:8080/api/v1/users/me \
  -H "Authorization: Bearer <accessToken>" \
  -H "Accept-Language: az"
```

### 4. Swagger UI

Open `http://localhost:8080/docs` and use the **Authorize** button to paste a Bearer token.

---

## Configuration reference

All settings can be overridden via environment variables.

| Property                  | Env var               | Default                                  | Notes                                  |
| ------------------------- | --------------------- | ---------------------------------------- | -------------------------------------- |
| `spring.datasource.url`   | `DB_URL`              | `jdbc:postgresql://localhost:5432/qazan` |                                        |
| `spring.datasource.username` | `DB_USER`          | `qazan`                                  |                                        |
| `spring.datasource.password` | `DB_PASSWORD`      | `qazan`                                  |                                        |
| `app.jwt.secret`          | `JWT_SECRET`          | dev-only string                          | **MUST replace in prod — ≥256 bits**   |
| `app.jwt.access-token-ttl`|                       | 15 min                                   | ISO-8601 duration                      |
| `app.jwt.refresh-token-ttl`|                      | 30 days                                  |                                        |
| `app.cors.allowed-origins`| `CORS_ORIGINS`        | `http://localhost:3000,…`                | Comma-separated                        |
| `app.default-locale`      |                       | `az`                                     | Used when `Accept-Language` missing    |
| `server.port`             | `SERVER_PORT`         | `8080`                                   |                                        |

---

## Adding a new feature module

When loyalty / promotions / etc. land, follow the same layout the user module already uses:

```
features/companies/
├── domain/         — Company entity, BusinessCategory, repo
├── application/    — CompanyService, CompanyMapper
└── api/            — CompanyController, DTOs
```

Add a Flyway migration `V<N>__add_companies.sql` next to V1. No edits to existing files.

---

## Testing

```bash
./gradlew test
```

Tests use H2 in PostgreSQL-compatibility mode with `ddl-auto=create-drop` so they don't need Postgres. Production startup uses Flyway with strict `validate`.

---

## What's intentionally not done yet

- Email verification (registration is one-step)
- Password reset flow (separate ticket; skeleton hooks already in `AuthService`)
- Rate limiting on `/auth/login` (Bucket4j or Resilience4j)
- Avatar upload (storage choice — S3 vs local — pending)
- Audit columns `createdBy` / `updatedBy` (the auditor bean is already wired, just add the columns)

These are deliberately scoped out of the foundation and slot into the existing structure cleanly when needed.
