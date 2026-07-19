## 2025-01-24 - Inline Dynamic Button Loading and Accessible Config Icon Controls
**Learning:**
- Async operations, like scan/arbitrage executions, should never have separate external indicators (like a raw `CircularProgressIndicator` below the button) because it leads to redundant button clicks, a disjointed layout shift, and lacks immediate visual context on the clicked item.
- Inline progress indicators within the primary button (paired with disabled state) prevent redundant triggers while keeping visual attention on the primary interaction. They should use a standardized size (`20.dp`) and stroke width (`2.dp`) to look consistent and clean.
- Icon-only buttons must have custom accessible labels (`contentDescription`) in the local language context (e.g., Bengali `"কনফিগারেশন দেখান" / "কনফিগারেশন লুকান"`) rather than empty strings to maintain standard compliance and readability for screen readers.

**Action:** Always inline loading spinners into the buttons triggering the operations, set `enabled = !isLoading`, and ensure icon buttons have localized `contentDescription` tags rather than empty placeholders.
