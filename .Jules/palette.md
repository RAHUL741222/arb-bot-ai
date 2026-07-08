## 2025-05-14 - [Accessibility and Interaction Feedback]
**Learning:** Icon-only buttons must always have a `contentDescription` for screen reader accessibility. Adding visual feedback like a loading indicator on async buttons prevents multiple clicks and improves the perceived responsiveness of the app.
**Action:** Always include `contentDescription` for `IconButton` and implement `Loading` states for primary action buttons.
