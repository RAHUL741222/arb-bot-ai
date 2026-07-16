## 2025-07-16 - [Integrated Loading State for Primary Buttons]
**Learning:** Primary action buttons that trigger asynchronous operations (like scanning or execution) should provide immediate contextual feedback and prevent redundant triggers by disabling the button and displaying an inline loading indicator.
**Action:** When implementing loading states for buttons, set `enabled = !isLoading` and integrate a `CircularProgressIndicator` (size 20.dp, stroke 2.dp) inside the `Button` content.

## 2025-07-16 - [Robolectric KeyStore Initialization]
**Learning:** Robolectric tests in this project may fail with `KeyStoreException` if the custom `Application` class (which initializes `AndroidKeyStore`) is used.
**Action:** Use `@Config(application = android.app.Application::class)` in test classes to bypass custom application logic that depends on `AndroidKeyStore`.
