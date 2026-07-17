# Palette's Journal - Critical Learnings

## 2026-07-17 - Localized Content Descriptions and Inline Async Button Loading State
**Learning:** Icon-only toggles (such as configuration panels) require explicit `contentDescription` attributes in the target interface language (Bengali) to remain fully accessible to screen reader users. Additionally, in transaction-heavy DeFi applications, primary action buttons (like scan) should embed an inline `CircularProgressIndicator` (constrained to `size(20.dp)` and `strokeWidth = 2.dp`) and be temporarily disabled during active background scanning to block redundant asynchronous execution loops.
**Action:** When designing interface controls in multilanguage/localized contexts, ensure screen reader assets match the surrounding UI language and combine state-based button content modifications with immediate interaction lockout (`enabled = !isLoading`).
