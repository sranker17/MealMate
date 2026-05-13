# MealMate — Remediation Plan

This document outlines all gaps between the current implementation and the original
[meal_mate_plan.md](./meal_mate_plan.md). Each item is scoped to be fully separable.

---

## Priority Legend

- 🔴 **Critical** — Data loss or broken UX
- 🟡 **Medium** — Missing feature but app still usable
- 🟢 **Low** — Polish / nice-to-have

---

### 🔴 P1 — Persist Menu Acceptance State

**Problem:** `MenuRepository.acceptMenu()` is a no-op — it returns `true` without writing
anything to the database. If the app process is killed after acceptance, the accepted
state is lost and the menu becomes editable again.

**Changes:**

1. **`MenuEntity.kt`** — Add `isAccepted: Boolean = false` column.
2. **`MealMateDatabase.kt`** — Bump version to 2, add migration.
3. **`MenuDao.kt`** — Add `acceptMenu(menuId: Long)` query that sets `isAccepted = 1`.
4. **`MenuRepository.kt`** — Change `acceptMenu()` to call `menuDao.acceptMenu(menu.id)`.
5. **`PlannerViewModel.kt`** — Read `isAccepted` from the active menu entity instead of
   deriving it from cross-ref count.
6. **`MealListViewModel.kt`** — Expose `isActiveMenuLocked` state so the "add to plan"
   button respects the accepted state.

---

### 🔴 P2 — Cooldown Boundary Fix

**Problem:** The cooldown SQL queries in `MealDao` use `>` (strictly greater than), but
the plan states: *"A meal cooked in menu N will not be recommended again until menu
N + Cooldown is completed"* — this implies `>=`.

**Example:** With cooldown = 3, a meal completed in menu #1 should be blocked until
menu #4 is completed (1 + 3 = 4). With `>`, the meal becomes available after menu #4
completes (i.e. before menu #5), which is one menu too early.

**Changes:**

1. **`MealDao.kt`** — Change both `getRandomMealNotInCooldown` and
   `getRandomMealNotInCooldownByTags` to use `>=` instead of `>`.

```sql
-- Before:
AND (:currentIndex - last_completed_menu_index) > :cooldown
-- After:
AND (:currentIndex - last_completed_menu_index) >= :cooldown
```

2. Update tests in `MealRepositoryTest.kt` / `PlannerViewModelTest.kt` to reflect the
   new boundary.

---

### 🟡 P3 — Filter Bottom Sheet for Planner Screen

**Problem:** The filter icon button in `PlannerScreen` has a `// TODO: filter bottom sheet`
comment and does nothing when tapped. Users cannot filter the recommendation engine
from the planner screen.

**Changes:**

1. **`PlannerScreen.kt`** — Replace the TODO `IconButton` with one that shows a
   `ModalBottomSheet` containing all available tags as `FilterChip`s.
2. Wire the sheet's tag selection to `viewModel.onTagFilterToggled()`.
3. Show currently active filter tags as chips below the header (already partially
   implemented, but only appears when `selectedTagIds` is non-empty).

---

### 🟡 P4 — Tablet-Optimized Planner Layout

**Problem:** The `PlannerScreen` uses a single `Column` layout regardless of window
size. On tablets (≥600dp width), the recommendation card and pinned list should be
side-by-side for better screen utilization.

**Changes:**

1. **`PlannerScreen.kt`** — Accept an optional `windowWidthSizeClass` parameter.
2. When `WindowWidthSizeClass.Expanded`: use a `Row` with recommendation card on the
   left (weight ~0.5) and pinned list on the right (weight ~0.5).
3. Pass `windowWidthSizeClass` from `AppNavGraph.kt` or compute via
   `calculateWindowSizeClass()` inside the screen.

---

### 🟡 P5 — Swipe-to-Unpin Gesture

**Problem:** The plan specifies "swipe to unpin" for pinned meals, but the current
implementation uses an `IconButton` instead.

**Changes:**

1. **`PlannerScreen.kt`** — Wrap `PinnedMealCard` items with
   `SwipeToDismissBox` (Material3) when the menu is not yet accepted.
2. On swipe dismissal, call `viewModel.unpinMeal(mealId)`.
3. Keep the unpin `IconButton` as an alternative action.

---

### 🟡 P6 — Outfit Font Bundling

**Problem:** The `Type.kt` file has a TODO comment: *"Replace with Outfit bundled fonts
once added"* — the app currently uses `FontFamily.Default`.

**Changes:**

1. Download Outfit font TTF files (Regular, Medium, Bold) and place them in
   `app/src/main/res/font/`.
2. **`Type.kt`** — Replace `FontFamily.Default` with `FontFamily(Font(R.font.outfit_regular))`,
   and wire Medium/Bold variants to appropriate `FontWeight` values.
3. Create `font/fonts.xml` if needed for programmatic access.

---

### 🟡 P7 — Add `domain` and `util` Packages

**Problem:** The plan calls for `data`, `ui`, `di`, `domain`, and `util` packages.
The `domain` and `util` packages are missing.

**Changes:**

1. **`domain/`** — Create `com.sranker.mealmate.domain` package. This is optional for
   now since the architecture is already clean with repositories acting as the domain
   layer, but the following would fit here:
   - `CooldownCalculator.kt` — Encapsulates the cooldown check logic.
   - `RecommendationEngine.kt` — Extracts recommendation logic from `PlannerViewModel`.
2. **`util/`** — Create `com.sranker.mealmate.util` package. Move/copy:
   - `DateUtils.kt` — Date formatting helpers (currently inline in `MenuRepository`).
   - `Constants.kt` — App-wide constants.

---

### 🟡 P8 — Menu Rename Feature

**Problem:** The plan says past menus should be "renameable", but no rename UI exists.

**Changes:**

1. **`MenuHistoryDetailScreen.kt`** — Add an "edit title" icon button in the header.
2. Show an `AlertDialog` with a text field pre-filled with the current menu title.
3. **`MenuDao.kt`** — Add `updateMenuTitle(menuId: Long, title: String)` query.
4. **`MenuRepository.kt`** — Add `renameMenu(menuId: Long, newTitle: String)`.
5. **`MenuHistoryDetailViewModel.kt`** — Add a `renameMenu(title: String)` function.

---

### 🟢 P9 — Test Coverage Expansion

**Problem:** Several ViewModels and critical business logic lack dedicated unit tests.

**Missing tests:**
- `MenuHistoryViewModel` — No test file exists.
- `MenuHistoryDetailViewModel` — No test file exists.
- `SettingsViewModel` — No test file exists.
- Cooldown boundary logic — No dedicated test for the `>=` behavior.
- Recommendation engine edge cases — Empty database, all meals in cooldown, all meals
  filtered out.

---

### 🟢 P10 — Accent Color Sync (SettingsScreen)

**Problem:** The `SettingsScreen` shows 4 accent color options (teal, green, pink,
slate) in the color picker, but `Color.kt` defines 7 `AccentColor` variants (Teal,
Green, Pink, Slate, Sky, Rose, Sand). The SettingsViewModel and SettingsRepository
only support 4.

**Changes:**

1. **`SettingsScreen.kt`** — Update `accentOptions` list to include all 7 colors.
2. Ensure the theme composable is called with the correct accent from settings.

---

## Recommended Execution Order

| Step | Task | Priority | Dependencies |
|------|------|----------|--------------|
| 1 | P1 — Persist acceptance state | 🔴 | — |
| 2 | P2 — Cooldown boundary | 🔴 | — |
| 3 | P3 — Filter bottom sheet | 🟡 | — |
| 4 | P4 — Tablet layout | 🟡 | — |
| 5 | P5 — Swipe to unpin | 🟡 | — |
| 6 | P6 — Outfit font | 🟡 | — |
| 7 | P7 — domain/util packages | 🟡 | — |
| 8 | P8 — Menu rename | 🟡 | P1 (needs acceptance first) |
| 9 | P9 — Test coverage | 🟢 | P2 (cooldown tests) |
| 10 | P10 — Accent color sync | 🟢 | — |

Steps 1–2 are critical and should be done first. Steps 3–8 are medium priority and can
be parallelized. Steps 9–10 are low priority polish.

