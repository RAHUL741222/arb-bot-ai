# Palette's UX Journal

## 2025-02-15 - [DeFi Button Loading States and Visual Feedback]
**Learning:** In DeFi applications, action buttons (such as scanning and transaction executions) must provide clear inline visual feedback and prevent redundant triggers. Disabling buttons and rendering an inline `CircularProgressIndicator` (using `size(20.dp)` and `strokeWidth = 2.dp`) keeps the user informed and prevents accidental double-spending of gas or redundant network requests.
**Action:** Always set `enabled = !isLoading` and integrate a correctly sized inline progress indicator inside the action button contents to ensure consistent, safe, and pleasant user interactions.
