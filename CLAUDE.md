# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
# Assemble debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.example.dolarapptest.ExampleUnitTest"

# Run instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Lint
./gradlew lint
```

## Architecture

This is a single-screen Android app (currency exchange rate calculator) following clean architecture with three layers:

**Data layer** (`data/`)
- `api/TickersApi` — Retrofit interface against `https://api.dolarapp.dev/`. Two endpoints: `GET /v1/tickers?currencies=...` (always available) and `GET /v1/tickers-currencies` (not yet live — falls back to `["MXN", "ARS", "BRL", "COP"]` via `recoverCatching` in the repository).
- `model/TickerResponse` — Raw API model. The `book` field uses pattern `usdc_<currency>` (e.g. `usdc_mxn`); `toDomain()` splits it to populate `Ticker.from` / `Ticker.to`.
- `typeadapter/BigDecimalTypeAdapter` — Custom Gson adapter to safely parse the API's string-formatted decimal numbers (e.g. `"18.4105000000"`) as `BigDecimal`.
- DI modules: `ApiModule` (Retrofit + Gson), `RepositoryModule` (binds impl to interface).

**Domain layer** (`domain/`)
- `model/Ticker` — The app's canonical exchange rate: `from`, `to`, `ask`, `bid`, `date`.
- `repository/TickersRepository` — Interface used by use cases; implemented in data layer.
- Use cases: `GetCurrenciesUseCase`, `GetTickersUseCase`, `ConvertCurrencyUseCase`.
- `ConvertCurrencyUseCase` — Pure, synchronous (no dispatcher needed). Converts `amount` using `ask` when selling from-currency, `bid` when buying to-currency.
- `DispatcherModule` — Qualifier annotations (`@IoDispatcher`, `@DefaultDispatcher`, `@MainDispatcher`) for injecting coroutine dispatchers.

**UI layer** (`ui/feature/exchange/`)
- Single feature: `ExchangeScreen` (Jetpack Compose) + `ExchangeViewModel` (Hilt ViewModel).
- State is a single `ExchangeUiState` data class exposed via `StateFlow`. User interactions go through `ExchangeUiIntent` sealed interface.
- One-shot events (e.g. toast on error) go through `ExchangeUiEffect` sealed interface, emitted via a `Channel<ExchangeUiEffect>(BUFFERED)` exposed as `receiveAsFlow()`. Collected in `ExchangeScreen` via `LaunchedEffect`.
- `ExchangeUiState` holds two `ExchangeInputFieldUiState` objects (top/bottom fields). The top field is always USDc (non-selectable); the bottom field shows the selected currency and opens a bottom sheet for currency selection.
- `ImmutableList` from `kotlinx-collections-immutable` is used for `availableCurrencies` to avoid unnecessary Compose recompositions.
- `baseCurrencyField: FieldPosition` in `UiState.Success` tracks which field the user is actively typing in. `FieldPosition` is `TOP` or `BOTTOM`.
- Amount conversion: editing the base field uses `bid` rate (selling base); editing the non-base field uses `ask` rate (buying base).

## Key design decisions

- `BigDecimal` is used throughout for all monetary/rate arithmetic (never `Double` or `Float`).
- The currencies endpoint fallback (`recoverCatching` with hardcoded list) is intentional — the endpoint doesn't exist yet in production.
- Coroutine dispatchers are injected rather than hardcoded so they can be replaced in tests.
- `USDc` is always one side of the exchange; only the other currency is selectable.
- `ExchangeUiEffect.ShowToast` carries `@StringRes Int` (not a `String`) — the ViewModel stays context-free; the screen resolves the string.
- `Channel.BUFFERED` on the effect channel guards against effects emitted before the screen's `LaunchedEffect` starts collecting (e.g. errors during `loadInitialState`).