## 2024-05-15 - [Robolectric KeyStore Bypass]
**Learning:** Robolectric tests in this project fail when accessing `AndroidKeyStore` through the custom `FlashArbApp` initialization. Bypassing this by using a plain `android.app.Application` class in tests is a clean way to avoid `KeyStoreException` without modifying production security logic.
**Action:** Use `@Config(application = android.app.Application::class)` for any Robolectric tests that do not strictly require the custom application context.

## 2024-05-15 - [Action Button Loading States]
**Learning:** For primary action buttons like "Start Arbitrage Scan", users benefit significantly from inline loading feedback. Using a `CircularProgressIndicator` with `size(20.dp)` and `strokeWidth(2.dp)` maintains button height and visual consistency.
**Action:** Implement inline loading states for all high-latency blockchain interaction buttons.
