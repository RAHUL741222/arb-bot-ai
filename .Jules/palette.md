## 2025-02-15 - Inline Loading in Action Buttons
**Learning:** Inline loading indicators must use a constrained size (e.g., `Modifier.size(20.dp)` with `strokeWidth = 2.dp`) to maintain button dimensions and avoid visual layout shifting during async tasks in action buttons.
**Action:** Constrain all inline progress indicator sizes inside action buttons to match the button height seamlessly.
