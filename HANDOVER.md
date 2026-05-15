# Qazan — Layihə Handover Sənədi

> Sadiqlik və müştəri izləmə platforması — kafelər, restoranlar, gözəllik salonları, avtoyumalar üçün. Müştərilər QR kodu göstərir, biznes skan edir, möhürlər və mükafatlar avtomatik izlənir.

---

## 1. Layihə strukturu

İki ayrı qovluq, eyni `~/Desktop/` altında:

```
~/Desktop/
├── gazan-mobile/   ← Flutter app (iOS + Android + web + macOS)
└── gazan-backend/  ← Spring Boot 3.4 + PostgreSQL + JWT
```

**Adı:** Qazan (azərbaycanca "qazan/win")
**Brend rəng:** vibrant orange (#FF6F3C) + accent teal (#2EC4B6)
**Default mövzu:** light
**Tətbiq dilləri:** AZ (default), EN, RU, TR

---

## 2. Texnologiya seçimi

### Mobile

| Seçim | Niyə |
|---|---|
| Flutter 3.27+ | iOS, Android, web, macOS — bir codebase |
| Riverpod 2.x | DI + state, code-gen yox |
| go_router 14 | Deklarativ routing, redirect logic |
| dio 5 | HTTP client, interceptors |
| shared_preferences (web) + flutter_secure_storage (native) | Token saxlama (HTTPS olmayanda WebCrypto problemi yaranmır) |
| qr_flutter + mobile_scanner | QR göstər/skan |
| google_fonts (Inter) | Tipoqrafiya |

### Backend

| Seçim | Niyə |
|---|---|
| Java 21 + Spring Boot 3.4.1 | LTS, stabil |
| Gradle (Groovy DSL) | İstifadəçinin tanıdığı tool |
| Spring Security + JJWT 0.12 | Stateless JWT auth |
| Spring Data JPA + Hibernate | ORM |
| PostgreSQL 16 | Relational DB |
| Flyway 10 | Schema migrations |
| Springdoc OpenAPI 2.7 | Auto Swagger UI |
| Lombok | Entity boilerplate |
| Records (Java 21) | DTO-lar |

---

## 3. Mobile arxitektura

Clean architecture, hər feature öz `domain / data / application / presentation` qatlarında.

```
gazan-mobile/lib/
├── main.dart                     # ProviderScope + intl init
├── app.dart                      # MaterialApp.router + theme + locale
├── routing/
│   └── app_router.dart           # go_router config + redirect məntiqi
│
├── core/                         # feature-larə bağlı olmayan
│   ├── theme/
│   │   ├── app_colors.dart
│   │   ├── app_spacing.dart      # 4-pt baseline scale
│   │   ├── app_text_styles.dart
│   │   ├── app_theme.dart        # Material 3 light + dark
│   │   └── theme_mode_controller.dart
│   ├── constants/app_strings.dart
│   ├── utils/{extensions, formatters}.dart
│   ├── widgets/                  # PrimaryButton, AppTextField, EmptyState, Avatar, SectionHeader
│   ├── config/app_config.dart    # API base URL, timeouts
│   ├── storage/token_storage.dart   # web=SharedPreferences, native=secure
│   └── network/
│       ├── api_client.dart       # Dio + ApiException mapping + providers
│       ├── api_exception.dart
│       └── interceptors/
│           ├── auth_interceptor.dart    # Bearer token
│           └── locale_interceptor.dart  # Accept-Language
│
└── features/
    ├── auth/                     # splash, onboarding, role picker, login, register
    │   ├── domain/{user_role, app_user, role_mapping}.dart
    │   ├── application/auth_controller.dart   # session restore + signIn/Up/Out
    │   ├── data/auth_remote_data_source.dart  # /auth/* endpoints
    │   └── presentation/{splash, onboarding, role_picker, login, register}_screen.dart
    │
    ├── companies/                # discover feed + company detail
    │   ├── domain/{business_category, company}.dart
    │   ├── data/companies_repository.dart    # RemoteCompaniesRepository
    │   ├── application/companies_providers.dart
    │   └── presentation/
    │       ├── discover_screen.dart
    │       ├── company_detail_screen.dart
    │       └── widgets/{company_card, category_chips}.dart
    │
    ├── loyalty/                  # programs, cards, scan
    │   ├── domain/{loyalty_program, loyalty_card, loyalty_event}.dart
    │   ├── data/loyalty_repository.dart      # RemoteLoyaltyRepository (CRUD + scan)
    │   ├── application/loyalty_providers.dart
    │   └── presentation/
    │       ├── my_cards_screen.dart
    │       └── widgets/{loyalty_card_widget, stamp_grid}.dart
    │
    ├── promotions/               # stories + ad banners
    │   ├── domain/{story, promotion}.dart
    │   ├── data/promotions_repository.dart   # RemotePromotionsRepository
    │   ├── application/promotions_providers.dart
    │   └── presentation/
    │       ├── story_viewer_screen.dart      # Instagram-style: progress bars, tap, swipe
    │       └── widgets/{stories_strip, promotions_carousel}.dart
    │
    ├── qr/
    │   └── presentation/
    │       ├── qr_display_screen.dart        # müştəri tərəf
    │       └── qr_scanner_screen.dart        # biznes tərəf — proqram seç, /scans çağır
    │
    ├── profile/                  # /me + interaktiv parametrlər
    │   ├── application/profile_settings_controller.dart  # notif, language
    │   ├── data/profile_remote_data_source.dart
    │   └── presentation/
    │       ├── profile_screen.dart
    │       └── sheets/
    │           ├── _sheet_handle.dart
    │           ├── edit_profile_sheet.dart
    │           ├── notifications_sheet.dart
    │           ├── language_sheet.dart       # AZ/EN/RU/TR
    │           ├── theme_sheet.dart          # Light/Dark/System
    │           ├── security_sheet.dart       # şifrə dəyiş + biometric switch
    │           └── help_sheet.dart           # FAQ
    │
    ├── business/                 # biznes paneli
    │   ├── domain/customer_summary.dart
    │   ├── data/business_repository.dart     # RemoteBusinessRepository (stats)
    │   ├── application/business_providers.dart
    │   └── presentation/
    │       ├── business_dashboard_screen.dart
    │       ├── customers_list_screen.dart
    │       └── manage_programs_screen.dart   # CRUD + reward type picker + live preview
    │
    └── home/
        └── presentation/
            ├── customer_shell.dart           # bottom nav: Kəşf et / Kartlarım / QR / Profil
            └── business_shell.dart           # bottom nav: Panel / Skan / Müştərilər / Proqramlar
```

### Routing xəritəsi (`lib/routing/app_router.dart`)

| Yol | Rol | Səhifə |
|---|---|---|
| `/splash` | public | açılış (1.1 sn auto → /onboarding) |
| `/onboarding` | public | 3 slaydlıq onboarding |
| `/role` | public | Müştəri / Biznes seçimi |
| `/login?role=customer\|business` | public | Daxil ol |
| `/register?role=customer\|business` | public | Qeydiyyat |
| `/home` | customer | Discover (Stories + Reklamlar + Categories + Companies) |
| `/cards` | customer | Sadiqlik kartlarım |
| `/qr` | customer | QR kodum (qr_flutter ilə yaradılır) |
| `/profile` | customer | Profil + 6 sheet |
| `/companies/:id` | iki tərəf | Obyekt detalı + sadiqlik proqramları |
| `/stories/:id` | customer | Full-screen story viewer |
| `/business` | business | Dashboard (stats + skan CTA + son müştərilər) |
| `/business/scan` | business | QR scanner (mobile_scanner) |
| `/business/customers` | business | Müştəri siyahısı |
| `/business/programs` | business | Proqram CRUD + active/passive switch |

### Sadiqlik mükafat növləri

```dart
enum LoyaltyRewardType {
  freeItem,            // "1 pulsuz qəhvə"
  percentageDiscount,  // "burger üzrə 50% endirim"
  fixedDiscount,       // "5 ₼ endirim"
  cashback,            // "5 ₼ cashback"
}
```

Biznes sahibi proqram yaradanda:
1. Adı, açıqlama
2. Mükafat növü (4 chip)
3. Faiz/məbləğ (sadəcə discount/cashback üçün)
4. Hansı məhsul üçün
5. Stamps required (1-30, ± stepper)
6. **Canlı önbaxış** — formanı doldurduqca card preview yenilənir

---

## 4. Backend arxitektura

Modulyar monolit. Hər feature modulu eyni daxili layout-u izləyir: `domain / application / api`. Yeni feature əlavə etmək = yeni top-level paket, mövcud kodda dəyişiklik yox.

```
gazan-backend/src/main/java/az/qazan/backend/
├── QazanBackendApplication.java
├── config/
│   ├── SecurityConfig.java       # stateless JWT, CORS, public/protected paths
│   ├── WebConfig.java            # Accept-Language locale resolver
│   ├── OpenApiConfig.java        # Swagger Bearer scheme
│   └── PasswordEncoderConfig.java   # BCrypt cost 12
│
├── common/
│   ├── api/{ApiResponse, PageResponse}.java
│   ├── audit/{BaseEntity, JpaAuditingConfig}.java   # UUID, createdAt, version
│   ├── i18n/MessageService.java
│   ├── exception/
│   │   ├── ErrorCode.java        # enum, hər error üçün stable kod + i18n key
│   │   ├── AppException.java + alt class-lar (NotFound, Conflict, Unauthorized, BadRequest)
│   │   ├── ApiError.java         # RFC 7807-inspired record
│   │   └── GlobalExceptionHandler.java
│   └── security/
│       ├── JwtProperties.java    # @ConfigurationProperties("app.jwt")
│       ├── JwtTokenProvider.java # HS256 issue + parse
│       ├── JwtAuthenticationFilter.java
│       ├── AppUserPrincipal.java
│       └── CurrentUser.java      # @AuthenticationPrincipal alias
│
├── auth/
│   ├── domain/{RefreshToken, RefreshTokenRepository}.java
│   ├── application/AuthService.java   # register/login/refresh/logout/logoutAll, rotation
│   └── api/
│       ├── AuthController.java
│       └── dto/{Register, Login, Refresh, Auth}Request/Response.java
│
├── user/
│   ├── domain/{User, Role, AppLocale, UserRepository}.java
│   ├── application/{UserService, UserMapper}.java
│   └── api/
│       ├── UserController.java
│       └── dto/{UserResponse, UpdateProfile, ChangePassword}Request.java
│
├── companies/
│   ├── domain/{BusinessCategory, Company, CompanyRepository}.java
│   ├── application/{CompanyService, CompanyMapper}.java
│   └── api/
│       ├── CompanyController.java
│       └── dto/{CompanyResponse, CreateCompany, UpdateCompany}Request.java
│
├── loyalty/
│   ├── domain/
│   │   ├── LoyaltyRewardType.java
│   │   ├── LoyaltyProgram.java
│   │   ├── LoyaltyCard.java
│   │   ├── LoyaltyEvent.java     # STAMP_ADDED / REWARD_CLAIMED
│   │   └── 3 repositories
│   ├── application/
│   │   ├── LoyaltyProgramService.java
│   │   ├── LoyaltyCardService.java   # joinProgram, addStamp, redeem, scan
│   │   └── LoyaltyMapper.java
│   └── api/
│       ├── LoyaltyProgramController.java
│       ├── LoyaltyCardController.java
│       ├── ScanController.java
│       └── dto/...
│
├── promotions/
│   ├── domain/{Story, Promotion + 2 repositories}
│   ├── application/{PromotionsService, PromotionsMapper}.java
│   └── api/
│       ├── PromotionsController.java
│       └── dto/{StoryResponse, StoryGroupResponse, PromotionResponse}.java
│
├── business/
│   ├── application/BusinessStatsService.java
│   └── api/
│       ├── BusinessStatsController.java
│       └── dto/BusinessStatsResponse.java
│
└── seeder/
    └── DevDataSeeder.java        # @Profile("dev") — açılışda mock data yükləyir
```

Resources:

```
src/main/resources/
├── application.yml               # əsas config (dev profile default)
├── application-dev.yml           # debug log, formatted SQL
├── db/migration/
│   ├── V1__init_schema.sql       # users + refresh_tokens
│   └── V2__loyalty_companies_promotions.sql   # 6 cədvəl
└── i18n/
    ├── messages.properties       # default = AZ
    ├── messages_az.properties
    ├── messages_en.properties
    ├── messages_ru.properties
    └── messages_tr.properties
```

### REST API (15 endpoint)

#### Auth (5)
- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh` (rotation)
- `POST /api/v1/auth/logout`
- `POST /api/v1/auth/logout-all` (auth)

#### User profile (4)
- `GET /api/v1/users/me`
- `PUT /api/v1/users/me`
- `POST /api/v1/users/me/change-password`
- `DELETE /api/v1/users/me` (soft deactivate)

#### Companies (6)
- `GET /api/v1/companies?category=&q=`
- `GET /api/v1/companies/featured`
- `GET /api/v1/companies/{id}`
- `GET /api/v1/companies/me` (owner)
- `POST /api/v1/companies` (owner)
- `PUT /api/v1/companies/{id}` (owner)
- `DELETE /api/v1/companies/{id}` (owner)

#### Loyalty programs (5)
- `GET /api/v1/companies/{cid}/programs?activeOnly=`
- `GET /api/v1/programs/{id}`
- `POST /api/v1/companies/{cid}/programs` (owner)
- `PUT /api/v1/programs/{id}` (owner)
- `DELETE /api/v1/programs/{id}` (owner)

#### Loyalty cards (3)
- `GET /api/v1/loyalty/cards/me`
- `POST /api/v1/loyalty/cards/me` (join program)
- `POST /api/v1/loyalty/cards/{id}/redeem`

#### Scans (1)
- `POST /api/v1/scans` (business owner — adds stamp to customer's card)

#### Promotions (3)
- `GET /api/v1/stories` (grouped by company)
- `GET /api/v1/companies/{cid}/stories`
- `GET /api/v1/promotions`

#### Business (1)
- `GET /api/v1/business/stats` (owner dashboard)

### DB cədvəlləri

| Cədvəl | Açıqlama |
|---|---|
| `users` | id, email (uniq), password_hash, full_name, phone, avatar_url, business_name, role (CUSTOMER/BUSINESS_OWNER/ADMIN), locale (AZ/EN/RU/TR), active, last_login_at |
| `refresh_tokens` | opaque value, user_id, expires_at, revoked_at, user_agent, ip — rotation strategy |
| `companies` | name, tagline, category, logo_emoji, cover_color_hex, address, rating, review_count, is_featured, owner_id |
| `loyalty_programs` | company_id, title, description, stamps_required, reward_type, reward_value, reward_item, expires_at, active |
| `loyalty_cards` | user_id, program_id, company_id, stamps, stamps_required (snapshot), rewards_available, total_rewards_claimed, last_activity_at + uniq(user_id, program_id) |
| `loyalty_events` | card_id, type (STAMP_ADDED/REWARD_CLAIMED), amount, note |
| `stories` | company_id, headline, body, emoji, gradient hexes, cta, duration_seconds, active, sort_order |
| `promotions` | company_id (nullable), tag, title, subtitle, emoji, gradient hexes, cta, ends_at, active, sort_order |

Hər cədvəldə `BaseEntity`-dən: `id (UUID)`, `created_at`, `updated_at`, `version (optimistic lock)`.

---

## 5. i18n (4 dil)

- Backend: `MessageService` Spring `MessageSource` üzərində; `Accept-Language` header-dən locale çıxarılır, fallback `app.default-locale=az`
- Hər error mesajı `ErrorCode` enum daşıyır → `messages_<lang>.properties`-dən resolve olunur
- Bean Validation mesajları da eyni keys istifadə edir (`{validation.email.format}` kimi)
- Mobile: `languageProvider` (Riverpod StateProvider) → MaterialApp locale + `LocaleInterceptor` hər sorğuda Accept-Language header

---

## 6. Auth flow (rotating refresh token)

```
Mobile               Backend                DB
  │                     │                    │
  ├── POST /login ─────►│                    │
  │                     │─ validate ───────►│
  │                     │◄─────── user OK ──┤
  │                     │─ insert refresh ─►│
  │◄── access + refresh+│                    │
  │                     │                    │
  ├── GET /users/me ───►│                    │
  │   Bearer access     │                    │
  │                     │                    │
  ├── (15 dəq sonra)    │                    │
  ├── POST /refresh ───►│                    │
  │                     │─ revoke old, ─────►│
  │                     │  insert new        │
  │◄── new pair ────────┤                    │
```

- Access token: 15 dəq, JWT HS256
- Refresh token: 30 gün, opaque base64, DB-də saxlanılır
- Rotation: hər refresh-də köhnəni revoke edir, yenisini insert edir
- `/auth/logout-all` istifadəçinin bütün sessiyalarını dayandırır

---

## 7. Lokal development (Mac)

### Backend

```bash
cd ~/Desktop/gazan-backend
docker compose up -d                  # postgres
./gradlew bootRun                     # backend (8080)
```

`flutter doctor` analoqu yoxlamaları:
```bash
java -version          # 21+
docker compose ps      # qazan-postgres Up (healthy)
curl http://localhost:8080/actuator/health
```

### Mobile

```bash
cd ~/Desktop/gazan-mobile
flutter pub get
flutter create . --platforms=web,ios,android,macos     # ilk dəfə
flutter run -d chrome
```

Default `API_BASE_URL` `app_config.dart`-dadır:
- web/iOS/macOS → `http://localhost:8080`
- Android emulator → `http://10.0.2.2:8080`

Override:
```bash
flutter run -d chrome --dart-define=API_BASE_URL=https://api.qazan.az
```

### Demo hesablar (DevDataSeeder yaradır)

| Email | Şifrə | Rol |
|---|---|---|
| `demo@qazan.az` | `password123` | Müştəri |
| `biz@qazan.az` | `password123` | Biznes (The Bagel Bar) |

Seeded data: 8 obyekt, 9 sadiqlik proqramı, 8 story, 5 promo banner.

---

## 8. Deployment (Ubuntu server)

### Server məlumatları

- IP: `209.97.135.206`
- Path: `/opt/qazan/gazan-backend/`
- Konteynerlər: `qazan-postgres`, `qazan-backend`, `qazan-web`
- Docker volume: `qazan_pg_data`
- Secrets: `/opt/qazan/gazan-backend/deploy/.env` (random JWT_SECRET + POSTGRES_PASSWORD)

### Deploy strategiyası

**Local-build, ship-artifact** modeli:

1. **Mac-də** Flutter web bundle qurulur (`flutter build web`)
2. **Mac-də** Spring Boot fat-jar qurulur (`./gradlew bootJar`)
3. tar arxivi + nginx.conf + Dockerfiles + docker-compose hazırlanır
4. scp ilə server-ə göndərilir
5. Server-də `server-setup.sh` qaçır:
   - Docker yoxdursa yükləyir
   - UFW firewall (80, 443) açır
   - Random `JWT_SECRET` + `POSTGRES_PASSWORD` generə edib `.env`-də saxlayır
   - Docker compose qaldırır (postgres + backend + nginx)
6. nginx 80-də: `/api/*` → backend:8080, `/*` → static Flutter web

### Deploy əmrləri

**Tek əmr** Mac-də:

```bash
cd ~/Desktop/gazan-backend
bash deploy/deploy-from-mac.sh
```

SSH şifrəsi 2 dəfə interaktiv soruşulur (scp + ssh).

### Server-də faydalı əmrlər

```bash
ssh root@209.97.135.206

# Konteyner statusu
cd /opt/qazan/gazan-backend
docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env ps

# Canlı log
docker logs -f qazan-backend
docker logs -f qazan-web
docker logs -f qazan-postgres

# Yenidən qaldır
docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env restart backend

# DB-yə daxil ol
docker exec -it qazan-postgres psql -U qazan -d qazan
```

### Production URL-lər

- Web app: `http://209.97.135.206`
- Swagger UI: `http://209.97.135.206/docs`
- Actuator: `http://209.97.135.206/actuator/health`

---

## 9. Vacib texniki həllər və səbəbləri

| Problem / qərar | Həll | Niyə |
|---|---|---|
| Web-də `flutter_secure_storage` HTTPS olmayanda crash edir (WebCrypto undefined) | `kIsWeb` runtime check → SharedPreferences (web) / FlutterSecureStorage (native) | HTTP üzərində demo qaçır, prod-da HTTPS qoyulanda da işləyir |
| Backend Docker build server-də 15+ dəq çəkir (Maven Central yavaş) | Mac-də local JAR build → server yalnız `COPY app.jar` | 15 dəq → 30 saniyə |
| Flutter web random port işlədir, CORS allowlist toqquşur | `app.cors.allowed-origins=http://localhost:*` (wildcard) | Dev üçün rahat, prod-da darıxdırılır |
| `BasicAuthenticationEntryPoint` functional interface deyil → lambda kompilyasiya etmir | `AuthenticationEntryPoint` (interface) istifadə et | Spring Security 6 dəyişikliyi |
| JWT secret base64 vs UTF-8 ambiguity | Sadəcə UTF-8 oxu, ≥32 byte yoxla | Yanlış parse-dan qaçınmaq |
| Refresh token JWT olsun yoxsa opaque? | Opaque, DB-də saxlanılır, rotation | Revoke imkanı (logout, theft response) |
| Mobile mock-dan real API-yə keçid | Repository interface dəyişmir, yalnız implementation: Mock → Remote (dio) | UI kodu toxunulmadan qalır |

---

## 10. Hazır olan vs gözləyən

### ✓ Tam hazır

- Bütün 15 REST endpoint backend-də işləyir, JPA + Postgres-ə yazır
- Mobile UI (29 ekran) backend-ə bağlıdır, mock kodu silinib
- Auth: register/login/refresh/logout, token rotation, restore session
- 4 dil i18n (backend error mesajları)
- Demo data seeder
- Docker deployment (postgres + backend + nginx)
- Local + production konfiq
- Profile interactivity (theme, language, notifications, edit, password change, FAQ, about)
- Stories (Instagram-style viewer) + ad banners
- QR scanner real `/scans` endpoint-i ilə işləyir

### ⚠ Yarımçıq

- Mobile lokalizasiya (.arb fayllarına köçürülməyib — strings hələ də Azərbaycanca hardcoded)
- Avatar upload yoxdur (yalnız URL)
- Email verification (registration tək addımdır)
- Password reset flow (skeleton var, endpoint hazır deyil)
- Biznes "Müştərilər" siyahısı endpoint backend-də yoxdur (mobile-də empty list göstərir)
- HTTPS yoxdur production-da (HTTP-də demo)
- Push notifications

### 🔜 Növbəti addımlar (priority sırası)

1. **HTTPS + domain** — Caddy + Let's Encrypt qoş (deploy script-ə əlavə et)
2. **Mobile localization** — `flutter gen-l10n`, .arb fayllarını yarat
3. **`/api/v1/business/customers` endpoint** — biznes müştəri siyahısı
4. **Avatar upload** — S3 / Cloudinary / lokal disk + multipart
5. **Rate limiting** `/auth/login`-də (Bucket4j)
6. **Password reset** — email-based token flow
7. **Push notifications** — Firebase Cloud Messaging
8. **Tests** — backend integration tests, Flutter widget tests
9. **CI/CD** — GitHub Actions: build + push image + auto deploy

---

## 11. Xronologiya — sessiya boyu nə baş verdi

1. **Mobile MVP** — boş qovluqdan başlayaraq Flutter app yığıldı (50 dart fayl, clean architecture, mock data)
2. **Loyalty rules genişləndirildi** — `LoyaltyRewardType` (4 növ: free / %, fixed, cashback) + biznes proqram CRUD UI + canlı önbaxış
3. **Profile interactivity** — 6 sheet (theme, language, notifications, edit, security, FAQ)
4. **Default theme = light, logout → login** — istifadəçi tələbi
5. **Stories + Ads** — yeni feature modulu (full-screen viewer + horizontal carousel)
6. **Backend layihə** — Spring Boot 3.4 + Java 21 + JWT + i18n + Flyway + 15 endpoint, 9 modul, 4 dil
7. **Mobile ↔ Backend inteqrasiyası** — dio + interceptors + secure/shared storage + 4 remote repository, mock-lar əvəzlənib
8. **Deployment** — Ubuntu server-ə Docker + nginx + automatic setup script
9. **Bug fixes** — `flutter_secure_storage` web HTTPS issue, gradle build sürəti, CORS wildcard, BasicAuthenticationEntryPoint compile error

---

## 12. Vacib fayllar — referans

| Yol | Nə üçün |
|---|---|
| `gazan-backend/build.gradle` | Spring Boot dependencies (Groovy DSL) |
| `gazan-backend/src/main/resources/application.yml` | Bütün konfiq (DB, JWT, CORS, i18n) |
| `gazan-backend/src/main/resources/db/migration/V*.sql` | DB schema |
| `gazan-backend/deploy/deploy-from-mac.sh` | Tek-əmr deploy script |
| `gazan-backend/deploy/server-setup.sh` | Server-side setup (Docker + run) |
| `gazan-backend/deploy/docker-compose.prod.yml` | Konteyner orchestration |
| `gazan-backend/deploy/Dockerfile.backend` | Slim JRE + COPY app.jar |
| `gazan-backend/deploy/Dockerfile.web` | nginx + Flutter web bundle |
| `gazan-backend/deploy/nginx.conf` | Reverse proxy + SPA fallback |
| `gazan-mobile/pubspec.yaml` | Flutter dependencies |
| `gazan-mobile/lib/main.dart` | App entry |
| `gazan-mobile/lib/core/config/app_config.dart` | API base URL |
| `gazan-mobile/lib/routing/app_router.dart` | Routes + redirect |

---

## 13. Tövsiyələr / xəbərdarlıqlar

- **Şifrəni dəyiş** — bu sessiyada server SSH şifrəsi mətnə düşüb. Mütləq `passwd` ilə dəyiş və ya SSH key-ə keç (`ssh-keygen` + `ssh-copy-id`).
- **JWT_SECRET** server-də random generə olunur (`/opt/qazan/gazan-backend/deploy/.env` daxilində), sənin tərəfdən heç nə etmək lazım deyil.
- **Postgres data** Docker volume-də (`qazan_pg_data`). Server yenidən qurulanda data qalır.
- **Backup**: `docker exec qazan-postgres pg_dump -U qazan qazan > backup.sql` lokal kopya götür.
- **Yeniləmə**: hər kod dəyişikliyindən sonra `bash deploy/deploy-from-mac.sh` qaçırırsan, hər şey avtomatik yenilənir.

---

## 14. Yeni hesabda işə davam etmək üçün

Yeni Claude (və ya başqa AI) hesabına keçəndə bu sənədi göstər və de:

> "Mən Qazan adlı sadiqlik tətbiqi üzərində işləyirəm. İki repo: `~/Desktop/gazan-mobile` (Flutter) və `~/Desktop/gazan-backend` (Spring Boot). Bu HANDOVER.md-də bütün arxitektura, kodun yeri və deployment yazılıb. Layihə [server IP-də] qaçır. İndi [konkret task] etmək istəyirəm."

Sənəddə hər şey var — strukturdan başlayıb deploy axınına qədər.

---

*Son redaktə: 14 May 2026 — sessiyanın yekun snapshot-ı.*
