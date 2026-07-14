## 2026-07-14 - [Bangla UI & Design Tokens]
**Learning:** The application extensively uses Bengali text for UI labels and action buttons (e.g., 'আর্বিট্রেজ স্ক্যান শুরু করুন'). It also defines custom design tokens like `CyberPrimary`, `CyberSecondary`, and `CyberBackground` which should be used to maintain visual consistency.
**Action:** Always ensure new UI elements use the appropriate Bengali translations and adhere to the `Cyber` design token system.

## 2026-07-14 - [Testing KeyStore in Robolectric]
**Learning:** Accessing `AndroidKeyStore` via custom `Application` classes in Robolectric tests causes `KeyStoreException`.
**Action:** Use `@Config(application = android.app.Application::class)` in test classes to bypass custom application logic that depends on `AndroidKeyStore` when it's not the focus of the test.
