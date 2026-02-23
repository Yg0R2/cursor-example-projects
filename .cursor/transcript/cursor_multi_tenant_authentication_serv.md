# Multi-tenant authentication service with Redis
_Exported on 2/22/2026 at 10:45:54 GMT+1 from Cursor (2.5.20)_

---

**User**

I want to build a muty tenent authentication service.
Currently I only focus on Spring Boot for this.
I want to use properly the previously created `core-library` in this new project. I also want to use the same Kotlin/Java/Gradle version and the same project structure (api, persistence, etc...) that are used in the `core-library` project.

This service is called `auth-service` and please put it in the `auth` folder.
I want to use REDIS session store to store the user sessions.
For the "tenent ID", please suggest some options where and how should be propagated/used/stored.

I want this service to provide a generic JWT that does not contain all information from the user jsut the necessary to identify the session/user/tenent. I want to read the real session data that might contains roles and other things from REDIS.

The UI is not important at the moment, it can be the default Spring base authentication.

---

**Cursor**

I'll explore the existing `core-library` project to understand its structure, versions, and configuration so I can provide accurate guidance for your new `auth-service`.

Now I have a complete picture of your `core-library` setup. Let me provide comprehensive guidance for your `auth-service`.

## Summary of Core Library Configuration

| Component | Version |
|-----------|---------|
| Spring Boot | 4.0.2 |
| Kotlin | 2.3.10 |
| Java | 25 |

## Recommended Auth Service Structure

Based on the `core-library` pattern, here's the recommended structure for `auth/auth-service`:

```
auth/
└── auth-service/
    ├── build.gradle.kts
    ├── settings.gradle.kts
    ├── gradle.properties
    ├── gradle/
    │   └── libs.versions.toml
    ├── buildSrc/
    │   ├── build.gradle.kts
    │   ├── settings.gradle.kts
    │   └── src/main/kotlin/
    │       ├── auth-service.kotlin-conventions.gradle.kts
    │       ├── auth-service.spring-module-conventions.gradle.kts
    │       └── auth-service.platform-conventions.gradle.kts
    │
    │   # Framework-agnostic modules
    ├── auth-api/                    # DTOs, requests/responses
    ├── auth-domain/                 # Domain models (User, Tenant, Session)
    ├── auth-persistence/            # Repository interfaces
    ├── auth-service/                # Business logic interfaces
    │
    │   # Spring implementation modules
    ├── spring-auth-platform/        # BOM with Spring Boot + core-library
    ├── spring-auth-api/             # Spring-specific API extensions
    ├── spring-auth-persistence/     # Redis repositories, JPA if needed
    ├── spring-auth-service/         # Authentication service impl
    ├── spring-auth-web/             # REST controllers, filters
    └── spring-auth-application/     # Boot application entry point
```

---

## Tenant ID Propagation Options

Here are the main strategies for propagating tenant ID in a multi-tenant system:

### Option 1: Subdomain-based (Recommended for SaaS)

```
https://tenant1.yourapp.com/api/...
https://tenant2.yourapp.com/api/...
```

**Pros:**
- Clean URL structure
- Easy client-side handling
- Natural isolation feeling for tenants

**Cons:**
- Requires wildcard DNS/SSL certificates
- More complex local development

**Implementation:**
```kotlin
@Component
class TenantSubdomainFilter : OncePerRequestFilter() {
    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        val host = request.serverName
        val tenantId = host.substringBefore(".yourapp.com")
        TenantContext.setCurrentTenant(tenantId)
        try {
            chain.doFilter(request, response)
        } finally {
            TenantContext.clear()
        }
    }
}
```

### Option 2: HTTP Header-based (Recommended for APIs)

```
X-Tenant-ID: tenant-uuid-here
```

**Pros:**
- Simple implementation
- Works well for API-first design
- Easy to test

**Cons:**
- Client must always send header
- Easy to forget/misconfigure

**Implementation:**
```kotlin
@Component
class TenantHeaderFilter : OncePerRequestFilter() {
    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        val tenantId = request.getHeader("X-Tenant-ID") 
            ?: throw TenantNotFoundException("X-Tenant-ID header required")
        TenantContext.setCurrentTenant(tenantId)
        try {
            chain.doFilter(request, response)
        } finally {
            TenantContext.clear()
        }
    }
}
```

### Option 3: JWT Claim (Recommended for your use case)

Include tenant ID inside the JWT itself:

```json
{
  "sub": "user-uuid",
  "sid": "session-uuid",
  "tid": "tenant-uuid",
  "iat": 1699999999,
  "exp": 1700000000
}
```

**Pros:**
- Self-contained - no extra headers needed
- Validated with JWT signature
- Cannot be tampered with

**Cons:**
- Tenant switch requires new token

### Option 4: Path-based

```
https://yourapp.com/api/v1/tenants/{tenantId}/users
```

**Pros:**
- RESTful, explicit
- Easy to understand

**Cons:**
- Verbose URLs
- Tenant ID in every endpoint

---

## My Recommendation: Hybrid Approach

For your use case (minimal JWT + Redis session), I recommend:

1. **JWT contains:** `sub` (user ID), `sid` (session ID), `tid` (tenant ID)
2. **Redis stores:** Full session data keyed by `session:{tid}:{sid}`
3. **TenantContext:** ThreadLocal holder for request-scoped tenant access

### Minimal JWT Structure

```kotlin
data class AuthToken(
    val sub: String,    // User ID
    val sid: String,    // Session ID  
    val tid: String,    // Tenant ID
    val iat: Long,      // Issued at
    val exp: Long       // Expiration
)
```

### Redis Session Data Structure

```kotlin
data class SessionData(
    val sessionId: String,
    val userId: String,
    val tenantId: String,
    val username: String,
    val email: String,
    val roles: Set<String>,
    val permissions: Set<String>,
    val metadata: Map<String, Any>,
    val createdAt: Instant,
    val lastAccessedAt: Instant,
    val expiresAt: Instant
)
```

Redis key pattern: `session:{tenantId}:{sessionId}`

---

## Key Dependencies to Add

Your `libs.versions.toml` should include:

```toml
[versions]
spring-boot = "4.0.2"
kotlin = "2.3.10"
core-library = "0.0.1-SNAPSHOT"

[libraries]
# Core library platform (imports its BOM)
core-library-platform = { module = "com.example.core:spring-core-platform", version.ref = "core-library" }

# Spring Boot starters
spring-boot-starter-security = { module = "org.springframework.boot:spring-boot-starter-security" }
spring-boot-starter-data-redis = { module = "org.springframework.boot:spring-boot-starter-data-redis" }
spring-boot-starter-oauth2-resource-server = { module = "org.springframework.boot:spring-boot-starter-oauth2-resource-server" }
spring-session-data-redis = { module = "org.springframework.session:spring-session-data-redis" }

# JWT
jjwt-api = { module = "io.jsonwebtoken:jjwt-api", version = "0.12.6" }
jjwt-impl = { module = "io.jsonwebtoken:jjwt-impl", version = "0.12.6" }
jjwt-jackson = { module = "io.jsonwebtoken:jjwt-jackson", version = "0.12.6" }
```

---

## Architecture Flow

```
┌─────────────────────────────────────────────────────────────────────┐
│                         Auth Service Flow                           │
└─────────────────────────────────────────────────────────────────────┘

1. LOGIN REQUEST
   ┌──────────┐    POST /auth/login     ┌──────────────┐
   │  Client  │ ────────────────────────▶ │  Auth Web    │
   │          │  {tenant, user, pass}   │  Controller  │
   └──────────┘                         └──────┬───────┘
                                               │
2. AUTHENTICATE & CREATE SESSION               ▼
                                        ┌──────────────┐
                                        │ Auth Service │
                                        │ (validates)  │
                                        └──────┬───────┘
                                               │
3. STORE FULL SESSION IN REDIS                 ▼
                                        ┌──────────────┐
   ┌──────────┐                         │   Redis      │
   │  Redis   │ ◀───────────────────────│  Repository  │
   │          │  session:{tid}:{sid}    │              │
   │ SessionData with roles, perms...   └──────────────┘
   └──────────┘
                                               │
4. RETURN MINIMAL JWT                          ▼
   ┌──────────┐    JWT {sub,sid,tid}    ┌──────────────┐
   │  Client  │ ◀─────────────────────── │  JWT Service │
   │          │                         │              │
   └──────────┘                         └──────────────┘


5. SUBSEQUENT REQUESTS
   ┌──────────┐   Authorization: Bearer  ┌──────────────┐
   │  Client  │ ─────────────────────────▶│ JWT Filter   │
   └──────────┘                          └──────┬───────┘
                                                │
                                                ▼ extract tid, sid
                                         ┌──────────────┐
   ┌──────────┐                          │   Redis      │
   │  Redis   │ ◀─────────────────────── │  Lookup      │
   │          │  GET session:{tid}:{sid} │              │
   └──────────┘                          └──────┬───────┘
                                                │
                                                ▼ populate SecurityContext
                                         ┌──────────────┐
                                         │ TenantContext│
                                         │ + Principal  │
                                         └──────────────┘
```

---

## Sample Configuration (application.yml)

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password: ${REDIS_PASSWORD:}
      
  session:
    store-type: redis
    redis:
      namespace: auth:sessions
      
auth:
  jwt:
    secret: ${JWT_SECRET}
    expiration: 3600  # 1 hour
    issuer: auth-service
    
  session:
    timeout: 86400    # 24 hours in Redis
    
  tenant:
    # Which method to extract tenant ID
    resolution-strategy: JWT  # or HEADER, SUBDOMAIN
    header-name: X-Tenant-ID
```

---

## Questions to Consider

Before implementing, you should decide:

1. **User storage:** Will users be stored per-tenant (tenant-scoped tables) or globally with tenant associations?

2. **Tenant resolution on login:** During login, tenant ID must come from somewhere (header, subdomain, or login payload) since JWT doesn't exist yet.

3. **Token refresh strategy:** Will you use refresh tokens? If so, those also go in Redis.

4. **Session invalidation:** Do you need to invalidate all sessions for a user/tenant (e.g., password change)?

---

Would you like me to elaborate on any specific part, or would you like to switch to Agent mode so I can create the project structure and initial code for you?

---

**User**

I like your idea to use `auth-service` for this project. So please use the `./auth-service` location for this proejct.

I want to go with the "Option 3: JWT Claim", I even like the fact that a new token needed to be generated when switching between tenants. This also can be semi-automated. When a user wants to do this the service will remove/expire the first token and generate and send a new one; where the token even can have multiple tenantIds. What do ytou think about having multiple tenant IDs in the JWT at the same time?

I also want to enable tenants to have their private domain like `my-super-domain` to be redirected/proxied/reverse proxied to a specific site, that can be also reached (like if it is enabled) throug `my-super-domain.example.com` where `example.com` is the domain where my service is running.

I like the `Hybrid Approach` that you suggest, with only one remark. I want to use suspended controllers and service classes , and even async clients where it is possible. So instead of using `ThreadLocal` to store the context, I would need Kotlin Context for this.

Which one can I use as a parameter in the controller function, the `AuthToken` or the `SessionData`?
I need to sometimes pass this as a parameter and also to do additinal checks like `@hasScope` or `@hasRole`.
For this object, I want to use fully qualified names, like the `sessionId` and `useId`, etc.

Answering your questions:
1. I thought I want to store the users in the `user-service` that will be implemented separately, so at the moment just use a static response for 1-2 users. This `user-service` would transform the user name or email address into an userId, and I want to use this userId and the provided password to get it from **Keykloak**.

Your question is: Will users be stored per-tenant?
Yes, and no. I want to make it configurable where I can store every user in the same DB but each user would have a list of tenantIds that can log into. And also want to be able to connect separate DBs for each tenant, where the users in this case would be separately stored.

2. The tenant Id would come from the sub-domain. Or from the redirected/proxied/reverse proxied content somehow. Please suggest/clarify this with me somehow :)

3. I thought the tokens can be refreshed somehow, but I did not do any research on this. So I do not know yet. Please give me som examples.

4. Yes, please. I want to invalidate the token when the password changes and even when the user signs out, but in this case just invalidate for the specific tenant or if it is requested by the user (through like a flag during logout) invalidate all session!

---

**Cursor**

Excellent questions! Let me address each point in detail.

---

## 1. Multiple Tenant IDs in JWT

This is a powerful pattern called **"Multi-Tenant Session"** or **"Tenant Switching"**. Here's my analysis:

### Pros
- User doesn't need to re-authenticate when switching tenants
- Single token for users who work across multiple tenants (consultants, admins)
- Smoother UX for multi-tenant users

### Cons
- Larger JWT payload
- Security consideration: if token is compromised, attacker has access to ALL tenants
- Need to track "active tenant" separately (either in JWT or as a separate claim)

### Recommended Approach: Active + Available Tenants

```kotlin
data class AuthToken(
    val userId: String,           // The authenticated user
    val sessionId: String,        // Session reference in Redis
    val activeTenantId: String,   // Currently selected tenant
    val availableTenantIds: Set<String>,  // All tenants user can access
    val issuedAt: Instant,
    val expiresAt: Instant
)
```

**JWT Claims:**
```json
{
  "userId": "usr_abc123",
  "sessionId": "ses_xyz789",
  "activeTenantId": "tenant_acme",
  "availableTenantIds": ["tenant_acme", "tenant_globex", "tenant_initech"],
  "iat": 1699999999,
  "exp": 1700003599
}
```

**Tenant Switch Flow:**
```
POST /auth/tenant/switch
{
  "targetTenantId": "tenant_globex"
}

Response:
{
  "token": "new.jwt.token",  // Same sessionId, different activeTenantId
  "activeTenantId": "tenant_globex"
}
```

This way:
- No re-authentication needed (same session)
- New token is issued (old one should be blacklisted or short-lived)
- Active tenant is explicit in the token

---

## 2. Custom Domain / Reverse Proxy Setup

For custom domains like `my-super-domain.com` → your service, here are the options:

### Option A: Reverse Proxy Header (Recommended)

When using nginx/Traefik/Cloudflare as reverse proxy:

```nginx
# nginx config for custom domain
server {
    server_name my-super-domain.com;
    
    location / {
        proxy_pass http://auth-service:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Original-Host $host;
        proxy_set_header X-Tenant-Domain my-super-domain.com;
        # Or resolve tenant ID at proxy level:
        proxy_set_header X-Tenant-ID tenant_acme;
    }
}
```

### Option B: Domain-to-Tenant Mapping Table

Store in Redis or DB:

```kotlin
data class TenantDomainMapping(
    val domain: String,           // "my-super-domain.com" or "acme.example.com"
    val tenantId: String,         // "tenant_acme"
    val isPrimaryDomain: Boolean, // true for custom domain
    val sslEnabled: Boolean
)
```

### Tenant Resolution Strategy (Priority Order)

```kotlin
@Component
class TenantResolver(
    private val domainMappingRepository: TenantDomainMappingRepository
) {
    suspend fun resolveTenant(request: ServerHttpRequest): String {
        // 1. Check X-Tenant-ID header (for API clients)
        request.headers.getFirst("X-Tenant-ID")?.let { return it }
        
        // 2. Check JWT (if already authenticated)
        extractTenantFromJwt(request)?.let { return it }
        
        // 3. Check X-Tenant-Domain header (from reverse proxy)
        request.headers.getFirst("X-Tenant-Domain")?.let { domain ->
            return domainMappingRepository.findByDomain(domain)?.tenantId
                ?: throw TenantNotFoundException("Unknown domain: $domain")
        }
        
        // 4. Check Host header (subdomain or custom domain)
        val host = request.headers.host?.hostString ?: throw TenantNotFoundException("No host")
        
        // 4a. Custom domain lookup
        domainMappingRepository.findByDomain(host)?.let { return it.tenantId }
        
        // 4b. Subdomain extraction (acme.example.com -> acme)
        val subdomain = extractSubdomain(host)
        return domainMappingRepository.findBySubdomain(subdomain)?.tenantId
            ?: throw TenantNotFoundException("Unknown subdomain: $subdomain")
    }
}
```

---

## 3. Kotlin Coroutine Context (Instead of ThreadLocal)

For suspended functions, use `CoroutineContext`:

```kotlin
// Define the context element
data class AuthContext(
    val token: AuthToken,
    val session: SessionData
) : AbstractCoroutineContextElement(AuthContext) {
    companion object Key : CoroutineContext.Key<AuthContext>
}

// Extension to access from coroutine
suspend fun currentAuthContext(): AuthContext {
    return coroutineContext[AuthContext] 
        ?: throw AuthenticationException("No auth context")
}

suspend fun currentSession(): SessionData = currentAuthContext().session
suspend fun currentTenantId(): String = currentAuthContext().token.activeTenantId
```

**Usage in Filter (WebFlux):**

```kotlin
@Component
class AuthContextFilter(
    private val jwtService: JwtService,
    private val sessionRepository: SessionRepository
) : WebFilter {
    
    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        return mono {
            val token = jwtService.extractToken(exchange.request)
            val session = sessionRepository.findById(token.sessionId)
                ?: throw SessionExpiredException("Session not found")
            
            val authContext = AuthContext(token, session)
            
            // Propagate context through the reactive chain
            withContext(authContext) {
                chain.filter(exchange).awaitSingleOrNull()
            }
        }.then()
    }
}
```

---

## 4. Controller Parameter: AuthToken vs SessionData

**Recommendation: Use `SessionData` as the primary parameter**, because:

1. It contains everything (token data is a subset)
2. It's already validated and loaded from Redis
3. You need roles/permissions for `@hasRole` checks

### Updated Data Classes with Fully Qualified Names

```kotlin
// Minimal JWT payload
data class AuthToken(
    val userId: String,
    val sessionId: String,
    val activeTenantId: String,
    val availableTenantIds: Set<String>,
    val issuedAt: Instant,
    val expiresAt: Instant
)

// Full session data from Redis
data class SessionData(
    val sessionId: String,
    val userId: String,
    val username: String,
    val email: String,
    val activeTenantId: String,
    val availableTenantIds: Set<String>,
    val roles: Set<String>,           // e.g., ["ADMIN", "USER"]
    val permissions: Set<String>,     // e.g., ["users:read", "users:write"]
    val scopes: Set<String>,          // OAuth scopes if applicable
    val metadata: Map<String, Any>,   // Custom attributes
    val createdAt: Instant,
    val lastAccessedAt: Instant,
    val expiresAt: Instant,
    val authToken: AuthToken          // Reference to the token
)
```

### Controller Usage

```kotlin
@RestController
@RequestMapping("/api/v1/users")
class UserController(
    private val userService: UserService
) {
    
    // Option 1: Inject via parameter resolver
    @GetMapping("/me")
    suspend fun getCurrentUser(session: SessionData): UserResponse {
        return userService.getUser(session.userId, session.activeTenantId)
    }
    
    // Option 2: Use annotation + resolver
    @GetMapping("/admin")
    @HasRole("ADMIN")
    suspend fun getAdminData(session: SessionData): AdminResponse {
        // Role is already validated by @HasRole
        return adminService.getData(session.activeTenantId)
    }
    
    // Option 3: Use scopes
    @GetMapping("/sensitive")
    @HasScope("sensitive:read")
    suspend fun getSensitiveData(session: SessionData): SensitiveResponse {
        return sensitiveService.getData(session)
    }
    
    // Option 4: Multiple checks
    @DeleteMapping("/{userId}")
    @HasRole("ADMIN")
    @HasPermission("users:delete")
    suspend fun deleteUser(
        @PathVariable userId: String,
        session: SessionData
    ): Unit {
        userService.delete(userId, session.activeTenantId)
    }
}
```

### Custom Annotations for Authorization

```kotlin
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class HasRole(vararg val roles: String)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class HasScope(vararg val scopes: String)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class HasPermission(vararg val permissions: String)

// Aspect to handle these
@Aspect
@Component
class AuthorizationAspect {
    
    @Around("@annotation(hasRole)")
    suspend fun checkRole(joinPoint: ProceedingJoinPoint, hasRole: HasRole): Any? {
        val session = currentSession()
        val requiredRoles = hasRole.roles.toSet()
        
        if (session.roles.intersect(requiredRoles).isEmpty()) {
            throw AccessDeniedException("Required roles: $requiredRoles")
        }
        
        return joinPoint.proceed()
    }
}
```

---

## 5. Token Refresh Strategy

### Option A: Refresh Token Pattern (Recommended)

```
Access Token:  Short-lived (15-60 min), used for API calls
Refresh Token: Long-lived (7-30 days), used only to get new access token
```

**Flow:**

```
1. LOGIN
   POST /auth/login { credentials }
   Response: {
     accessToken: "short.lived.jwt",      // 15 min
     refreshToken: "long.lived.token",    // 7 days, stored in Redis
     expiresIn: 900
   }

2. API CALL (with access token)
   GET /api/users
   Authorization: Bearer short.lived.jwt

3. ACCESS TOKEN EXPIRED
   Client receives 401

4. REFRESH
   POST /auth/refresh
   { refreshToken: "long.lived.token" }
   
   Response: {
     accessToken: "new.short.jwt",
     refreshToken: "new.long.token",  // Rotate for security
     expiresIn: 900
   }
```

**Redis Storage:**

```
Key: refresh_token:{tokenHash}
Value: {
  sessionId: "ses_xyz",
  userId: "usr_abc",
  tenantIds: ["tenant_acme"],
  issuedAt: "2024-01-01T00:00:00Z",
  expiresAt: "2024-01-08T00:00:00Z",
  deviceInfo: "Chrome/Windows"
}
```

### Option B: Sliding Session (Simpler)

Access token is refreshed automatically on each request if within a window:

```kotlin
class TokenRefreshFilter : WebFilter {
    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        return mono {
            val token = extractToken(exchange)
            
            // If token expires within 5 minutes, issue new one
            if (token.expiresAt.minus(5, ChronoUnit.MINUTES).isBefore(Instant.now())) {
                val newToken = jwtService.refresh(token)
                exchange.response.headers.add("X-New-Token", newToken)
            }
            
            chain.filter(exchange).awaitSingleOrNull()
        }.then()
    }
}
```

---

## 6. Session Invalidation Strategy

### Data Structures in Redis

```
# Session data
session:{tenantId}:{sessionId} -> SessionData (JSON)

# User's sessions index (for "logout all")
user_sessions:{userId} -> Set<sessionId>

# Tenant's sessions for a user (for "logout from tenant")
user_tenant_sessions:{userId}:{tenantId} -> Set<sessionId>

# Token blacklist (for immediate invalidation)
blacklist:{tokenHash} -> expiresAt (with TTL matching token expiry)
```

### Invalidation Scenarios

```kotlin
interface SessionInvalidationService {
    
    // Logout from current session only
    suspend fun logout(sessionId: String)
    
    // Logout from specific tenant (all sessions for that tenant)
    suspend fun logoutFromTenant(userId: String, tenantId: String)
    
    // Logout from all tenants (password change, security breach)
    suspend fun logoutFromAllTenants(userId: String)
    
    // Invalidate specific token immediately (before expiry)
    suspend fun blacklistToken(token: AuthToken)
}

@Service
class SessionInvalidationServiceImpl(
    private val redisTemplate: ReactiveRedisTemplate<String, String>,
    private val sessionRepository: SessionRepository
) : SessionInvalidationService {
    
    override suspend fun logout(sessionId: String) {
        // Delete session from Redis
        sessionRepository.deleteById(sessionId)
    }
    
    override suspend fun logoutFromTenant(userId: String, tenantId: String) {
        // Get all sessions for user+tenant
        val sessionIds = redisTemplate.opsForSet()
            .members("user_tenant_sessions:$userId:$tenantId")
            .asFlow()
            .toList()
        
        // Delete each session
        sessionIds.forEach { sessionId ->
            sessionRepository.deleteById(sessionId)
        }
        
        // Clear the index
        redisTemplate.delete("user_tenant_sessions:$userId:$tenantId").awaitSingle()
    }
    
    override suspend fun logoutFromAllTenants(userId: String) {
        // Get all sessions for user
        val sessionIds = redisTemplate.opsForSet()
            .members("user_sessions:$userId")
            .asFlow()
            .toList()
        
        // Delete all sessions
        sessionIds.forEach { sessionId ->
            sessionRepository.deleteById(sessionId)
        }
        
        // Clear indexes
        redisTemplate.delete("user_sessions:$userId").awaitSingle()
    }
    
    override suspend fun blacklistToken(token: AuthToken) {
        val tokenHash = hashToken(token)
        val ttl = Duration.between(Instant.now(), token.expiresAt)
        
        redisTemplate.opsForValue()
            .set("blacklist:$tokenHash", token.expiresAt.toString(), ttl)
            .awaitSingle()
    }
}
```

### Logout Endpoint

```kotlin
@RestController
@RequestMapping("/auth")
class AuthController(
    private val authService: AuthService,
    private val invalidationService: SessionInvalidationService
) {
    
    data class LogoutRequest(
        val logoutFromAllTenants: Boolean = false,
        val logoutFromAllSessions: Boolean = false
    )
    
    @PostMapping("/logout")
    suspend fun logout(
        @RequestBody request: LogoutRequest,
        session: SessionData
    ): ResponseEntity<Unit> {
        when {
            request.logoutFromAllSessions -> {
                // Password change scenario - kick out everywhere
                invalidationService.logoutFromAllTenants(session.userId)
            }
            request.logoutFromAllTenants -> {
                // User wants to logout from all tenants but same device
                session.availableTenantIds.forEach { tenantId ->
                    invalidationService.logoutFromTenant(session.userId, tenantId)
                }
            }
            else -> {
                // Just this session
                invalidationService.logout(session.sessionId)
            }
        }
        
        return ResponseEntity.noContent().build()
    }
}
```

---

## Summary Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         Auth Service Architecture                            │
└─────────────────────────────────────────────────────────────────────────────┘

                    ┌─────────────────────────────────────┐
                    │         Reverse Proxy               │
                    │  (nginx/Traefik/Cloudflare)         │
                    │                                     │
                    │  custom-domain.com ──┐              │
                    │  acme.example.com ───┼──▶ Headers   │
                    │  api.example.com ────┘              │
                    └───────────────┬─────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                           Auth Service                                       │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐         │
│  │ Tenant      │  │ JWT Filter  │  │ Auth        │  │ Session     │         │
│  │ Resolver    │─▶│ + Context   │─▶│ Controller  │─▶│ Service     │         │
│  │ (Domain/    │  │ Propagation │  │             │  │             │         │
│  │  Header)    │  │ (Coroutine) │  │             │  │             │         │
│  └─────────────┘  └─────────────┘  └─────────────┘  └──────┬──────┘         │
│                                                             │                │
└─────────────────────────────────────────────────────────────┼────────────────┘
                                                              │
                    ┌─────────────────────────────────────────┼───────┐
                    │                                         ▼       │
                    │  ┌──────────────────────────────────────────┐   │
                    │  │                 REDIS                    │   │
                    │  │                                          │   │
                    │  │  session:{tid}:{sid}     → SessionData   │   │
                    │  │  user_sessions:{uid}     → Set<sid>      │   │
                    │  │  refresh_token:{hash}    → RefreshData   │   │
                    │  │  blacklist:{hash}        → expiresAt     │   │
                    │  │  domain_mapping:{domain} → tenantId      │   │
                    │  │                                          │   │
                    │  └──────────────────────────────────────────┘   │
                    │                                                 │
                    │                External Services                │
                    │  ┌────────────┐  ┌────────────┐                 │
                    │  │  Keycloak  │  │   User     │                 │
                    │  │  (AuthN)   │  │  Service   │                 │
                    │  └────────────┘  └────────────┘                 │
                    └─────────────────────────────────────────────────┘
```

---

## Clarification Questions

1. **Custom domain SSL**: Will you handle SSL termination at the reverse proxy level, or do you need the auth-service to handle it?

2. **Keycloak integration**: Do you want to use Keycloak as:
   - **Identity Provider only** (just validates credentials, you issue your own JWT)?
   - **Full OAuth2/OIDC flow** (Keycloak issues tokens, you validate them)?

3. **User Service communication**: Should the auth-service call user-service via:
   - **REST/HTTP** (simpler)?
   - **gRPC** (faster, type-safe)?
   - **Message queue** (async)?

Would you like me to switch to Agent mode to create the project structure with all these patterns implemented?

---

**User**

I do not think it is necesary to store the `availableTenantIds` in the JWT, bcause twhen a request is coming in with a specific sessionId-userId-tenantId, that should be enought to find it in the REDIS session store.
Also, the caller doesn't have to know this data from the JWT.
I was thinking a solution where the user needs to switch between tenants is not necessary, but it works by deafult.
Of course, it is required to delete the session from the REDIS when it got expired. Maybe REDIS can delete it automatically if we set an expiration date on it somehow?

Regarding the "2. Custom Domain / Reverse Proxy Setup", I do not want to configure anythign via an nginx proxy, but make it possible other ways to e.g. Squerspace to configure something and direct the traffix to my service with the proepr identifier for a certain site. Also, I want to check this identifier together with the domain name something similar way as you described in "Option B: Domain-to-Tenant Mapping Table"
So this wahy, when a request that does not have a JWT yet comes in (like login request), the auth-service can see what is the tenantId.
So yeah, I do not want to use headers for this, like the `X-Tenant-ID` or the `X-Tenant-Domain`, but directly the JWT or the lookup from the DB.
I also want to be able to use this locally when I open e.g: localhost -> default (Guest) tenant, test1.localhost -> test1 tenant, test2.localhost -> test2 tenant

In the "3. Kotlin Coroutine Context (Instead of ThreadLocal)"
I prefer not t use `WebFilter` and `Mono`, but plain `suspended` functions.

In the "4. Controller Parameter: AuthToken vs SessionData"
I really like this `@HasRole` and `@HasPermission` annotations. Is it possible to use these and wire them into Spring's securtity? I know that there is a built in `@PreAuthorized` annotation for this. So I would not need the custom `AuthorizationAspect` but Spring could handle the security by itself?

For "5. Token Refresh Strategy", I prefer the "Option B: Sliding Session (Simpler)". With only one change,. Please make the values configurable through `application.yaml` or `ConfigurationProperties`.

In the "6. Session Invalidation Strategy"
What is this `SessionRepository` do? Can be only redis used for storing sessions? Or what is the idea behind this `SessionRepository`? Please explaine it to me.

Answers to your question:
1. I do not want to deal with any proxy, reverse-proxy configuration (at least at the moment)

2. If it is possible to integrate Keycloak the way to use full OAuth2 flow with the user password provided by the user and the userId provided by the user-service, then I would like the Full OAuth flow.

3. the 2 service will communicate through REST, but with KTor async client. But currently this is out of scope. So the client implementation should contain only hardcoded values.

---

**Cursor**

Great clarifications! Let me address each point:

---

## 1. Simplified JWT (Without availableTenantIds)

You're absolutely right. The simplified JWT:

```kotlin
data class AuthToken(
    val userId: String,
    val sessionId: String,
    val tenantId: String,
    val issuedAt: Instant,
    val expiresAt: Instant
)
```

**JWT Claims:**
```json
{
  "userId": "usr_abc123",
  "sessionId": "ses_xyz789",
  "tenantId": "tenant_acme",
  "iat": 1699999999,
  "exp": 1700003599
}
```

**Tenant Switching Flow:**
When user switches tenant, they simply login again (or call a switch endpoint that creates a new session):
- Old session remains valid (or is invalidated based on config)
- New session created for new tenant
- New JWT issued

This is cleaner and more secure.

---

## 2. Redis TTL for Automatic Expiration

Yes! Redis has built-in TTL (Time To Live). When you set a key, you can specify expiration:

```kotlin
// Using Spring Data Redis with coroutines
suspend fun saveSession(session: SessionData) {
    val key = "session:${session.tenantId}:${session.sessionId}"
    val ttl = Duration.between(Instant.now(), session.expiresAt)
    
    redisTemplate.opsForValue()
        .setAndAwait(key, session, ttl)  // Auto-deletes after TTL
}
```

Redis will automatically delete the key when TTL expires - no cleanup job needed.

---

## 3. Domain Resolution (Without Proxy Headers)

Pure Host header + Database lookup approach:

```kotlin
@Service
class TenantResolverService(
    private val tenantDomainRepository: TenantDomainRepository
) {
    
    suspend fun resolveTenantId(host: String): String {
        // 1. Check for custom domain first (e.g., "my-super-domain.com")
        tenantDomainRepository.findByCustomDomain(host)?.let { 
            return it.tenantId 
        }
        
        // 2. Extract subdomain and lookup
        val subdomain = extractSubdomain(host)
        
        // 3. Handle localhost for local development
        //    localhost -> "default" (guest)
        //    test1.localhost -> "test1"
        //    test2.localhost -> "test2"
        if (subdomain == null || subdomain.isBlank()) {
            return "default"  // Guest tenant
        }
        
        // 4. Lookup by subdomain
        return tenantDomainRepository.findBySubdomain(subdomain)?.tenantId
            ?: throw TenantNotFoundException("Unknown tenant: $subdomain")
    }
    
    private fun extractSubdomain(host: String): String? {
        // Remove port if present
        val hostWithoutPort = host.substringBefore(":")
        
        // Handle localhost variants
        return when {
            hostWithoutPort == "localhost" -> null
            hostWithoutPort.endsWith(".localhost") -> 
                hostWithoutPort.removeSuffix(".localhost")
            
            // Handle your production domain (e.g., example.com)
            hostWithoutPort.endsWith(".example.com") -> 
                hostWithoutPort.removeSuffix(".example.com")
            
            // Custom domain (no subdomain extraction needed)
            else -> null
        }
    }
}
```

**Domain Mapping Entity:**

```kotlin
data class TenantDomainMapping(
    val tenantId: String,
    val subdomain: String?,           // "acme" for acme.example.com
    val customDomain: String?,        // "my-super-domain.com"
    val isActive: Boolean,
    val createdAt: Instant
)
```

**For Squarespace/External DNS:**
They would configure a CNAME record pointing to your service, and you'd have a record in your DB mapping that domain to a tenant.

---

## 4. Pure Suspended Functions (No WebFilter/Mono)

With Spring Boot 4.x and Kotlin coroutines, you can use `CoWebFilter`:

```kotlin
@Component
class AuthContextCoFilter(
    private val jwtService: JwtService,
    private val sessionRepository: SessionRepository,
    private val tenantResolver: TenantResolverService
) : CoWebFilter() {
    
    override suspend fun filter(
        exchange: ServerWebExchange, 
        chain: CoWebFilterChain
    ) {
        val request = exchange.request
        val host = request.headers.host?.hostString ?: "localhost"
        
        // Resolve tenant from host (for unauthenticated requests like login)
        val resolvedTenantId = tenantResolver.resolveTenantId(host)
        
        // Check for JWT
        val token = jwtService.extractTokenOrNull(request)
        
        val authContext = if (token != null) {
            // Authenticated request - load session from Redis
            val session = sessionRepository.findById(token.tenantId, token.sessionId)
                ?: throw SessionExpiredException("Session not found")
            
            // Verify tenant matches
            if (token.tenantId != resolvedTenantId) {
                throw TenantMismatchException("Token tenant doesn't match request domain")
            }
            
            AuthContext(token = token, session = session, tenantId = token.tenantId)
        } else {
            // Unauthenticated request (login, public endpoints)
            AuthContext(token = null, session = null, tenantId = resolvedTenantId)
        }
        
        // Propagate through coroutine context
        withContext(authContext) {
            chain.filter(exchange)
        }
    }
}
```

---

## 5. Spring Security Integration (@PreAuthorize)

Yes! Spring Security has excellent support for method security. You can use the built-in annotations:

### Option A: Use Spring's Built-in Annotations Directly

```kotlin
@RestController
@RequestMapping("/api/v1/users")
class UserController(private val userService: UserService) {
    
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    suspend fun getAdminData(session: SessionData): AdminResponse {
        return adminService.getData(session.tenantId)
    }
    
    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('users:delete')")
    suspend fun deleteUser(
        @PathVariable userId: String,
        session: SessionData
    ) {
        userService.delete(userId, session.tenantId)
    }
    
    @GetMapping("/sensitive")
    @PreAuthorize("hasAuthority('SCOPE_sensitive:read')")
    suspend fun getSensitiveData(session: SessionData): SensitiveResponse {
        return sensitiveService.getData(session)
    }
}
```

### Option B: Create Custom Annotations That Map to @PreAuthorize

```kotlin
// Custom annotations that are more readable
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@PreAuthorize("hasRole('ADMIN')")
annotation class RequireAdmin

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@PreAuthorize("hasAuthority('users:read')")
annotation class CanReadUsers

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@PreAuthorize("hasAuthority('users:delete')")
annotation class CanDeleteUsers

// Usage
@RestController
class UserController {
    
    @GetMapping("/admin")
    @RequireAdmin
    suspend fun getAdminData(session: SessionData): AdminResponse {
        // ...
    }
    
    @DeleteMapping("/{userId}")
    @RequireAdmin
    @CanDeleteUsers
    suspend fun deleteUser(@PathVariable userId: String, session: SessionData) {
        // ...
    }
}
```

### Option C: Parameterized Custom Annotations (More Flexible)

For dynamic roles/permissions, you need SpEL (Spring Expression Language):

```kotlin
// These work with SpEL
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@PreAuthorize("hasRole(#root.args[0])")  // First argument is the role
annotation class HasRole(val value: String)

// But simpler approach - use @PreAuthorize directly with expressions:
@PreAuthorize("@authService.hasPermission(#session, 'users:delete')")
suspend fun deleteUser(session: SessionData) { }
```

### Security Configuration

```kotlin
@Configuration
@EnableReactiveMethodSecurity  // For WebFlux
class SecurityConfig {
    
    @Bean
    fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http
            .csrf { it.disable() }
            .authorizeExchange { exchanges ->
                exchanges
                    .pathMatchers("/auth/login", "/auth/register").permitAll()
                    .pathMatchers("/public/**").permitAll()
                    .anyExchange().authenticated()
            }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { }  // If using JWT validation
            }
            .build()
    }
}
```

### Populating Spring Security Context from SessionData

```kotlin
@Component
class SessionAuthenticationConverter(
    private val sessionRepository: SessionRepository
) : ServerAuthenticationConverter {
    
    override fun convert(exchange: ServerWebExchange): Mono<Authentication> = mono {
        val token = extractToken(exchange) ?: return@mono null
        
        val session = sessionRepository.findById(token.tenantId, token.sessionId)
            ?: return@mono null
        
        // Convert to Spring Security Authentication
        SessionAuthentication(
            session = session,
            authorities = session.roles.map { SimpleGrantedAuthority("ROLE_$it") } +
                         session.permissions.map { SimpleGrantedAuthority(it) } +
                         session.scopes.map { SimpleGrantedAuthority("SCOPE_$it") }
        )
    }
}

class SessionAuthentication(
    val session: SessionData,
    private val authorities: List<GrantedAuthority>
) : Authentication {
    override fun getAuthorities() = authorities
    override fun getCredentials() = null
    override fun getDetails() = session
    override fun getPrincipal() = session
    override fun isAuthenticated() = true
    override fun setAuthenticated(isAuthenticated: Boolean) { }
    override fun getName() = session.userId
}
```

---

## 6. Sliding Session with ConfigurationProperties

```kotlin
@ConfigurationProperties(prefix = "auth.session")
data class SessionProperties(
    val tokenExpirationMinutes: Long = 60,           // JWT lifetime
    val refreshWindowMinutes: Long = 5,              // Refresh if expires within this
    val sessionTimeoutMinutes: Long = 1440,          // Redis session TTL (24h)
    val slidingSessionEnabled: Boolean = true
)

// application.yml
auth:
  session:
    token-expiration-minutes: 60
    refresh-window-minutes: 5
    session-timeout-minutes: 1440
    sliding-session-enabled: true
```

**Sliding Session Filter:**

```kotlin
@Component
class SlidingSessionCoFilter(
    private val jwtService: JwtService,
    private val sessionProperties: SessionProperties
) : CoWebFilter() {
    
    override suspend fun filter(exchange: ServerWebExchange, chain: CoWebFilterChain) {
        chain.filter(exchange)
        
        // After request processing, check if token needs refresh
        if (sessionProperties.slidingSessionEnabled) {
            val token = jwtService.extractTokenOrNull(exchange.request)
            if (token != null && shouldRefresh(token)) {
                val newToken = jwtService.refresh(token)
                exchange.response.headers.add("X-Refreshed-Token", newToken)
            }
        }
    }
    
    private fun shouldRefresh(token: AuthToken): Boolean {
        val refreshThreshold = Instant.now()
            .plus(sessionProperties.refreshWindowMinutes, ChronoUnit.MINUTES)
        return token.expiresAt.isBefore(refreshThreshold)
    }
}
```

---

## 7. SessionRepository Explanation

`SessionRepository` is an **abstraction layer** (interface) over your session storage. Here's why:

### The Interface

```kotlin
interface SessionRepository {
    suspend fun save(session: SessionData): SessionData
    suspend fun findById(tenantId: String, sessionId: String): SessionData?
    suspend fun deleteById(tenantId: String, sessionId: String)
    suspend fun findAllByUserId(userId: String): List<SessionData>
    suspend fun deleteAllByUserId(userId: String)
    suspend fun deleteAllByUserIdAndTenantId(userId: String, tenantId: String)
}
```

### Why Use an Abstraction?

1. **Testability**: In tests, you can use an in-memory implementation instead of Redis
2. **Flexibility**: If you ever need to switch storage (Redis → Memcached → Database), only the implementation changes
3. **Clean Architecture**: Business logic doesn't depend on infrastructure details

### For Your Case: Redis-Only Implementation

Since you only want Redis:

```kotlin
@Repository
class RedisSessionRepository(
    private val redisTemplate: ReactiveStringRedisTemplate,
    private val objectMapper: ObjectMapper,
    private val sessionProperties: SessionProperties
) : SessionRepository {
    
    override suspend fun save(session: SessionData): SessionData {
        val key = sessionKey(session.tenantId, session.sessionId)
        val value = objectMapper.writeValueAsString(session)
        val ttl = Duration.ofMinutes(sessionProperties.sessionTimeoutMinutes)
        
        redisTemplate.opsForValue()
            .setAndAwait(key, value, ttl)
        
        // Also index by userId for "logout all" functionality
        redisTemplate.opsForSet()
            .addAndAwait(userSessionsKey(session.userId), session.sessionId)
        
        redisTemplate.opsForSet()
            .addAndAwait(
                userTenantSessionsKey(session.userId, session.tenantId), 
                session.sessionId
            )
        
        return session
    }
    
    override suspend fun findById(tenantId: String, sessionId: String): SessionData? {
        val key = sessionKey(tenantId, sessionId)
        val value = redisTemplate.opsForValue().getAndAwait(key) ?: return null
        return objectMapper.readValue(value, SessionData::class.java)
    }
    
    override suspend fun deleteById(tenantId: String, sessionId: String) {
        val key = sessionKey(tenantId, sessionId)
        
        // Get session first to clean up indexes
        findById(tenantId, sessionId)?.let { session ->
            redisTemplate.opsForSet()
                .removeAndAwait(userSessionsKey(session.userId), sessionId)
            redisTemplate.opsForSet()
                .removeAndAwait(
                    userTenantSessionsKey(session.userId, tenantId), 
                    sessionId
                )
        }
        
        redisTemplate.deleteAndAwait(key)
    }
    
    override suspend fun deleteAllByUserId(userId: String) {
        val sessionIds = redisTemplate.opsForSet()
            .membersAndAwait(userSessionsKey(userId))
        
        // Note: We need tenant IDs to build keys, so we'd need to store that in the index
        // Alternative: scan for keys matching pattern session:*:{sessionId}
        sessionIds.forEach { sessionId ->
            // Delete sessions across all tenants
            redisTemplate.execute { connection ->
                connection.keyCommands()
                    .scan(ScanOptions.scanOptions().match("session:*:$sessionId").build())
            }.collect { key ->
                redisTemplate.deleteAndAwait(key)
            }
        }
        
        redisTemplate.deleteAndAwait(userSessionsKey(userId))
    }
    
    private fun sessionKey(tenantId: String, sessionId: String) = 
        "session:$tenantId:$sessionId"
    
    private fun userSessionsKey(userId: String) = 
        "user_sessions:$userId"
    
    private fun userTenantSessionsKey(userId: String, tenantId: String) = 
        "user_tenant_sessions:$userId:$tenantId"
}
```

### For Testing: In-Memory Implementation

```kotlin
class InMemorySessionRepository : SessionRepository {
    private val sessions = ConcurrentHashMap<String, SessionData>()
    
    override suspend fun save(session: SessionData): SessionData {
        sessions["${session.tenantId}:${session.sessionId}"] = session
        return session
    }
    
    override suspend fun findById(tenantId: String, sessionId: String): SessionData? {
        return sessions["$tenantId:$sessionId"]
    }
    
    // ... other methods
}
```

---

## 8. Keycloak Full OAuth2/OIDC Flow

For full OAuth2 with Keycloak using the **Resource Owner Password Credentials (ROPC)** grant:

```
┌────────┐     1. Login Request      ┌──────────────┐
│ Client │ ──────────────────────────▶│ Auth Service │
│        │   {email, password}       │              │
└────────┘                           └──────┬───────┘
                                            │
         2. Lookup userId                   ▼
         ┌──────────────────────────────────────────┐
         │                                          │
         │  ┌─────────────┐     ┌──────────────┐    │
         │  │ User Service│◀────│ Auth Service │    │
         │  │ (mocked)    │     │              │    │
         │  └─────────────┘     └──────┬───────┘    │
         │     {userId}                │            │
         └─────────────────────────────┼────────────┘
                                       │
         3. Authenticate with          ▼
            Keycloak using
            userId + password
         ┌──────────────────────────────────────────┐
         │                                          │
         │  ┌─────────────┐     ┌──────────────┐    │
         │  │  Keycloak   │◀────│ Auth Service │    │
         │  │             │     │              │    │
         │  └──────┬──────┘     └──────────────┘    │
         │         │                                │
         │         ▼ Keycloak tokens                │
         │   {access_token, refresh_token, id_token}│
         └──────────────────────────────────────────┘
                                       │
         4. Create session in Redis    ▼
            Issue our own minimal JWT
         ┌──────────────────────────────────────────┐
         │  ┌─────────────┐     ┌──────────────┐    │
         │  │    Redis    │◀────│ Auth Service │    │
         │  │ (sessions)  │     │              │    │
         │  └─────────────┘     └──────┬───────┘    │
         │                             │            │
         └─────────────────────────────┼────────────┘
                                       │
┌────────┐     5. Our JWT              │
│ Client │ ◀───────────────────────────┘
│        │   {userId, sessionId, tenantId}
└────────┘
```

**Important Note**: The ROPC grant (password grant) is deprecated in OAuth 2.1 for security reasons. However, if you control both the client and the auth server (Keycloak), it can still be used for internal services.

**Alternative**: Consider using Keycloak's **Direct Access Grant** or implementing a **Backend-for-Frontend (BFF)** pattern.

---

## Updated Architecture Summary

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    Auth Service - Final Architecture                         │
└─────────────────────────────────────────────────────────────────────────────┘

Request Flow:
─────────────
1. Request arrives with Host header (e.g., "acme.example.com" or "custom.com")
2. TenantResolver extracts tenantId from Host (DB lookup, no headers needed)
3. If JWT present: validate and load SessionData from Redis
4. Populate Spring Security context with roles/permissions
5. @PreAuthorize annotations handle authorization
6. Sliding session issues new token if near expiry

Data Model:
───────────
JWT (minimal):           SessionData (Redis):
┌─────────────────┐      ┌────────────────────────────┐
│ userId          │      │ sessionId                  │
│ sessionId       │      │ userId                     │
│ tenantId        │      │ tenantId                   │
│ issuedAt        │      │ username                   │
│ expiresAt       │      │ email                      │
└─────────────────┘      │ roles: Set<String>         │
                         │ permissions: Set<String>   │
                         │ scopes: Set<String>        │
                         │ keycloakTokens: {...}      │
                         │ createdAt                  │
                         │ lastAccessedAt             │
                         │ expiresAt                  │
                         └────────────────────────────┘
                              TTL auto-expires ↑

Redis Keys:
───────────
session:{tenantId}:{sessionId}           → SessionData (with TTL)
user_sessions:{userId}                   → Set<sessionId>
user_tenant_sessions:{userId}:{tenantId} → Set<sessionId>
tenant_domains:{domain}                  → tenantId
tenant_subdomains:{subdomain}            → tenantId
```

---

Would you like me to switch to Agent mode to create the project structure with all these patterns implemented?

---

**User**

In the "3. Domain Resolution (Without Proxy Headers)" section, can you include the `CNAME` resolution also?
Of course resolving `localhost` is required only in development/local run, so maybe a separate implementation for `local` and other environment usage would be better then hardcoding the values in the production code. Can you create 2 separate classes/implementation for the `TenantResolverService`?

In "5. Spring Security Integration (@PreAuthorize)", I like the "Option B: Create Custom Annotations That Map to @PreAuthorize", but I want to use the `@HassRole` and `@HasPermission` annotations like `@HasRole("ADMIN")` or `@HasAnyRole("MEMBER", "USER")` and `@HasPermission("som-resource:read")` or `@HasAnyPermission("some-resource:read", "some-resource:read:admin")`
But wired with Spring's secutiry management.
So something like you did in "Option C: Parameterized Custom Annotations (More Flexible)", but with custom annotations.

In "8. Keycloak Full OAuth2/OIDC Flow" section, you mention that:

> The ROPC grant (password grant) is deprecated in OAuth 2.1 for security reasons.

Please update the specs to align with OAuth 2.1 but kee in mind that the email address/user name is resolved into a `useId` that is coming from the user-service.


---

**Cursor**

Let me address each of these points in detail.

---

## 1. TenantResolverService - Separate Implementations

### Interface Definition

```kotlin
interface TenantResolverService {
    suspend fun resolveTenantId(host: String): String
}
```

### Production Implementation (with CNAME/Custom Domain Resolution)

```kotlin
@Service
@Profile("!local")  // Active in all profiles except "local"
class ProductionTenantResolverService(
    private val tenantDomainRepository: TenantDomainRepository,
    private val dnsResolver: DnsResolverService,
    private val tenantProperties: TenantProperties
) : TenantResolverService {
    
    private val logger = LoggerFactory.getLogger(javaClass)
    
    override suspend fun resolveTenantId(host: String): String {
        val hostWithoutPort = host.substringBefore(":")
        
        // 1. Direct custom domain lookup (e.g., "my-super-domain.com")
        tenantDomainRepository.findByCustomDomain(hostWithoutPort)?.let { mapping ->
            if (mapping.isActive) {
                logger.debug("Resolved tenant '${mapping.tenantId}' from custom domain '$hostWithoutPort'")
                return mapping.tenantId
            }
        }
        
        // 2. Subdomain lookup (e.g., "acme.example.com" -> "acme")
        val subdomain = extractSubdomain(hostWithoutPort)
        if (subdomain != null) {
            tenantDomainRepository.findBySubdomain(subdomain)?.let { mapping ->
                if (mapping.isActive) {
                    logger.debug("Resolved tenant '${mapping.tenantId}' from subdomain '$subdomain'")
                    return mapping.tenantId
                }
            }
        }
        
        // 3. CNAME resolution - check if this domain CNAMEs to one of our known domains
        val resolvedTenantId = resolveTenantViaCname(hostWithoutPort)
        if (resolvedTenantId != null) {
            logger.debug("Resolved tenant '$resolvedTenantId' via CNAME from '$hostWithoutPort'")
            return resolvedTenantId
        }
        
        // 4. Default tenant (if configured)
        if (tenantProperties.defaultTenantId != null) {
            logger.debug("Using default tenant '${tenantProperties.defaultTenantId}' for '$hostWithoutPort'")
            return tenantProperties.defaultTenantId
        }
        
        throw TenantNotFoundException("Cannot resolve tenant for host: $hostWithoutPort")
    }
    
    private fun extractSubdomain(host: String): String? {
        val baseDomains = tenantProperties.baseDomains  // e.g., ["example.com", "example.io"]
        
        for (baseDomain in baseDomains) {
            if (host.endsWith(".$baseDomain")) {
                val subdomain = host.removeSuffix(".$baseDomain")
                // Ensure it's a single-level subdomain (no dots)
                if (!subdomain.contains(".")) {
                    return subdomain
                }
            }
        }
        return null
    }
    
    /**
     * Resolves tenant by following CNAME chain.
     * 
     * Example: 
     *   Customer configures: my-super-domain.com CNAME acme.example.com
     *   We resolve: my-super-domain.com -> acme.example.com -> tenant "acme"
     */
    private suspend fun resolveTenantViaCname(host: String): String? {
        val cnameChain = dnsResolver.resolveCnameChain(host)
        
        for (cname in cnameChain) {
            // Check if CNAME points to our subdomain
            val subdomain = extractSubdomain(cname)
            if (subdomain != null) {
                tenantDomainRepository.findBySubdomain(subdomain)?.let { mapping ->
                    if (mapping.isActive) {
                        // Cache this custom domain -> tenant mapping for future lookups
                        cacheCustomDomainMapping(host, mapping.tenantId)
                        return mapping.tenantId
                    }
                }
            }
            
            // Check if CNAME points to a registered custom domain
            tenantDomainRepository.findByCustomDomain(cname)?.let { mapping ->
                if (mapping.isActive) {
                    return mapping.tenantId
                }
            }
        }
        
        return null
    }
    
    private suspend fun cacheCustomDomainMapping(domain: String, tenantId: String) {
        // Optionally cache discovered CNAME mappings to avoid repeated DNS lookups
        // This could write to Redis with a TTL
    }
}
```

### DNS Resolver Service

```kotlin
interface DnsResolverService {
    suspend fun resolveCnameChain(host: String): List<String>
}

@Service
@Profile("!local")
class ProductionDnsResolverService : DnsResolverService {
    
    private val logger = LoggerFactory.getLogger(javaClass)
    
    override suspend fun resolveCnameChain(host: String): List<String> = withContext(Dispatchers.IO) {
        val chain = mutableListOf<String>()
        var currentHost = host
        val visited = mutableSetOf<String>()
        
        repeat(10) {  // Max 10 CNAME hops to prevent infinite loops
            if (currentHost in visited) {
                logger.warn("CNAME loop detected for $host")
                return@withContext chain
            }
            visited.add(currentHost)
            
            try {
                val env = Hashtable<String, String>().apply {
                    put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory")
                }
                val dirContext = InitialDirContext(env)
                val attributes = dirContext.getAttributes(currentHost, arrayOf("CNAME"))
                val cnameAttr = attributes.get("CNAME")
                
                if (cnameAttr != null) {
                    val cname = cnameAttr.get().toString().removeSuffix(".")
                    chain.add(cname)
                    currentHost = cname
                } else {
                    // No more CNAMEs
                    return@withContext chain
                }
            } catch (e: NamingException) {
                // No CNAME record found
                return@withContext chain
            }
        }
        
        chain
    }
}
```

### Local Development Implementation

```kotlin
@Service
@Profile("local")
class LocalTenantResolverService(
    private val localTenantProperties: LocalTenantProperties
) : TenantResolverService {
    
    private val logger = LoggerFactory.getLogger(javaClass)
    
    override suspend fun resolveTenantId(host: String): String {
        val hostWithoutPort = host.substringBefore(":")
        
        // 1. Check explicit mappings from config
        localTenantProperties.domainMappings[hostWithoutPort]?.let { tenantId ->
            logger.debug("Resolved tenant '$tenantId' from explicit mapping for '$hostWithoutPort'")
            return tenantId
        }
        
        // 2. Extract subdomain from localhost (e.g., "test1.localhost" -> "test1")
        val subdomain = extractLocalSubdomain(hostWithoutPort)
        
        // 3. Check subdomain mappings
        if (subdomain != null) {
            localTenantProperties.subdomainMappings[subdomain]?.let { tenantId ->
                logger.debug("Resolved tenant '$tenantId' from subdomain '$subdomain'")
                return tenantId
            }
            
            // If no explicit mapping, use subdomain as tenant ID (convenient for testing)
            if (localTenantProperties.useSubdomainAsTenantId) {
                logger.debug("Using subdomain '$subdomain' directly as tenant ID")
                return subdomain
            }
        }
        
        // 4. Default tenant for plain localhost
        logger.debug("Using default tenant '${localTenantProperties.defaultTenantId}' for '$hostWithoutPort'")
        return localTenantProperties.defaultTenantId
    }
    
    private fun extractLocalSubdomain(host: String): String? {
        val localDomains = listOf("localhost", "127.0.0.1", "local.test")
        
        for (localDomain in localDomains) {
            if (host == localDomain) {
                return null  // No subdomain
            }
            if (host.endsWith(".$localDomain")) {
                return host.removeSuffix(".$localDomain")
            }
        }
        return null
    }
}

@Service
@Profile("local")
class LocalDnsResolverService : DnsResolverService {
    // No-op for local development
    override suspend fun resolveCnameChain(host: String): List<String> = emptyList()
}
```

### Configuration Properties

```kotlin
@ConfigurationProperties(prefix = "auth.tenant")
data class TenantProperties(
    val baseDomains: List<String> = listOf("example.com"),
    val defaultTenantId: String? = "default"
)

@ConfigurationProperties(prefix = "auth.tenant.local")
data class LocalTenantProperties(
    val defaultTenantId: String = "default",
    val useSubdomainAsTenantId: Boolean = true,
    val domainMappings: Map<String, String> = emptyMap(),
    val subdomainMappings: Map<String, String> = emptyMap()
)
```

### Configuration Files

**application.yml (production):**
```yaml
auth:
  tenant:
    base-domains:
      - example.com
      - example.io
    default-tenant-id: null  # No default in production - must resolve explicitly
```

**application-local.yml:**
```yaml
spring:
  profiles:
    active: local

auth:
  tenant:
    local:
      default-tenant-id: default
      use-subdomain-as-tenant-id: true
      # Explicit mappings for testing specific scenarios
      domain-mappings:
        "custom-test.local": tenant-custom
      subdomain-mappings:
        test1: tenant-test1
        test2: tenant-test2
        admin: tenant-admin
```

**Example local usage:**
| Host | Resolved Tenant ID |
|------|-------------------|
| `localhost` | `default` |
| `localhost:8080` | `default` |
| `test1.localhost` | `tenant-test1` (from explicit mapping) |
| `test2.localhost` | `tenant-test2` (from explicit mapping) |
| `acme.localhost` | `acme` (subdomain as tenant ID) |
| `custom-test.local` | `tenant-custom` (explicit domain mapping) |

---

## 2. Custom Security Annotations with Spring Security

### The Annotations

```kotlin
/**
 * Requires the user to have the specified role.
 * Role names are automatically prefixed with "ROLE_" for Spring Security.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class HasRole(val value: String)

/**
 * Requires the user to have at least one of the specified roles.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class HasAnyRole(vararg val value: String)

/**
 * Requires the user to have all of the specified roles.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class HasAllRoles(vararg val value: String)

/**
 * Requires the user to have the specified permission (authority).
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class HasPermission(val value: String)

/**
 * Requires the user to have at least one of the specified permissions.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class HasAnyPermission(vararg val value: String)

/**
 * Requires the user to have all of the specified permissions.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class HasAllPermissions(vararg val value: String)
```

### Custom Method Security Expression Handler

To wire these custom annotations into Spring Security, we need to create a custom `MethodSecurityExpressionHandler` and an `AuthorizationManager`:

```kotlin
@Configuration
@EnableReactiveMethodSecurity
class MethodSecurityConfig {
    
    @Bean
    fun hasRoleAuthorizationManager(): ReactiveAuthorizationManager<MethodInvocation> {
        return HasRoleAuthorizationManager()
    }
    
    @Bean
    fun hasPermissionAuthorizationManager(): ReactiveAuthorizationManager<MethodInvocation> {
        return HasPermissionAuthorizationManager()
    }
}
```

### Authorization Managers

```kotlin
class HasRoleAuthorizationManager : ReactiveAuthorizationManager<MethodInvocation> {
    
    override fun check(
        authentication: Mono<Authentication>,
        invocation: MethodInvocation
    ): Mono<AuthorizationDecision> {
        return authentication.map { auth ->
            val method = invocation.method
            
            // Check @HasRole
            method.getAnnotation(HasRole::class.java)?.let { annotation ->
                val requiredRole = "ROLE_${annotation.value}"
                return@map AuthorizationDecision(
                    auth.authorities.any { it.authority == requiredRole }
                )
            }
            
            // Check @HasAnyRole
            method.getAnnotation(HasAnyRole::class.java)?.let { annotation ->
                val requiredRoles = annotation.value.map { "ROLE_$it" }.toSet()
                return@map AuthorizationDecision(
                    auth.authorities.any { it.authority in requiredRoles }
                )
            }
            
            // Check @HasAllRoles
            method.getAnnotation(HasAllRoles::class.java)?.let { annotation ->
                val requiredRoles = annotation.value.map { "ROLE_$it" }.toSet()
                val userRoles = auth.authorities.map { it.authority }.toSet()
                return@map AuthorizationDecision(
                    userRoles.containsAll(requiredRoles)
                )
            }
            
            // No role annotation - allow
            AuthorizationDecision(true)
        }.defaultIfEmpty(AuthorizationDecision(false))
    }
}

class HasPermissionAuthorizationManager : ReactiveAuthorizationManager<MethodInvocation> {
    
    override fun check(
        authentication: Mono<Authentication>,
        invocation: MethodInvocation
    ): Mono<AuthorizationDecision> {
        return authentication.map { auth ->
            val method = invocation.method
            
            // Check @HasPermission
            method.getAnnotation(HasPermission::class.java)?.let { annotation ->
                return@map AuthorizationDecision(
                    auth.authorities.any { it.authority == annotation.value }
                )
            }
            
            // Check @HasAnyPermission
            method.getAnnotation(HasAnyPermission::class.java)?.let { annotation ->
                val requiredPermissions = annotation.value.toSet()
                return@map AuthorizationDecision(
                    auth.authorities.any { it.authority in requiredPermissions }
                )
            }
            
            // Check @HasAllPermissions
            method.getAnnotation(HasAllPermissions::class.java)?.let { annotation ->
                val requiredPermissions = annotation.value.toSet()
                val userPermissions = auth.authorities.map { it.authority }.toSet()
                return@map AuthorizationDecision(
                    userPermissions.containsAll(requiredPermissions)
                )
            }
            
            // No permission annotation - allow
            AuthorizationDecision(true)
        }.defaultIfEmpty(AuthorizationDecision(false))
    }
}
```

### Better Approach: Custom Security Expressions with SpEL

A cleaner approach that integrates fully with Spring Security's expression-based system:

```kotlin
@Configuration
@EnableReactiveMethodSecurity
class SecurityExpressionConfig {
    
    @Bean
    fun authSecurityExpressionRoot(): AuthSecurityExpressionRoot {
        return AuthSecurityExpressionRoot()
    }
}

/**
 * Custom security expressions available in @PreAuthorize annotations.
 * 
 * Usage:
 *   @PreAuthorize("@auth.hasRole('ADMIN')")
 *   @PreAuthorize("@auth.hasAnyRole('ADMIN', 'MODERATOR')")
 *   @PreAuthorize("@auth.hasPermission('users:delete')")
 */
@Component("auth")
class AuthSecurityExpressionRoot {
    
    fun hasRole(role: String): Boolean {
        val authentication = ReactiveSecurityContextHolder.getContext()
            .map { it.authentication }
            .block() ?: return false
        
        return authentication.authorities.any { it.authority == "ROLE_$role" }
    }
    
    fun hasAnyRole(vararg roles: String): Boolean {
        val authentication = ReactiveSecurityContextHolder.getContext()
            .map { it.authentication }
            .block() ?: return false
        
        val requiredRoles = roles.map { "ROLE_$it" }.toSet()
        return authentication.authorities.any { it.authority in requiredRoles }
    }
    
    fun hasAllRoles(vararg roles: String): Boolean {
        val authentication = ReactiveSecurityContextHolder.getContext()
            .map { it.authentication }
            .block() ?: return false
        
        val requiredRoles = roles.map { "ROLE_$it" }.toSet()
        val userRoles = authentication.authorities.map { it.authority }.toSet()
        return userRoles.containsAll(requiredRoles)
    }
    
    fun hasPermission(permission: String): Boolean {
        val authentication = ReactiveSecurityContextHolder.getContext()
            .map { it.authentication }
            .block() ?: return false
        
        return authentication.authorities.any { it.authority == permission }
    }
    
    fun hasAnyPermission(vararg permissions: String): Boolean {
        val authentication = ReactiveSecurityContextHolder.getContext()
            .map { it.authentication }
            .block() ?: return false
        
        val requiredPermissions = permissions.toSet()
        return authentication.authorities.any { it.authority in requiredPermissions }
    }
    
    fun hasAllPermissions(vararg permissions: String): Boolean {
        val authentication = ReactiveSecurityContextHolder.getContext()
            .map { it.authentication }
            .block() ?: return false
        
        val requiredPermissions = permissions.toSet()
        val userPermissions = authentication.authorities.map { it.authority }.toSet()
        return userPermissions.containsAll(requiredPermissions)
    }
}
```

### Custom Annotations That Wrap @PreAuthorize

Now we can create the nice annotations that delegate to our expression bean:

```kotlin
// === Role Annotations ===

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@PreAuthorize("@auth.hasRole(#root.target.class.getMethod(#root.method.name, *#root.method.parameterTypes).getAnnotation(T(com.example.auth.security.HasRole)).value)")
annotation class HasRole(val value: String)

// The above is complex. Simpler approach using meta-annotations:

/**
 * Requires ADMIN role
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@PreAuthorize("@auth.hasRole('ADMIN')")
annotation class RequireAdmin

/**
 * Requires USER role
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@PreAuthorize("@auth.hasRole('USER')")
annotation class RequireUser
```

### Best Approach: AOP with Spring Security Integration

For fully parameterized annotations, use AOP that integrates with Spring Security:

```kotlin
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class SecurityAnnotationAspect(
    private val authorizationService: AuthorizationService
) {
    
    @Around("@annotation(hasRole)")
    fun checkHasRole(joinPoint: ProceedingJoinPoint, hasRole: HasRole): Any? {
        authorizationService.requireRole(hasRole.value)
        return joinPoint.proceed()
    }
    
    @Around("@annotation(hasAnyRole)")
    fun checkHasAnyRole(joinPoint: ProceedingJoinPoint, hasAnyRole: HasAnyRole): Any? {
        authorizationService.requireAnyRole(*hasAnyRole.value)
        return joinPoint.proceed()
    }
    
    @Around("@annotation(hasAllRoles)")
    fun checkHasAllRoles(joinPoint: ProceedingJoinPoint, hasAllRoles: HasAllRoles): Any? {
        authorizationService.requireAllRoles(*hasAllRoles.value)
        return joinPoint.proceed()
    }
    
    @Around("@annotation(hasPermission)")
    fun checkHasPermission(joinPoint: ProceedingJoinPoint, hasPermission: HasPermission): Any? {
        authorizationService.requirePermission(hasPermission.value)
        return joinPoint.proceed()
    }
    
    @Around("@annotation(hasAnyPermission)")
    fun checkHasAnyPermission(joinPoint: ProceedingJoinPoint, hasAnyPermission: HasAnyPermission): Any? {
        authorizationService.requireAnyPermission(*hasAnyPermission.value)
        return joinPoint.proceed()
    }
    
    @Around("@annotation(hasAllPermissions)")
    fun checkHasAllPermissions(joinPoint: ProceedingJoinPoint, hasAllPermissions: HasAllPermissions): Any? {
        authorizationService.requireAllPermissions(*hasAllPermissions.value)
        return joinPoint.proceed()
    }
}

@Service
class AuthorizationService {
    
    suspend fun requireRole(role: String) {
        val session = currentSession()
        if ("ROLE_$role" !in session.roles && role !in session.roles) {
            throw AccessDeniedException("Required role: $role")
        }
    }
    
    suspend fun requireAnyRole(vararg roles: String) {
        val session = currentSession()
        val userRoles = session.roles
        val hasAny = roles.any { role -> 
            "ROLE_$role" in userRoles || role in userRoles 
        }
        if (!hasAny) {
            throw AccessDeniedException("Required any of roles: ${roles.joinToString()}")
        }
    }
    
    suspend fun requireAllRoles(vararg roles: String) {
        val session = currentSession()
        val userRoles = session.roles
        val missingRoles = roles.filter { role ->
            "ROLE_$role" !in userRoles && role !in userRoles
        }
        if (missingRoles.isNotEmpty()) {
            throw AccessDeniedException("Missing required roles: ${missingRoles.joinToString()}")
        }
    }
    
    suspend fun requirePermission(permission: String) {
        val session = currentSession()
        if (permission !in session.permissions) {
            throw AccessDeniedException("Required permission: $permission")
        }
    }
    
    suspend fun requireAnyPermission(vararg permissions: String) {
        val session = currentSession()
        if (permissions.none { it in session.permissions }) {
            throw AccessDeniedException("Required any of permissions: ${permissions.joinToString()}")
        }
    }
    
    suspend fun requireAllPermissions(vararg permissions: String) {
        val session = currentSession()
        val missingPermissions = permissions.filter { it !in session.permissions }
        if (missingPermissions.isNotEmpty()) {
            throw AccessDeniedException("Missing required permissions: ${missingPermissions.joinToString()}")
        }
    }
}
```

### Usage Examples

```kotlin
@RestController
@RequestMapping("/api/v1/users")
class UserController(
    private val userService: UserService
) {
    
    // Single role check
    @GetMapping("/admin-panel")
    @HasRole("ADMIN")
    suspend fun getAdminPanel(session: SessionData): AdminPanelResponse {
        return userService.getAdminPanel(session.tenantId)
    }
    
    // Multiple roles (any)
    @GetMapping("/dashboard")
    @HasAnyRole("ADMIN", "MODERATOR", "MANAGER")
    suspend fun getDashboard(session: SessionData): DashboardResponse {
        return userService.getDashboard(session)
    }
    
    // Multiple roles (all required)
    @PostMapping("/super-action")
    @HasAllRoles("ADMIN", "VERIFIED")
    suspend fun performSuperAction(session: SessionData): ActionResponse {
        return userService.superAction(session)
    }
    
    // Single permission
    @DeleteMapping("/{userId}")
    @HasPermission("users:delete")
    suspend fun deleteUser(
        @PathVariable userId: String,
        session: SessionData
    ): ResponseEntity<Unit> {
        userService.delete(userId, session.tenantId)
        return ResponseEntity.noContent().build()
    }
    
    // Multiple permissions (any)
    @GetMapping("/{userId}/sensitive")
    @HasAnyPermission("users:read:sensitive", "users:read:admin")
    suspend fun getSensitiveUserData(
        @PathVariable userId: String,
        session: SessionData
    ): SensitiveUserData {
        return userService.getSensitiveData(userId, session.tenantId)
    }
    
    // Combined role and permission
    @PutMapping("/{userId}/role")
    @HasRole("ADMIN")
    @HasPermission("users:modify:role")
    suspend fun updateUserRole(
        @PathVariable userId: String,
        @RequestBody request: UpdateRoleRequest,
        session: SessionData
    ): UserResponse {
        return userService.updateRole(userId, request, session)
    }
}
```

---

## 3. OAuth 2.1 Compliant Flow with Keycloak

OAuth 2.1 removes the Resource Owner Password Credentials (ROPC) grant. The recommended flows are:

1. **Authorization Code Flow with PKCE** - For web/mobile apps
2. **Client Credentials Flow** - For service-to-service
3. **Device Authorization Flow** - For devices without browsers

Since you need to:
1. Accept username/email from user
2. Resolve to `userId` via user-service
3. Authenticate with Keycloak

Here's the OAuth 2.1 compliant approach:

### Option A: Authorization Code Flow with PKCE (Recommended)

This is the standard flow but requires redirect-based auth:

```
┌────────┐                                           ┌──────────────┐
│ Client │                                           │ Auth Service │
│ (SPA)  │                                           │              │
└───┬────┘                                           └──────┬───────┘
    │                                                       │
    │  1. User enters email, clicks "Login"                 │
    │─────────────────────────────────────────────────────▶│
    │                                                       │
    │                    2. Lookup userId from email        │
    │                    ┌─────────────────────────────────▶│
    │                    │  User Service                    │
    │                    │◀─────────────────────────────────│
    │                    │  {userId: "usr_abc123"}          │
    │                                                       │
    │  3. Return auth URL with login_hint                   │
    │◀─────────────────────────────────────────────────────│
    │     {authUrl: "keycloak.../auth?                      │
    │       login_hint=usr_abc123&                          │
    │       code_challenge=..."}                            │
    │                                                       │
    │  4. Redirect to Keycloak                              │
    │───────────────────────────────────────────────────────┼──▶ Keycloak
    │                                                       │
    │  5. User authenticates with password in Keycloak      │
    │◀──────────────────────────────────────────────────────┼─── Keycloak
    │     ?code=AUTH_CODE                                   │
    │                                                       │
    │  6. Exchange code for tokens                          │
    │─────────────────────────────────────────────────────▶│
    │     {code, code_verifier}                             │
    │                                                       │
    │                    7. Exchange code with Keycloak     │
    │                    ┌─────────────────────────────────▶│──▶ Keycloak
    │                    │◀─────────────────────────────────│◀── Keycloak
    │                    │  {access_token, refresh_token}   │
    │                                                       │
    │  8. Create session, return our JWT                    │
    │◀─────────────────────────────────────────────────────│
    │     {token, sessionId}                                │
    │                                                       │
```

### Option B: Backend-for-Frontend (BFF) Pattern (Better for Your Case)

Since you want the auth-service to handle everything and issue its own JWT:

```
┌────────┐     POST /auth/login       ┌──────────────┐
│ Client │ ───────────────────────────▶│ Auth Service │
│        │  {email, password,         │              │
│        │   tenantId (from domain)}  │              │
└────────┘                            └──────┬───────┘
                                             │
    ┌────────────────────────────────────────┼────────────────────────┐
    │                                        ▼                        │
    │  Step 1: Resolve userId from email                              │
    │  ┌──────────────┐      GET /users?email=...   ┌──────────────┐  │
    │  │ Auth Service │ ────────────────────────────▶│ User Service │  │
    │  │              │ ◀────────────────────────────│   (mocked)   │  │
    │  └──────────────┘      {userId: "usr_abc"}    └──────────────┘  │
    │                                                                 │
    │  Step 2: Authenticate via Keycloak Token Exchange               │
    │  ┌──────────────┐                             ┌──────────────┐  │
    │  │ Auth Service │  Token Exchange Grant       │   Keycloak   │  │
    │  │  (confidential│ ─────────────────────────▶│              │  │
    │  │   client)     │   subject_token=userId    │              │  │
    │  │              │ ◀─────────────────────────│              │  │
    │  └──────────────┘   {access_token, ...}      └──────────────┘  │
    │                                                                 │
    └─────────────────────────────────────────────────────────────────┘
                                             │
                                             ▼
    ┌──────────────────────────────────────────────────────────────────┐
    │  Step 3: Validate password (Option: Direct User Service check)   │
    │                                                                  │
    │  If Keycloak doesn't have the password, and user-service does:   │
    │  ┌──────────────┐  POST /users/validate     ┌──────────────┐     │
    │  │ Auth Service │ ─────────────────────────▶│ User Service │     │
    │  │              │  {userId, password}       │              │     │
    │  │              │ ◀─────────────────────────│              │     │
    │  └──────────────┘  {valid: true}            └──────────────┘     │
    │                                                                  │
    │  Then use Token Exchange to get Keycloak tokens for the user     │
    └──────────────────────────────────────────────────────────────────┘
                                             │
                                             ▼
┌────────┐     {token, sessionId}      ┌──────────────┐
│ Client │ ◀───────────────────────────│ Auth Service │
│        │                             │              │
└────────┘                             └──────────────┘
```

### Option C: Keycloak Token Exchange (OAuth 2.1 Compliant)

The **Token Exchange** grant (RFC 8693) is OAuth 2.1 compliant and allows:
- Service-to-service token exchange
- Impersonation (with proper permissions)
- Acting on behalf of users

```kotlin
@Service
class KeycloakAuthenticationService(
    private val keycloakClient: KeycloakClient,
    private val userServiceClient: UserServiceClient,
    private val keycloakProperties: KeycloakProperties
) {
    
    /**
     * OAuth 2.1 compliant authentication flow:
     * 1. Resolve email -> userId via User Service
     * 2. Validate password (User Service or Keycloak Admin API)
     * 3. Use Token Exchange to get tokens for the user
     */
    suspend fun authenticate(
        email: String,
        password: String,
        tenantId: String
    ): AuthenticationResult {
        
        // Step 1: Resolve userId from email
        val userId = userServiceClient.resolveUserId(email, tenantId)
            ?: throw AuthenticationException("User not found")
        
        // Step 2: Validate password
        // Option A: Via Keycloak Admin API
        val isPasswordValid = keycloakClient.validateCredentials(userId, password)
        
        // Option B: Via User Service (if it manages passwords)
        // val isPasswordValid = userServiceClient.validatePassword(userId, password)
        
        if (!isPasswordValid) {
            throw AuthenticationException("Invalid credentials")
        }
        
        // Step 3: Get tokens via Token Exchange
        // Auth service authenticates as itself (client credentials)
        // Then exchanges for user tokens
        val serviceToken = keycloakClient.getClientCredentialsToken()
        
        val userTokens = keycloakClient.tokenExchange(
            subjectToken = serviceToken.accessToken,
            requestedSubject = userId,  // The user we want tokens for
            scope = "openid profile email"
        )
        
        return AuthenticationResult(
            userId = userId,
            keycloakAccessToken = userTokens.accessToken,
            keycloakRefreshToken = userTokens.refreshToken,
            keycloakIdToken = userTokens.idToken
        )
    }
}
```

### Keycloak Client Implementation

```kotlin
interface KeycloakClient {
    suspend fun getClientCredentialsToken(): TokenResponse
    suspend fun tokenExchange(
        subjectToken: String,
        requestedSubject: String,
        scope: String
    ): TokenResponse
    suspend fun validateCredentials(userId: String, password: String): Boolean
    suspend fun introspectToken(token: String): TokenIntrospectionResponse
}

@Service
class KeycloakClientImpl(
    private val httpClient: HttpClient,  // Ktor client
    private val keycloakProperties: KeycloakProperties
) : KeycloakClient {
    
    override suspend fun getClientCredentialsToken(): TokenResponse {
        val response = httpClient.submitForm(
            url = "${keycloakProperties.serverUrl}/realms/${keycloakProperties.realm}/protocol/openid-connect/token",
            formParameters = parameters {
                append("grant_type", "client_credentials")
                append("client_id", keycloakProperties.clientId)
                append("client_secret", keycloakProperties.clientSecret)
            }
        )
        return response.body()
    }
    
    override suspend fun tokenExchange(
        subjectToken: String,
        requestedSubject: String,
        scope: String
    ): TokenResponse {
        val response = httpClient.submitForm(
            url = "${keycloakProperties.serverUrl}/realms/${keycloakProperties.realm}/protocol/openid-connect/token",
            formParameters = parameters {
                append("grant_type", "urn:ietf:params:oauth:grant-type:token-exchange")
                append("client_id", keycloakProperties.clientId)
                append("client_secret", keycloakProperties.clientSecret)
                append("subject_token", subjectToken)
                append("requested_subject", requestedSubject)
                append("scope", scope)
            }
        )
        return response.body()
    }
    
    override suspend fun validateCredentials(userId: String, password: String): Boolean {
        // Use Keycloak Admin API to validate credentials
        val adminToken = getAdminToken()
        
        return try {
            httpClient.post("${keycloakProperties.serverUrl}/admin/realms/${keycloakProperties.realm}/users/$userId/credentials/validate") {
                bearerAuth(adminToken.accessToken)
                contentType(ContentType.Application.Json)
                setBody(mapOf("value" to password))
            }
            true
        } catch (e: ClientRequestException) {
            if (e.response.status == HttpStatusCode.BadRequest) {
                false  // Invalid password
            } else {
                throw e
            }
        }
    }
    
    private suspend fun getAdminToken(): TokenResponse {
        // Get admin token for Keycloak Admin API access
        // This should be cached with refresh logic
        return httpClient.submitForm(
            url = "${keycloakProperties.serverUrl}/realms/master/protocol/openid-connect/token",
            formParameters = parameters {
                append("grant_type", "client_credentials")
                append("client_id", keycloakProperties.adminClientId)
                append("client_secret", keycloakProperties.adminClientSecret)
            }
        ).body()
    }
}
```

### Configuration

```kotlin
@ConfigurationProperties(prefix = "auth.keycloak")
data class KeycloakProperties(
    val serverUrl: String,              // https://keycloak.example.com
    val realm: String,                  // your-realm
    val clientId: String,               // auth-service
    val clientSecret: String,           // secret
    val adminClientId: String,          // admin-cli or custom admin client
    val adminClientSecret: String,
    val tokenExchangeEnabled: Boolean = true
)
```

**application.yml:**
```yaml
auth:
  keycloak:
    server-url: https://keycloak.example.com
    realm: your-realm
    client-id: auth-service
    client-secret: ${KEYCLOAK_CLIENT_SECRET}
    admin-client-id: admin-cli
    admin-client-secret: ${KEYCLOAK_ADMIN_SECRET}
    token-exchange-enabled: true
```

### Keycloak Configuration Required

For Token Exchange to work, you need to configure Keycloak:

1. **Enable Token Exchange** in your realm
2. **Create a confidential client** (auth-service) with:
   - `token-exchange` permission
   - `impersonation` permission (if using impersonation)
3. **Configure fine-grained permissions** for which users can be exchanged

---

## Updated Complete Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    OAuth 2.1 Compliant Auth Flow                             │
└─────────────────────────────────────────────────────────────────────────────┘

┌────────┐  1. Login Request           ┌──────────────┐
│ Client │  POST /auth/login           │ Auth Service │
│        │ ────────────────────────────▶│              │
│        │  Host: acme.example.com     │              │
│        │  {email, password}          │              │
└────────┘                             └──────┬───────┘
                                              │
           2. Resolve tenant from host        │
           (TenantResolverService)            │
           tenantId = "acme"                  │
                                              │
           3. Resolve userId from email       ▼
           ┌─────────────────────────────────────────────┐
           │  User Service (mocked for now)             │
           │  email: "john@example.com" → userId: "u123"│
           └─────────────────────────────────────────────┘
                                              │
           4. Validate password &             ▼
              Token Exchange with Keycloak
           ┌─────────────────────────────────────────────┐
           │  Keycloak                                   │
           │  a) Validate password via Admin API        │
           │  b) Token Exchange → user tokens           │
           └─────────────────────────────────────────────┘
                                              │
           5. Create session in Redis         ▼
           ┌─────────────────────────────────────────────┐
           │  Redis                                      │
           │  session:acme:ses_xyz = {                  │
           │    sessionId, userId, tenantId,            │
           │    roles, permissions,                      │
           │    keycloakTokens: {...}                   │
           │  }                                          │
           │  TTL: 24 hours (auto-expire)               │
           └─────────────────────────────────────────────┘
                                              │
┌────────┐  6. Return our minimal JWT         │
│ Client │ ◀──────────────────────────────────┘
│        │  {
│        │    token: "eyJ...",  // {userId, sessionId, tenantId}
│        │    expiresIn: 3600
│        │  }
└────────┘
```

---

Would you like me to switch to Agent mode to create the complete project structure with all these implementations?

---

**User**

Please update the "1. TenantResolverService - Separate Implementations" section, because I want to always store the tenant information in a DB and use dynamic values. So it should not be in the `application.yml`.
The guest/default is the only one between the tenants that does not have a subdomain. Every other tenant should have a subdomain. So finding it is streightforward in this sense.

In the "2. Custom Security Annotations with Spring Security" section:
I feel that you are right. Having only the "Better Approach: Custom Security Expressions with SpEL" is much cleaner. Please keep this one.

In the "3. OAuth 2.1 Compliant Flow with Keycloak" section:
I like the "Option B: Backend-for-Frontend (BFF) Pattern (Better for Your Case)", but with one change.
Isn't it possible to store the password only in Keycloak.
If yes, then the "Step 3: Validate password (Option: Direct User Service check)" is readundant.

---

**Cursor**

Let me provide clean, updated versions of all three sections.

---

## 1. TenantResolverService - Database-Driven Implementation

### Domain Model

```kotlin
data class Tenant(
    val tenantId: String,
    val name: String,
    val subdomain: String?,              // null for default/guest tenant
    val customDomains: Set<String>,      // e.g., {"my-super-domain.com", "another.com"}
    val isDefault: Boolean,              // true for guest tenant
    val isActive: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
)
```

### Repository Interface

```kotlin
interface TenantRepository {
    suspend fun findBySubdomain(subdomain: String): Tenant?
    suspend fun findByCustomDomain(domain: String): Tenant?
    suspend fun findDefault(): Tenant?
    suspend fun findById(tenantId: String): Tenant?
    suspend fun existsBySubdomain(subdomain: String): Boolean
}
```

### Interface

```kotlin
interface TenantResolverService {
    suspend fun resolveTenantId(host: String): String
    suspend fun resolveTenant(host: String): Tenant
}
```

### Production Implementation

```kotlin
@Service
@Profile("!local")
class ProductionTenantResolverService(
    private val tenantRepository: TenantRepository,
    private val dnsResolverService: DnsResolverService,
    private val tenantResolverCache: TenantResolverCache
) : TenantResolverService {
    
    private val logger = LoggerFactory.getLogger(javaClass)
    
    override suspend fun resolveTenantId(host: String): String {
        return resolveTenant(host).tenantId
    }
    
    override suspend fun resolveTenant(host: String): Tenant {
        val hostWithoutPort = host.substringBefore(":")
        
        // 1. Check cache first
        tenantResolverCache.get(hostWithoutPort)?.let { tenant ->
            if (tenant.isActive) {
                return tenant
            }
        }
        
        // 2. Direct custom domain lookup (e.g., "my-super-domain.com")
        tenantRepository.findByCustomDomain(hostWithoutPort)?.let { tenant ->
            if (tenant.isActive) {
                logger.debug("Resolved tenant '${tenant.tenantId}' from custom domain '$hostWithoutPort'")
                tenantResolverCache.put(hostWithoutPort, tenant)
                return tenant
            }
        }
        
        // 3. Extract subdomain and lookup
        val subdomain = extractSubdomain(hostWithoutPort)
        
        if (subdomain != null) {
            // Has subdomain - must find matching tenant
            tenantRepository.findBySubdomain(subdomain)?.let { tenant ->
                if (tenant.isActive) {
                    logger.debug("Resolved tenant '${tenant.tenantId}' from subdomain '$subdomain'")
                    tenantResolverCache.put(hostWithoutPort, tenant)
                    return tenant
                }
            }
        } else {
            // No subdomain - return default tenant
            tenantRepository.findDefault()?.let { tenant ->
                if (tenant.isActive) {
                    logger.debug("Resolved default tenant '${tenant.tenantId}' for '$hostWithoutPort'")
                    return tenant
                }
            }
        }
        
        // 4. Try CNAME resolution for custom domains
        val tenantViaCname = resolveTenantViaCname(hostWithoutPort)
        if (tenantViaCname != null) {
            logger.debug("Resolved tenant '${tenantViaCname.tenantId}' via CNAME from '$hostWithoutPort'")
            tenantResolverCache.put(hostWithoutPort, tenantViaCname)
            return tenantViaCname
        }
        
        throw TenantNotFoundException("Cannot resolve tenant for host: $hostWithoutPort")
    }
    
    /**
     * Extracts subdomain from host.
     * Returns null if host is a base domain (no subdomain).
     * 
     * Examples:
     *   "acme.example.com" → "acme"
     *   "example.com" → null
     *   "my-custom-domain.com" → null (handled separately as custom domain)
     */
    private suspend fun extractSubdomain(host: String): String? {
        // Get all registered base domains from DB
        // This could be cached with a reasonable TTL
        val baseDomains = tenantResolverCache.getBaseDomains()
        
        for (baseDomain in baseDomains) {
            when {
                // Exact match with base domain = no subdomain = default tenant
                host == baseDomain -> return null
                
                // Has subdomain
                host.endsWith(".$baseDomain") -> {
                    val subdomain = host.removeSuffix(".$baseDomain")
                    // Ensure single-level subdomain (no dots)
                    if (!subdomain.contains(".")) {
                        return subdomain
                    }
                }
            }
        }
        
        // Not matching any base domain - could be a custom domain
        return null
    }
    
    /**
     * Resolves tenant by following CNAME chain.
     * 
     * Example:
     *   Customer sets: my-super-domain.com CNAME acme.example.com
     *   Resolution: my-super-domain.com → acme.example.com → tenant "acme"
     */
    private suspend fun resolveTenantViaCname(host: String): Tenant? {
        val cnameChain = dnsResolverService.resolveCnameChain(host)
        
        for (cname in cnameChain) {
            // Check if CNAME points to one of our subdomains
            val subdomain = extractSubdomain(cname)
            if (subdomain != null) {
                tenantRepository.findBySubdomain(subdomain)?.let { tenant ->
                    if (tenant.isActive) {
                        // Optionally: auto-register this custom domain for the tenant
                        // to avoid repeated DNS lookups
                        return tenant
                    }
                }
            }
            
            // Check if CNAME points to a registered custom domain
            tenantRepository.findByCustomDomain(cname)?.let { tenant ->
                if (tenant.isActive) {
                    return tenant
                }
            }
        }
        
        return null
    }
}
```

### Local Development Implementation

```kotlin
@Service
@Profile("local")
class LocalTenantResolverService(
    private val tenantRepository: TenantRepository
) : TenantResolverService {
    
    private val logger = LoggerFactory.getLogger(javaClass)
    
    // Local domains that are treated like base domains
    private val localBaseDomains = setOf("localhost", "127.0.0.1", "local.test", "lvh.me")
    
    override suspend fun resolveTenantId(host: String): String {
        return resolveTenant(host).tenantId
    }
    
    override suspend fun resolveTenant(host: String): Tenant {
        val hostWithoutPort = host.substringBefore(":")
        
        // 1. Extract subdomain from localhost variants
        val subdomain = extractLocalSubdomain(hostWithoutPort)
        
        if (subdomain != null) {
            // Has subdomain - lookup in DB
            tenantRepository.findBySubdomain(subdomain)?.let { tenant ->
                if (tenant.isActive) {
                    logger.debug("Resolved tenant '${tenant.tenantId}' from local subdomain '$subdomain'")
                    return tenant
                }
            }
            
            // Subdomain not found in DB
            throw TenantNotFoundException("Unknown tenant subdomain: $subdomain")
        }
        
        // No subdomain - return default tenant
        val defaultTenant = tenantRepository.findDefault()
            ?: throw TenantNotFoundException("No default tenant configured")
        
        logger.debug("Resolved default tenant '${defaultTenant.tenantId}' for '$hostWithoutPort'")
        return defaultTenant
    }
    
    /**
     * Extracts subdomain from localhost variants.
     * 
     * Examples:
     *   "localhost" → null (default tenant)
     *   "localhost:8080" → null (default tenant)
     *   "acme.localhost" → "acme"
     *   "acme.localhost:8080" → "acme"
     *   "test1.lvh.me" → "test1"
     *   "127.0.0.1" → null (default tenant)
     */
    private fun extractLocalSubdomain(host: String): String? {
        for (localDomain in localBaseDomains) {
            when {
                host == localDomain -> return null
                host.endsWith(".$localDomain") -> {
                    val subdomain = host.removeSuffix(".$localDomain")
                    if (!subdomain.contains(".")) {
                        return subdomain
                    }
                }
            }
        }
        return null
    }
}
```

### DNS Resolver Service

```kotlin
interface DnsResolverService {
    suspend fun resolveCnameChain(host: String): List<String>
}

@Service
@Profile("!local")
class ProductionDnsResolverService : DnsResolverService {
    
    private val logger = LoggerFactory.getLogger(javaClass)
    
    override suspend fun resolveCnameChain(host: String): List<String> = withContext(Dispatchers.IO) {
        val chain = mutableListOf<String>()
        var currentHost = host
        val visited = mutableSetOf<String>()
        
        repeat(10) {  // Max 10 hops to prevent infinite loops
            if (currentHost in visited) {
                logger.warn("CNAME loop detected for $host")
                return@withContext chain
            }
            visited.add(currentHost)
            
            try {
                val env = Hashtable<String, String>().apply {
                    put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory")
                }
                val dirContext = InitialDirContext(env)
                val attributes = dirContext.getAttributes(currentHost, arrayOf("CNAME"))
                val cnameAttr = attributes.get("CNAME")
                
                if (cnameAttr != null) {
                    val cname = cnameAttr.get().toString().removeSuffix(".")
                    chain.add(cname)
                    currentHost = cname
                } else {
                    return@withContext chain
                }
            } catch (e: NamingException) {
                return@withContext chain
            }
        }
        
        chain
    }
}

@Service
@Profile("local")
class LocalDnsResolverService : DnsResolverService {
    // No DNS resolution needed for local development
    override suspend fun resolveCnameChain(host: String): List<String> = emptyList()
}
```

### Cache Service

```kotlin
interface TenantResolverCache {
    suspend fun get(host: String): Tenant?
    suspend fun put(host: String, tenant: Tenant)
    suspend fun invalidate(host: String)
    suspend fun getBaseDomains(): Set<String>
}

@Service
class RedisTenantResolverCache(
    private val redisTemplate: ReactiveStringRedisTemplate,
    private val objectMapper: ObjectMapper,
    private val baseDomainRepository: BaseDomainRepository
) : TenantResolverCache {
    
    private val cacheTtl = Duration.ofMinutes(15)
    
    override suspend fun get(host: String): Tenant? {
        val key = "tenant_cache:$host"
        val value = redisTemplate.opsForValue().getAndAwait(key) ?: return null
        return objectMapper.readValue(value, Tenant::class.java)
    }
    
    override suspend fun put(host: String, tenant: Tenant) {
        val key = "tenant_cache:$host"
        val value = objectMapper.writeValueAsString(tenant)
        redisTemplate.opsForValue().setAndAwait(key, value, cacheTtl)
    }
    
    override suspend fun invalidate(host: String) {
        redisTemplate.deleteAndAwait("tenant_cache:$host")
    }
    
    override suspend fun getBaseDomains(): Set<String> {
        // Cache base domains with longer TTL since they rarely change
        val cacheKey = "base_domains"
        val cached = redisTemplate.opsForSet().membersAndAwait(cacheKey)
        
        if (cached.isNotEmpty()) {
            return cached.toSet()
        }
        
        // Load from DB and cache
        val domains = baseDomainRepository.findAll().map { it.domain }.toSet()
        if (domains.isNotEmpty()) {
            redisTemplate.opsForSet().addAndAwait(cacheKey, *domains.toTypedArray())
            redisTemplate.expireAndAwait(cacheKey, Duration.ofHours(1))
        }
        
        return domains
    }
}
```

### Database Schema (for reference)

```sql
-- Tenants table
CREATE TABLE tenants (
    tenant_id       VARCHAR(50) PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    subdomain       VARCHAR(63) UNIQUE,          -- null for default tenant
    is_default      BOOLEAN DEFAULT FALSE,       -- only one can be true
    is_active       BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP NOT NULL,
    
    CONSTRAINT only_one_default CHECK (
        NOT is_default OR subdomain IS NULL
    )
);

-- Custom domains table (one-to-many with tenants)
CREATE TABLE tenant_custom_domains (
    domain          VARCHAR(255) PRIMARY KEY,
    tenant_id       VARCHAR(50) NOT NULL REFERENCES tenants(tenant_id),
    is_verified     BOOLEAN DEFAULT FALSE,
    created_at      TIMESTAMP NOT NULL,
    
    INDEX idx_tenant_id (tenant_id)
);

-- Base domains table (domains your service runs on)
CREATE TABLE base_domains (
    domain          VARCHAR(255) PRIMARY KEY,
    is_primary      BOOLEAN DEFAULT FALSE,
    created_at      TIMESTAMP NOT NULL
);

-- Example data
INSERT INTO base_domains (domain, is_primary, created_at) VALUES
    ('example.com', true, NOW()),
    ('example.io', false, NOW());

INSERT INTO tenants (tenant_id, name, subdomain, is_default, is_active, created_at, updated_at) VALUES
    ('default', 'Guest', NULL, true, true, NOW(), NOW()),
    ('acme', 'Acme Corp', 'acme', false, true, NOW(), NOW()),
    ('globex', 'Globex Inc', 'globex', false, true, NOW(), NOW());

INSERT INTO tenant_custom_domains (domain, tenant_id, is_verified, created_at) VALUES
    ('my-super-domain.com', 'acme', true, NOW()),
    ('acme-internal.net', 'acme', true, NOW());
```

### Resolution Examples

| Host | Resolved Tenant | How |
|------|-----------------|-----|
| `example.com` | `default` | No subdomain → default |
| `acme.example.com` | `acme` | Subdomain lookup |
| `globex.example.io` | `globex` | Subdomain lookup |
| `my-super-domain.com` | `acme` | Custom domain lookup |
| `unknown.example.com` | Error | Subdomain not found |
| `random-site.com` (CNAMEs to `acme.example.com`) | `acme` | CNAME resolution |
| **Local:** `localhost` | `default` | No subdomain → default |
| **Local:** `acme.localhost` | `acme` | Subdomain lookup |
| **Local:** `test.lvh.me` | `test` | Subdomain lookup |

---

## 2. Custom Security Annotations with Spring Security (SpEL Only)

### Security Expression Bean

```kotlin
/**
 * Custom security expressions available in @PreAuthorize annotations.
 * 
 * Usage:
 *   @PreAuthorize("@auth.hasRole('ADMIN')")
 *   @PreAuthorize("@auth.hasAnyRole('ADMIN', 'MODERATOR')")
 *   @PreAuthorize("@auth.hasPermission('users:delete')")
 *   @PreAuthorize("@auth.hasAnyPermission('users:read', 'users:read:admin')")
 */
@Component("auth")
class AuthSecurityExpressions {
    
    /**
     * Checks if user has the specified role.
     */
    fun hasRole(role: String): Mono<Boolean> {
        return getAuthentication().map { auth ->
            auth.authorities.any { it.authority == "ROLE_$role" }
        }
    }
    
    /**
     * Checks if user has at least one of the specified roles.
     */
    fun hasAnyRole(vararg roles: String): Mono<Boolean> {
        return getAuthentication().map { auth ->
            val requiredRoles = roles.map { "ROLE_$it" }.toSet()
            auth.authorities.any { it.authority in requiredRoles }
        }
    }
    
    /**
     * Checks if user has all of the specified roles.
     */
    fun hasAllRoles(vararg roles: String): Mono<Boolean> {
        return getAuthentication().map { auth ->
            val requiredRoles = roles.map { "ROLE_$it" }.toSet()
            val userRoles = auth.authorities.map { it.authority }.toSet()
            userRoles.containsAll(requiredRoles)
        }
    }
    
    /**
     * Checks if user has the specified permission.
     */
    fun hasPermission(permission: String): Mono<Boolean> {
        return getAuthentication().map { auth ->
            auth.authorities.any { it.authority == permission }
        }
    }
    
    /**
     * Checks if user has at least one of the specified permissions.
     */
    fun hasAnyPermission(vararg permissions: String): Mono<Boolean> {
        return getAuthentication().map { auth ->
            val requiredPermissions = permissions.toSet()
            auth.authorities.any { it.authority in requiredPermissions }
        }
    }
    
    /**
     * Checks if user has all of the specified permissions.
     */
    fun hasAllPermissions(vararg permissions: String): Mono<Boolean> {
        return getAuthentication().map { auth ->
            val requiredPermissions = permissions.toSet()
            val userPermissions = auth.authorities.map { it.authority }.toSet()
            userPermissions.containsAll(requiredPermissions)
        }
    }
    
    /**
     * Checks if user belongs to the specified tenant.
     */
    fun isTenant(tenantId: String): Mono<Boolean> {
        return getSession().map { session ->
            session.tenantId == tenantId
        }
    }
    
    /**
     * Checks if user is the owner of a resource.
     */
    fun isOwner(resourceOwnerId: String): Mono<Boolean> {
        return getSession().map { session ->
            session.userId == resourceOwnerId
        }
    }
    
    /**
     * Combined check: has role OR is owner.
     */
    fun hasRoleOrIsOwner(role: String, resourceOwnerId: String): Mono<Boolean> {
        return getAuthentication().zipWith(getSession()).map { (auth, session) ->
            auth.authorities.any { it.authority == "ROLE_$role" } || session.userId == resourceOwnerId
        }
    }
    
    private fun getAuthentication(): Mono<Authentication> {
        return ReactiveSecurityContextHolder.getContext()
            .map { it.authentication }
    }
    
    private fun getSession(): Mono<SessionData> {
        return getAuthentication().map { auth ->
            auth.principal as SessionData
        }
    }
}
```

### Custom Convenience Annotations

```kotlin
// === Role Annotations ===

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@PreAuthorize("@auth.hasRole('ADMIN')")
annotation class RequireAdmin

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@PreAuthorize("@auth.hasRole('USER')")
annotation class RequireUser

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@PreAuthorize("@auth.hasAnyRole('ADMIN', 'MODERATOR')")
annotation class RequireAdminOrModerator

// === Permission Annotations ===

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@PreAuthorize("@auth.hasPermission('users:read')")
annotation class CanReadUsers

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@PreAuthorize("@auth.hasPermission('users:write')")
annotation class CanWriteUsers

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@PreAuthorize("@auth.hasPermission('users:delete')")
annotation class CanDeleteUsers
```

### Security Configuration

```kotlin
@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
class SecurityConfig {
    
    @Bean
    fun securityWebFilterChain(
        http: ServerHttpSecurity,
        authenticationConverter: SessionAuthenticationConverter
    ): SecurityWebFilterChain {
        return http
            .csrf { it.disable() }
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            .authorizeExchange { exchanges ->
                exchanges
                    .pathMatchers("/auth/login", "/auth/register", "/auth/refresh").permitAll()
                    .pathMatchers("/public/**").permitAll()
                    .pathMatchers("/actuator/health").permitAll()
                    .anyExchange().authenticated()
            }
            .addFilterBefore(
                JwtAuthenticationWebFilter(authenticationConverter),
                SecurityWebFiltersOrder.AUTHENTICATION
            )
            .build()
    }
}
```

### Controller Usage Examples

```kotlin
@RestController
@RequestMapping("/api/v1/users")
class UserController(
    private val userService: UserService
) {
    
    // Using custom convenience annotations
    @GetMapping("/admin-panel")
    @RequireAdmin
    suspend fun getAdminPanel(session: SessionData): AdminPanelResponse {
        return userService.getAdminPanel(session.tenantId)
    }
    
    // Using SpEL directly for single role
    @GetMapping("/dashboard")
    @PreAuthorize("@auth.hasRole('USER')")
    suspend fun getDashboard(session: SessionData): DashboardResponse {
        return userService.getDashboard(session)
    }
    
    // Using SpEL for multiple roles (any)
    @PutMapping("/{userId}")
    @PreAuthorize("@auth.hasAnyRole('ADMIN', 'MODERATOR')")
    suspend fun updateUser(
        @PathVariable userId: String,
        @RequestBody request: UpdateUserRequest,
        session: SessionData
    ): UserResponse {
        return userService.update(userId, request, session.tenantId)
    }
    
    // Using SpEL for permissions
    @DeleteMapping("/{userId}")
    @PreAuthorize("@auth.hasPermission('users:delete')")
    suspend fun deleteUser(
        @PathVariable userId: String,
        session: SessionData
    ): ResponseEntity<Unit> {
        userService.delete(userId, session.tenantId)
        return ResponseEntity.noContent().build()
    }
    
    // Using SpEL for multiple permissions (any)
    @GetMapping("/{userId}/sensitive")
    @PreAuthorize("@auth.hasAnyPermission('users:read:sensitive', 'users:read:admin')")
    suspend fun getSensitiveData(
        @PathVariable userId: String,
        session: SessionData
    ): SensitiveUserData {
        return userService.getSensitiveData(userId, session.tenantId)
    }
    
    // Combined: role AND permission
    @PostMapping("/{userId}/promote")
    @PreAuthorize("@auth.hasRole('ADMIN') and @auth.hasPermission('users:promote')")
    suspend fun promoteUser(
        @PathVariable userId: String,
        session: SessionData
    ): UserResponse {
        return userService.promote(userId, session.tenantId)
    }
    
    // Owner check: user can read their own data OR admin can read anyone's
    @GetMapping("/{userId}/profile")
    @PreAuthorize("@auth.hasRoleOrIsOwner('ADMIN', #userId)")
    suspend fun getUserProfile(
        @PathVariable userId: String,
        session: SessionData
    ): UserProfile {
        return userService.getProfile(userId, session.tenantId)
    }
    
    // All roles required
    @PostMapping("/super-admin-action")
    @PreAuthorize("@auth.hasAllRoles('ADMIN', 'VERIFIED', 'MFA_ENABLED')")
    suspend fun superAdminAction(session: SessionData): ActionResponse {
        return userService.superAdminAction(session)
    }
}
```

---

## 3. OAuth 2.1 Flow with Keycloak (Passwords in Keycloak Only)

### Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                Password stored ONLY in Keycloak                              │
│                                                                              │
│  User Service: Stores user profile, email, tenantIds                        │
│  Keycloak:     Stores credentials, handles authentication                    │
│  Auth Service: Orchestrates the flow, issues application JWT                │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Flow Diagram

```
┌────────┐  1. Login Request           ┌──────────────┐
│ Client │  POST /auth/login           │ Auth Service │
│        │ ────────────────────────────▶│              │
│        │  Host: acme.example.com     │              │
│        │  {email, password}          │              │
└────────┘                             └──────┬───────┘
                                              │
           2. Resolve tenant from host        │
           tenantId = "acme"                  │
                                              │
           3. Resolve userId from email       ▼
           ┌─────────────────────────────────────────────┐
           │  User Service (mocked)                     │
           │  GET /users?email=john@example.com         │
           │  Response: {userId: "usr_abc123"}          │
           └─────────────────────────────────────────────┘
                                              │
           4. Authenticate with Keycloak      ▼
              (password validation happens HERE)
           ┌─────────────────────────────────────────────┐
           │  Keycloak                                   │
           │                                             │
           │  Direct Access Grant (if enabled) OR       │
           │  Admin API credential validation           │
           │                                             │
           │  → Validates password                       │
           │  → Returns Keycloak tokens                  │
           └─────────────────────────────────────────────┘
                                              │
           5. Create session in Redis         ▼
           ┌─────────────────────────────────────────────┐
           │  Redis                                      │
           │  session:acme:ses_xyz = SessionData        │
           │  (includes Keycloak tokens for later use)  │
           └─────────────────────────────────────────────┘
                                              │
┌────────┐  6. Return our minimal JWT         │
│ Client │ ◀──────────────────────────────────┘
│        │  {token, expiresIn}
└────────┘
```

### Authentication Service

```kotlin
@Service
class AuthenticationService(
    private val userServiceClient: UserServiceClient,
    private val keycloakClient: KeycloakClient,
    private val sessionRepository: SessionRepository,
    private val jwtService: JwtService,
    private val sessionProperties: SessionProperties
) {
    
    private val logger = LoggerFactory.getLogger(javaClass)
    
    /**
     * Authenticates a user and creates a session.
     * 
     * Flow:
     * 1. Resolve userId from email via User Service
     * 2. Authenticate with Keycloak (password validation)
     * 3. Get user roles/permissions from User Service
     * 4. Create session in Redis
     * 5. Issue application JWT
     */
    suspend fun login(
        email: String,
        password: String,
        tenantId: String
    ): LoginResponse {
        
        // Step 1: Resolve userId from email
        val userInfo = userServiceClient.resolveUser(email, tenantId)
            ?: throw AuthenticationException("User not found")
        
        // Verify user has access to this tenant
        if (tenantId !in userInfo.tenantIds) {
            throw AuthenticationException("User does not have access to this tenant")
        }
        
        // Step 2: Authenticate with Keycloak
        // Keycloak stores and validates the password
        val keycloakTokens = keycloakClient.authenticate(
            username = userInfo.userId,  // Or use email if that's how Keycloak is configured
            password = password
        ) ?: throw AuthenticationException("Invalid credentials")
        
        // Step 3: Get user roles/permissions from User Service
        val userDetails = userServiceClient.getUserDetails(userInfo.userId, tenantId)
        
        // Step 4: Create session
        val sessionId = generateSessionId()
        val now = Instant.now()
        
        val session = SessionData(
            sessionId = sessionId,
            userId = userInfo.userId,
            tenantId = tenantId,
            username = userDetails.username,
            email = userDetails.email,
            roles = userDetails.roles,
            permissions = userDetails.permissions,
            scopes = extractScopes(keycloakTokens),
            keycloakAccessToken = keycloakTokens.accessToken,
            keycloakRefreshToken = keycloakTokens.refreshToken,
            createdAt = now,
            lastAccessedAt = now,
            expiresAt = now.plus(sessionProperties.sessionTimeoutMinutes, ChronoUnit.MINUTES)
        )
        
        sessionRepository.save(session)
        
        // Step 5: Issue our JWT
        val token = jwtService.generateToken(
            userId = userInfo.userId,
            sessionId = sessionId,
            tenantId = tenantId
        )
        
        logger.info("User '${userInfo.userId}' logged in to tenant '$tenantId'")
        
        return LoginResponse(
            token = token,
            expiresIn = sessionProperties.tokenExpirationMinutes * 60
        )
    }
    
    private fun generateSessionId(): String {
        return "ses_${UUID.randomUUID().toString().replace("-", "")}"
    }
    
    private fun extractScopes(tokens: KeycloakTokens): Set<String> {
        // Extract scopes from Keycloak access token if needed
        return emptySet()
    }
}
```

### Keycloak Client

```kotlin
interface KeycloakClient {
    /**
     * Authenticates user with username/password.
     * Returns Keycloak tokens if successful, null if authentication fails.
     */
    suspend fun authenticate(username: String, password: String): KeycloakTokens?
    
    /**
     * Refreshes Keycloak tokens using refresh token.
     */
    suspend fun refreshTokens(refreshToken: String): KeycloakTokens?
    
    /**
     * Introspects a token to check validity.
     */
    suspend fun introspect(token: String): TokenIntrospectionResponse
    
    /**
     * Revokes a token (logout from Keycloak).
     */
    suspend fun revokeToken(token: String)
}

data class KeycloakTokens(
    val accessToken: String,
    val refreshToken: String?,
    val idToken: String?,
    val expiresIn: Long,
    val tokenType: String
)

@Service
class KeycloakClientImpl(
    private val httpClient: HttpClient,
    private val keycloakProperties: KeycloakProperties
) : KeycloakClient {
    
    private val logger = LoggerFactory.getLogger(javaClass)
    
    override suspend fun authenticate(username: String, password: String): KeycloakTokens? {
        return try {
            val response: HttpResponse = httpClient.submitForm(
                url = "${keycloakProperties.serverUrl}/realms/${keycloakProperties.realm}/protocol/openid-connect/token",
                formParameters = parameters {
                    append("grant_type", "password")
                    append("client_id", keycloakProperties.clientId)
                    append("client_secret", keycloakProperties.clientSecret)
                    append("username", username)
                    append("password", password)
                    append("scope", "openid profile email")
                }
            )
            
            if (response.status == HttpStatusCode.OK) {
                response.body<KeycloakTokenResponse>().toKeycloakTokens()
            } else {
                logger.debug("Keycloak authentication failed with status: ${response.status}")
                null
            }
        } catch (e: ClientRequestException) {
            logger.debug("Keycloak authentication failed: ${e.message}")
            null
        }
    }
    
    override suspend fun refreshTokens(refreshToken: String): KeycloakTokens? {
        return try {
            val response: HttpResponse = httpClient.submitForm(
                url = "${keycloakProperties.serverUrl}/realms/${keycloakProperties.realm}/protocol/openid-connect/token",
                formParameters = parameters {
                    append("grant_type", "refresh_token")
                    append("client_id", keycloakProperties.clientId)
                    append("client_secret", keycloakProperties.clientSecret)
                    append("refresh_token", refreshToken)
                }
            )
            
            if (response.status == HttpStatusCode.OK) {
                response.body<KeycloakTokenResponse>().toKeycloakTokens()
            } else {
                null
            }
        } catch (e: Exception) {
            logger.warn("Failed to refresh Keycloak tokens: ${e.message}")
            null
        }
    }
    
    override suspend fun introspect(token: String): TokenIntrospectionResponse {
        val response: HttpResponse = httpClient.submitForm(
            url = "${keycloakProperties.serverUrl}/realms/${keycloakProperties.realm}/protocol/openid-connect/token/introspect",
            formParameters = parameters {
                append("client_id", keycloakProperties.clientId)
                append("client_secret", keycloakProperties.clientSecret)
                append("token", token)
            }
        )
        return response.body()
    }
    
    override suspend fun revokeToken(token: String) {
        httpClient.submitForm(
            url = "${keycloakProperties.serverUrl}/realms/${keycloakProperties.realm}/protocol/openid-connect/revoke",
            formParameters = parameters {
                append("client_id", keycloakProperties.clientId)
                append("client_secret", keycloakProperties.clientSecret)
                append("token", token)
            }
        )
    }
}

@Serializable
private data class KeycloakTokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("id_token") val idToken: String? = null,
    @SerialName("expires_in") val expiresIn: Long,
    @SerialName("token_type") val tokenType: String
) {
    fun toKeycloakTokens() = KeycloakTokens(
        accessToken = accessToken,
        refreshToken = refreshToken,
        idToken = idToken,
        expiresIn = expiresIn,
        tokenType = tokenType
    )
}
```

### User Service Client (Mocked for Now)

```kotlin
interface UserServiceClient {
    suspend fun resolveUser(email: String, tenantId: String): UserResolveResponse?
    suspend fun getUserDetails(userId: String, tenantId: String): UserDetailsResponse
}

data class UserResolveResponse(
    val userId: String,
    val email: String,
    val tenantIds: Set<String>
)

data class UserDetailsResponse(
    val userId: String,
    val username: String,
    val email: String,
    val roles: Set<String>,
    val permissions: Set<String>
)

/**
 * Mocked implementation for development.
 * Replace with actual Ktor HTTP client implementation when user-service is ready.
 */
@Service
@Profile("local", "dev")
class MockedUserServiceClient : UserServiceClient {
    
    private val users = mapOf(
        "john@example.com" to UserResolveResponse(
            userId = "usr_john_001",
            email = "john@example.com",
            tenantIds = setOf("default", "acme", "globex")
        ),
        "jane@example.com" to UserResolveResponse(
            userId = "usr_jane_002",
            email = "jane@example.com",
            tenantIds = setOf("default", "acme")
        ),
        "admin@example.com" to UserResolveResponse(
            userId = "usr_admin_003",
            email = "admin@example.com",
            tenantIds = setOf("default", "acme", "globex")
        )
    )
    
    private val userDetails = mapOf(
        "usr_john_001" to UserDetailsResponse(
            userId = "usr_john_001",
            username = "john.doe",
            email = "john@example.com",
            roles = setOf("USER"),
            permissions = setOf("users:read", "profile:read", "profile:write")
        ),
        "usr_jane_002" to UserDetailsResponse(
            userId = "usr_jane_002",
            username = "jane.smith",
            email = "jane@example.com",
            roles = setOf("USER", "MODERATOR"),
            permissions = setOf("users:read", "users:write", "profile:read", "profile:write")
        ),
        "usr_admin_003" to UserDetailsResponse(
            userId = "usr_admin_003",
            username = "admin",
            email = "admin@example.com",
            roles = setOf("USER", "ADMIN"),
            permissions = setOf("users:read", "users:write", "users:delete", "admin:access")
        )
    )
    
    override suspend fun resolveUser(email: String, tenantId: String): UserResolveResponse? {
        return users[email.lowercase()]
    }
    
    override suspend fun getUserDetails(userId: String, tenantId: String): UserDetailsResponse {
        return userDetails[userId] 
            ?: throw UserNotFoundException("User not found: $userId")
    }
}
```

### Configuration

```kotlin
@ConfigurationProperties(prefix = "auth.keycloak")
data class KeycloakProperties(
    val serverUrl: String,
    val realm: String,
    val clientId: String,
    val clientSecret: String
)
```

**application.yml:**
```yaml
auth:
  keycloak:
    server-url: ${KEYCLOAK_URL:http://localhost:8180}
    realm: ${KEYCLOAK_REALM:auth-service}
    client-id: ${KEYCLOAK_CLIENT_ID:auth-service-client}
    client-secret: ${KEYCLOAK_CLIENT_SECRET:secret}
  
  session:
    token-expiration-minutes: 60
    refresh-window-minutes: 5
    session-timeout-minutes: 1440
    sliding-session-enabled: true
```

### Important: Keycloak Configuration

For this flow to work, Keycloak needs:

1. **Realm** created (e.g., `auth-service`)
2. **Confidential Client** with:
   - Client ID: `auth-service-client`
   - Client authentication: ON
   - Direct Access Grants: ENABLED (for password grant)
3. **Users** created in Keycloak with:
   - Username matching your `userId`
   - Password set

**Note on OAuth 2.1 Compliance:**
The "Direct Access Grants" (Resource Owner Password Credentials) is technically deprecated in OAuth 2.1. However:
- It's still supported by Keycloak
- It's acceptable when the auth-service is a trusted first-party service
- For stricter OAuth 2.1 compliance, use Authorization Code Flow with PKCE

---

## Final Summary

| Component | Design Decision |
|-----------|-----------------|
| **Tenant Resolution** | Database-driven, no hardcoded values. Default tenant has no subdomain. CNAME resolution for custom domains. |
| **Security Annotations** | SpEL expressions via `@auth` bean. Clean `@PreAuthorize("@auth.hasRole('ADMIN')")` syntax. |
| **Password Storage** | Keycloak only. User Service has no password. |
| **Authentication Flow** | Email → User Service → userId → Keycloak (validates password) → Session → JWT |

Would you like me to switch to Agent mode to create the complete project structure?

