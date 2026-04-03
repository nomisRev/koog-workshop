# Agent Fantasy Store — CLAUDE.md

## Communication
- Chat as little as possible. Use short single-line sentences and be critical & strict.
- Do not try to unnecessarily please.

## Build & Run
- Always use `./gradlew`
- Run app: `./gradlew :app:run`
- Run tests: `./gradlew :app:test`
- The app is a Kotlin/JVM Desktop app using Compose Multiplatform.
- Requires `OPENAI_API_KEY` environment variable at runtime.

## Project Structure

```
app/src/main/kotlin/org/example/project/
├── main.kt                         # Entry point, wires dependencies, launches windows
├── dependencies.kt                 # Manual DI: creates all services and injects them
├── AppColorScheme.kt               # Material3 color scheme
├── chat/                           # Chat UI + agent (ChatAgent, ChatViewModel, ChatScreen)
├── admin/                          # Admin UI (products, orders, merchants, dashboard)
│   ├── app/AdminRoute.kt           # Top-level admin navigation
│   ├── data/AdminDatabase.kt       # DataSource / DB initialization
│   ├── products/, orders/, merchants/, shared/
├── domain/                         # Business logic, services, repositories
│   ├── admin/                      # Admin-specific services + repos
│   │   ├── dashboard/              # AdminDashboardService, AdminDashboardRepository
│   │   ├── merchants/              # AdminMerchantService
│   │   ├── orders/                 # AdminOrderService, AdminOrderRepository, AdminOrderModels
│   │   └── products/               # (admin product service/repo)
│   ├── catalog/                    # Product, Merchant, CatalogService
│   ├── order/                      # Order, OrderService, OrderRepository, OrderStatus
│   ├── character/                  # Character, CharacterService, CharacterRepository
│   ├── cart/                       # Cart, CartService, CartRepository
│   ├── currency/                   # Currency, CurrencyService
│   ├── review/                     # Review, ReviewService
│   ├── shipping/                   # Shipping, ShippingService
│   ├── wishlist/                   # Wishlist, WishlistService
│   └── shared/                     # Ids.kt (type aliases), Page.kt
├── db/                             # SQLite setup, Exposed table definitions, demo seeder
└── koog/                           # AI agent infrastructure (JdbcChatHistoryProvider, tools)
```

## Libraries & Conventions

### Exposed v1
- Package: `org.jetbrains.exposed.v1.*` (e.g., `org.jetbrains.exposed.v1.core.*`, `org.jetbrains.exposed.v1.jdbc.*`)
- DSL functions (`selectAll`, `update`, `insert`, `deleteWhere`) are in `org.jetbrains.exposed.v1.jdbc`
- Wrap DB calls in `database.suspendTransaction { ... }` (extension from `db/SqliteDatabase.kt`)

### Naming
- Functions returning `null` as an absent-row signal must have `OrNull` suffix (e.g., `loadOrderDetailOrNull`)
- Type-safe ID aliases live in `domain/shared/Ids.kt`

### AI / Koog
- `ChatAgent` uses [koog](https://github.com/JetBrains/koog) with `simpleOpenAIExecutor`
- Chat history persisted to SQLite via `JdbcChatHistoryProvider`
- Tools added to the agent live in `koog/` (e.g., `AskQuestionTool`)

### Compose / ViewModel
- ViewModels use `androidx.lifecycle.viewmodel`; factories created with companion `factory()` functions
- UI state is `StateFlow` collected via `collectAsState()`
- Admin UI components are in `admin/shared/ui/`

## Testing
- Tests live in `app/src/test/kotlin/org/example/project/`
- Use JUnit 5 + coroutine test utilities
