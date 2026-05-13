# Bug Fixes Plan (Batch 2)

This document breaks down all required changes into completely separable, LLM-understandable agent tasks.

---

## Task B1: Center-Align Pin/Skip Buttons in Planner

### Description
The Pin and Skip buttons in the recommendation card (`RecommendedMealCard`) should be center-aligned horizontally, not left-aligned.

### File to Modify
- `app/src/main/java/com/sranker/mealmate/ui/screens/PlannerScreen.kt` — `RecommendedMealCard` composable

### Steps
1. In `RecommendedMealCard`, find the `Row` containing the Skip and Pin buttons.
2. Add `modifier = Modifier.fillMaxWidth()` and `horizontalArrangement = Arrangement.Center` (from `Arrangement.spacedBy(12.dp)`).

---

## Task B2: Pin Icon on Left Side of Pinned Meal Card (Remove Right-Side Pin Action)

### Description
Currently, the `PinnedMealCardContent` shows a left pin icon as a pinned indicator AND a right-side `PushPin` IconButton as the unpin action. The user wants:
- The right-side pin icon (which serves as the unpin action) should be removed/changed.
- A pin icon should appear on the left side.
- The unpin should instead be triggered by swiping or by tapping a proper "unpin" visual (not another pin icon).

### File to Modify
- `app/src/main/java/com/sranker/mealmate/ui/screens/PlannerScreen.kt` — `PinnedMealCardContent` composable

### Steps
1. In `PinnedMealCardContent`, change the right-side `IconButton` with `Icons.Default.PushPin` to use a different icon (e.g., `Icons.Default.Clear` or `Icons.Default.Close`) for clarity, or replace it with a small `Icon` that clearly indicates removal (like a minus/close icon with error tint).
2. The left-side `PushPin` icon should remain (it's correct).
3. The `contentDescription` should clearly indicate this is an "unpin" action.
4. Swipe-to-unpin via `SwipeToDismissBox` should still work.

---

## Task B3: Undo Unpinning for Meal Plan

### Description
When a meal is unpinned from the active menu, show a Snackbar with an "Undo" action (similar to undo delete in the meal list).

### Files to Modify
- `app/src/main/java/com/sranker/mealmate/ui/viewmodel/PlannerViewModel.kt` — Emit an event when unpinning, support undo.
- `app/src/main/java/com/sranker/mealmate/ui/screens/PlannerScreen.kt` — Show snackbar with undo action.
- `app/src/main/res/values/strings.xml` + `values-en/strings.xml` — Add string resource for "Meal unpinned" and "Undo" (reuse existing `meal_list_undo` if available).

### Steps
1. In `PlannerViewModel`, add a `_events` `MutableSharedFlow` (like `MealListViewModel`) that emits events.
2. Define a `PlannerEvent` sealed interface with `MealUnpinned(mealId: Long)`.
3. In `unpinMeal()`, before calling `menuRepository.unpinMealFromActiveMenu(mealId)`, save the pinned data and emit `MealUnpinned`.
4. Add `undoUnpinMeal()` function that re-pins the meal via `menuRepository.pinMealToActiveMenu(mealId)`.
5. In `PlannerScreen`, collect events and show a Snackbar with "Undo" action.
6. Add string resource key `meal_unpinned` to both locale files.

---

## Task B4: Skip Should Not Return Same Food Until Full Cycle

### Description
Currently, when a meal is skipped, it could be recommended again immediately on the next `recommendMeal()` call because it's only excluded if it's the `lastRecommendedMealId`. The user wants: when a food is skipped, it should NOT appear again until the user has accepted and finished the menu (completed a full cycle).

### Files to Modify
- `app/src/main/java/com/sranker/mealmate/ui/viewmodel/PlannerViewModel.kt` — Track skipped meal IDs in a set, clear on accept+finish.
- `app/src/main/java/com/sranker/mealmate/data/MenuRepository.kt` — Add method to get skipped meal IDs if needed.

### Steps
1. In `PlannerViewModel`, add `private var skippedMealIds: Set<Long> = emptySet()`.
2. In `recommendMeal()`, when a meal is skipped (previous recommendation not pinned), add its `mealId` to `skippedMealIds`.
3. Append `skippedMealIds` to the `excludeIds` list passed to `getRandomMealNotInCooldown()`.
4. In `acceptMenu()`, clear `skippedMealIds` (or keep until finish — depends on desired behavior). The requirement says "until a full cycle has completed (`skip food -> accept menu -> finish menu -> food appears again`)". So clear `skippedMealIds` in `finishMenu()`.
5. In `finishMenu()`, after the menu is finished, clear `skippedMealIds = emptySet()`.

---

## Task B5: Reorder Meal Detail Fields — Cooked/Skipped at Bottom

### Description
In `MealDetailScreen.kt`, the stats row (timesCooked, timesSkipped, servingSize) currently appears right after the meal name. It should appear at the very bottom of the content, after ingredients. Tags should be displayed directly below the meal name.

### File to Modify
- `app/src/main/java/com/sranker/mealmate/ui/screens/MealDetailScreen.kt`

### Steps
1. In the `MealDetailScreen` content Column, reorder the blocks:
   - Current: Name → Stats → Tags → Source → Recipe → Ingredients
   - New: Name → Tags → Source → Recipe → Ingredients → Stats
2. Move the complete stats `Row` block to after the ingredients section.
3. Keep the dividers for visual separation.

---

## Task B6: Show Validation Error Messages in Meal Edit Screen

### Description
When validation fails during meal creation/editing, error messages should be visible. Currently `nameError` is shown as `supportingText` on the name field, but `ingredientsError` is not displayed anywhere.

### Files to Modify
- `app/src/main/java/com/sranker/mealmate/ui/screens/MealEditScreen.kt` — Add display of `ingredientsError` below ingredients section.

### Steps
1. In `MealEditScreen`, after the ingredients list and the "Add ingredient" button, add an error text display:
   - If `state.ingredientsError != null`, show a `Text` with `color = MaterialTheme.colorScheme.error` below the ingredients.
2. Ensure `onNameChanged` also clears the `nameErrorResId` when name is non-blank (currently it clears `nameError` but not `nameErrorResId` — should be fixed).

---

## Task B7: Refresh Meal Details After Editing

### Description
After navigating back from editing a meal, the meal detail screen shows stale data. The `MealDetailViewModel` needs to refresh its data when the screen is re-shown.

### Files to Modify
- `app/src/main/java/com/sranker/mealmate/ui/viewmodel/MealDetailViewModel.kt` — Add `refresh()` method or trigger reload when screen is active.
- `app/src/main/java/com/sranker/mealmate/ui/screens/MealDetailScreen.kt` — Call `refresh()` on composition/re-entry.

### Steps
1. In `MealDetailViewModel`, add a `triggerRefresh` method (or use `LaunchedEffect` / `DisposableEffect`).
2. The simplest approach: expose a `refresh()` function that reloads the meal data.
3. In `MealDetailScreen`, use `LaunchedEffect(Unit)` to call `viewModel.refresh()` each time the screen re-composes (it already loads in `init`, but the screen may be recreated when navigating back).
4. Alternatively, observe the meal data reactively instead of loading once.

---

## Task B8: Delete from Meal Detail with Undo

### Description
The trash/delete icon in the meal detail screen should delete the meal but with an undo option (snackbar). Currently it just pops back.

### Files to Modify
- `app/src/main/java/com/sranker/mealmate/ui/screens/MealDetailScreen.kt` — Change delete handler, show snackbar with undo.
- `app/src/main/java/com/sranker/mealmate/ui/viewmodel/MealDetailViewModel.kt` — Add delete with undo support.
- `app/src/main/java/com/sranker/mealmate/navigation/AppNavGraph.kt` — Handle delete differently (not just popBackStack).

### Steps
1. In `MealDetailViewModel`, add `deleteMeal()`, `undoDeleteMeal()`, and a `_events` shared flow.
2. Define `MealDetailEvent` sealed interface with `MealDeleted` event.
3. In `MealDetailScreen`, collect events and show snackbar with "Undo" action.
4. Change the `onDeleteClick` callback to a `viewModel.deleteMeal()` call directly.
5. When the snackbar is dismissed without undo, call `onBackClick`/`navigateBack`.

---

## Task B9: Language Selector as Dropdown

### Description
Change the language selector in `SettingsScreen` from a `SegmentedButtonRow` to an `ExposedDropdownMenuBox` (Material3 dropdown).

### Files to Modify
- `app/src/main/java/com/sranker/mealmate/ui/screens/SettingsScreen.kt`

### Steps
1. Replace the `SingleChoiceSegmentedButtonRow` with an `ExposedDropdownMenuBox` + `OutlinedTextField` (read-only) + `DropdownMenu`.
2. Keep the same three options: System, Magyar, English.
3. The dropdown should display the currently selected language label.

---

## Task B10: Sort Meals by creationDateTime Desc

### Description
Add a `createdAt` timestamp field to `MealEntity` and sort all meal list queries by `created_at DESC` (newest first).

### Files to Modify
- `app/src/main/java/com/sranker/mealmate/data/MealEntity.kt` — Add `createdAt: Long = System.currentTimeMillis()`.
- `app/src/main/java/com/sranker/mealmate/data/MealDao.kt` — Change all `ORDER BY name ASC` to `ORDER BY created_at DESC`.
- `app/src/main/java/com/sranker/mealmate/data/MealMateDatabase.kt` — Bump version to 3, add MIGRATION_2_3.
- `app/src/main/java/com/sranker/mealmate/data/MealRepository.kt` — No changes needed (delegates to DAO).

### Steps
1. Add `@ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()` to `MealEntity`.
2. Create migration `MIGRATION_2_3`: `ALTER TABLE meals ADD COLUMN created_at INTEGER NOT NULL DEFAULT 0`.
3. Update version to 3 in `@Database` annotation.
4. Update all `ORDER BY name ASC` queries in `MealDao` to `ORDER BY created_at DESC`.
5. For the random selection queries, keep `ORDER BY RANDOM()` (no change needed).

---

## Task B11: Toast When No More Recommendations

### Description
In the weekly planner, when `recommendMeal()` finds no eligible meal (returned null), show a snackbar/toast message to inform the user.

### Files to Modify
- `app/src/main/java/com/sranker/mealmate/ui/viewmodel/PlannerViewModel.kt` — Emit a `NoRecommendationAvailable` event.
- `app/src/main/java/com/sranker/mealmate/ui/screens/PlannerScreen.kt` — Consume the event and show snackbar.
- `app/src/main/res/values/strings.xml` + `values-en/strings.xml` — Add string resource `planner_no_recommendation_toast`.

### Steps
1. Add `NoRecommendationAvailable` to `PlannerEvent` sealed interface.
2. In `recommendMeal()`, when the result is null, emit `NoRecommendationAvailable` event.
3. In `PlannerScreen`, collect events and show snackbar with the message.

---

## Task B12: Toggle Completion State (Unselect Food)

### Description
Currently, the checkbox in `PinnedMealCardContent` (post-acceptance) calls `onToggleComplete()` but it only marks the meal as completed (one-way). The user must be able to both select and deselect the completion state.

### Files to Modify
- `app/src/main/java/com/sranker/mealmate/data/MenuDao.kt` — Add a query to unmark completion.
- `app/src/main/java/com/sranker/mealmate/data/MenuRepository.kt` — Add `unmarkMealCompleted()` or change `markMealCompleted` to toggle.
- `app/src/main/java/com/sranker/mealmate/ui/viewmodel/PlannerViewModel.kt` — Update `onMealCompletedToggled` to actually toggle.

### Steps
1. In `MenuDao`, add a query to reset `is_completed` for a meal in a specific menu:
   ```sql
   @Query("UPDATE menu_meal_cross_ref SET is_completed = 0 WHERE menuId = :menuId AND mealId = :mealId")
   suspend fun unmarkMealCompleted(menuId: Long, mealId: Long)
   ```
2. In `MenuRepository`, update `markMealCompleted()` to toggle: if already completed, unmark it; otherwise mark it.
   - Need a way to get the current cross-ref state for a specific meal. Use `getMenuMealCrossRef()`.
3. In `PlannerViewModel`, the `onMealCompletedToggled` already exists — the toggle logic moves to the repository.

---

## Task B13: Remove CheckCircle Icon from Right Side

### Description
When a food is marked as completed in the accepted menu, the `CheckCircle` icon appears on the right side. The user wants it removed.

### File to Modify
- `app/src/main/java/com/sranker/mealmate/ui/screens/PlannerScreen.kt` — `PinnedMealCardContent` composable.

### Steps
1. In `PinnedMealCardContent`, remove the block that renders the `Icons.Default.CheckCircle` icon when `isCompleted` is true (around lines 661-668).

---

## Task B14: Accept Menu Clears Selected Tag Filter

### Description
When the user presses "Accept Menu", any active tag filter (selectedTagIds) should be cleared.

### File to Modify
- `app/src/main/java/com/sranker/mealmate/ui/viewmodel/PlannerViewModel.kt` — `acceptMenu()` function.

### Steps
1. In `acceptMenu()`, after setting `isAccepted = true`, also set `selectedTagIds = emptySet()`.
2. Clear the tag filter visually by resetting the state.

---

## Summary of All Tasks

| #   | Task                                     | Priority | Dependencies    |
|-----|------------------------------------------|----------|-----------------|
| B1  | Center-align Pin/Skip buttons            | Low      | None            |
| B2  | Pin icon on left, change right-side icon | Medium   | None            |
| B3  | Undo unpinning                           | Medium   | None            |
| B4  | Skip exclusion until full cycle          | High     | None            |
| B5  | Reorder meal detail fields               | Medium   | None            |
| B6  | Show validation errors                   | High     | None            |
| B7  | Refresh meal detail after edit           | High     | None            |
| B8  | Delete from detail with undo             | Medium   | None            |
| B9  | Language selector as dropdown            | Low      | None            |
| B10 | Sort meals by creation date desc         | High     | DB version bump |
| B11 | Toast when no recommendations            | Medium   | None            |
| B12 | Toggle completion state                  | Medium   | None            |
| B13 | Remove right-side checkmark              | Low      | None            |
| B14 | Accept menu clears tag filter            | Medium   | None            |

