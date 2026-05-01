# VibeBoard

Android **keyboard (IME)** and companion app that suggests **GIFs and memes** from **[Giphy](https://developers.giphy.com/)** based on draft text, with debounced requests and local fallback when offline or rate-limited.

## Stack

| Layer | Choice |
|--------|--------|
| Language | Kotlin **2.0** (JVM 11) |
| UI | **Jetpack Compose** + **Material 3** |
| Async | **Kotlin Coroutines**, **StateFlow** |
| Min / target SDK | **24** / **36** |
| Build | **Gradle** with Kotlin DSL, **version catalogs** (`gradle/libs.versions.toml`) |

## Architecture

Clean-ish **layered** structure inside a single app module:

```text
presentation/     # Compose screens, ViewModels, UI state
  home/
  di/             # AppContainer (manual composition root)
domain/           # Use cases, repository interfaces, models, ML hook (query extractor)
data/             # Giphy API, DTOs, MemeRepositoryImpl, LocalMemeCatalog fallback
ime/              # InputMethodService, Compose keyboard UI
```

- **Use cases** (`GetMediaSuggestionsUseCase`, `LoadMemeGalleryPageUseCase`) isolate app logic from data sources.
- **`MemeRepository`** abstracts **Giphy** vs **bundled catalog**; implements debouncing-friendly caching, **429 backoff**, and error fallback.
- **`MemeSearchQueryExtractor`** (`HeuristicMemeSearchQueryExtractor` today) is the seam for a future **on-device ML** intent → search query mapping.
- **IME** uses the same repository/use cases but cannot use system `Dialog`; meme picker is an **in-compose overlay** (`MemeGalleryOverlay`).

## Libraries (key dependencies)

Defined in `gradle/libs.versions.toml` and wired in `app/build.gradle.kts`:

| Library | Role |
|---------|------|
| **AndroidX Core KTX** | Kotlin extensions |
| **Lifecycle** (runtime, ViewModel, compose) | `ViewModel`, `StateFlow`, lifecycle-aware collection |
| **Activity Compose** | `ComponentActivity` + Compose entry |
| **Compose BOM** | Aligned Compose + Material 3 versions |
| **Material 3** | Theming, components |
| **Kotlin Coroutines** | `viewModelScope`, `Flow`, debounce |
| **Coil** (`coil-compose`, `coil-gif`) | Image/GIF loading; global `ImageLoader` in `Application` |
| **Retrofit 2** | Giphy REST API |
| **OkHttp 3** | HTTP client for Retrofit |
| **Kotlinx Serialization (JSON)** | Giphy response parsing |
| **Jake Wharton** `retrofit2-kotlinx-serialization-converter` | Retrofit ↔ kotlinx-serialization |

## Configuration & secrets

1. Clone the repo.
2. Copy `local.properties.example` to **`local.properties`** (or let Android Studio create `local.properties` with `sdk.dir`).
3. Add **`GIPHY_API_KEY`** to `local.properties` (see [Giphy Developers](https://developers.giphy.com/dashboard/)).  
   - **`local.properties` is gitignored** — never commit it.  
   - The key is injected at build time into **`BuildConfig.GIPHY_API_KEY`**.
4. Sync Gradle and build.

If `GIPHY_API_KEY` is empty, the app still runs using the **local bundled catalog** only.

## Giphy attribution

When showing remote Giphy assets, the UI may display **“Powered by Giphy”** as required by [Giphy’s brand guidelines](https://developers.giphy.com/docs/sdk/#design-guidelines) for API use.

## License

Add your license here if applicable.
