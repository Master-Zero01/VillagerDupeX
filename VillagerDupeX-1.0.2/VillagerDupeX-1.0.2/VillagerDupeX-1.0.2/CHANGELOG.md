# Changelog

## 1.0.2
- Changed repeating behavior to scan for all eligible nearby villagers rather than only the crosshair target.
- Added the emerald-mainhand and shears-in-hotbar preflight requirements.
- Switched to shears for each batch and restored/synchronized the emerald slot in a `finally` block.
- Restricted horse-armor filtering to leather horse armor.
- Added nearest-first target ordering, per-target revalidation, and clearer stop conditions.
- Added per-session counters and updated controls/documentation.
