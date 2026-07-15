## 2025-05-15 - [Inline Loading Buttons]
**Learning:** For a consistent and polished look in this app's design system, inline loading indicators in buttons should use a `CircularProgressIndicator` with `Modifier.size(20.dp)` and `strokeWidth = 2.dp`. This prevents the button from resizing and maintains visual balance.
**Action:** Apply this pattern to all primary action buttons that trigger asynchronous operations (like scanning or executing trades).
