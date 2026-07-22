# Palette's Journal

## 2025-02-15 - [Interactive Button Loading States]
**Learning:** For primary action buttons triggering async operations (like "start arbitrage scan"), users often experience anxiety when there is no immediate contextual visual feedback, leading them to click the button multiple times. Integrating an inline `CircularProgressIndicator` with `size(20.dp)` and `strokeWidth = 2.dp`, changing the button text to "স্ক্যান করা হচ্ছে...", and disabling the button prevents duplicate submissions and provides clear, immediate feedback.
**Action:** Always set `enabled = !isLoading` and embed a properly sized `CircularProgressIndicator` directly inside major action buttons during async states.
