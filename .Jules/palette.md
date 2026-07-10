## 2025-05-15 - Loading State and Accessibility Patterns
**Learning:** In this application, primary action buttons (like scan) lack feedback during async operations, and icon-only buttons often miss ARIA labels. Users benefit from localized feedback (spinners inside buttons) to prevent redundant clicks and maintain context.
**Action:** Always implement a `Loading` state within primary action buttons (disabling them and showing an inline `CircularProgressIndicator`) and ensure all `IconButton` components have descriptive `contentDescription` attributes.
