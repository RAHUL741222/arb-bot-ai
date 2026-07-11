## 2025-05-15 - [Android Keystore in Robolectric Tests]
**Learning:** Accessing `AndroidKeyStore` via custom `Application` classes causes `KeyStoreException` in Robolectric tests.
**Action:** Use `@Config(application = android.app.Application::class)` to bypass custom application initialization in test classes that don't require the custom logic.

## 2025-05-15 - [Unified Loading States]
**Learning:** Moving loading indicators from standalone components to inside the triggering button improves focus and reduces visual clutter.
**Action:** Implement `CircularProgressIndicator` within `Button` scopes and disable the button during async operations.
