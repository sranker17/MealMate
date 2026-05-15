# Bug Fix & Feature Enhancement Plan

## Overview
This document breaks down all reported issues into fully separable, LLM-understandable agent tasks. Each task is self-contained with clear file paths, root cause analysis, and implementation details.

---

## Task 1: Fix Weekly Planner Filter Not Working

### Root Cause
In `PlannerScreen.kt`, the filter `IconButton` sets `showFilterSheet = true` (line 144), but `showFilterSheet` is never used to conditionally render any filter selection UI. There is no sheet, dialog, or dropdown that lets the user select tags to filter by in the planner. The `FilterChipRow` composable only shows already-selected tags (when `state.selectedTagIds.isNotEmpty()`), but there's no way to *add* tags to `selectedTagIds` from the planner screen — only the `onTagFilterToggled` in `PlannerViewModel` exists, but it's never connected to a UI element that lists all available tags for selection.

### Files to Modify
- `app/src/main/java/com/sranker/mealmate/ui/screens/PlannerScreen.kt`
- (Optional) `app/src/main/res/values/strings.xml`

### Implementation
1. Replace the unused `showFilterSheet` boolean state with a `showFilterSheet` boolean that controls the visibility of a full tag selection UI.
2. When the filter icon is tapped, show a bottom sheet (`ModalBottomSheet`) or an overlay containing all available tags as selectable chips.
3. The tag selection UI should use `state.allTags` and `state.selectedTagIds`, calling `viewModel.onTagFilterToggled(tagId)` on tap.
4. Add a "Clear filters" button when filters are active.
5. Style the bottom sheet consistently with the app theme.

### Acceptance Criteria
- Tapping the filter icon in the planner opens a filter selection UI.
- Tags can be toggled on/off.
- Active filters are shown as chips above the recommend button (existing `FilterChipRow`).
- Clearing filters removes all selected tags.

---

## Task 2: Load Archived Menu into Weekly Planner

### Root Cause
There is no UI or functionality to load meals from a completed (archived) menu into the active planner menu. The `MenuRepository` has methods to get completed menus, but no method to copy meals from a completed menu into the active menu.

### Files to Modify
- `app/src/main/java/com/sranker/mealmate/data/MenuRepository.kt`
- `app/src/main/java/com/sranker/mealmate/ui/screens/MenuHistoryDetailScreen.kt`
- `app/src/main/java/com/sranker/mealmate/ui/viewmodel/MenuHistoryDetailViewModel.kt`
- `app/src/main/java/com/sranker/mealmate/navigation/AppNavGraph.kt` (if adding a new callback)
- `app/src/main/res/values/strings.xml`

### Implementation
1. Add a `loadMenuIntoPlanner(menuId: Long)` suspend function to `MenuRepository` that:
   - Gets the completed menu with its meals via `getMenuWithMeals(menuId)`.
   - Gets or creates the active menu via `getOrCreateActiveMenu()`.
   - For each meal in the completed menu, calls `addMealToActiveMenu(meal.id)`.
2. Add a `onLoadIntoPlanner: (Long) -> Unit` parameter to `MenuHistoryDetailScreen`.
3. Add a "Load into Planner" button to `MenuHistoryDetailScreen`'s header area that calls `onLoadIntoPlanner(menuId)`.
4. In `AppNavGraph`, pass a lambda to `MenuHistoryDetailScreen` that calls into `PlannerScreen` (or handles navigation).
5. Show a snackbar confirming the meals were loaded.

### Acceptance Criteria
- A "Load into Planner" button appears on archived menu detail screens.
- Tapping it loads all meals from that archived menu into the current active menu.
- The user sees a confirmation snackbar.
- Duplicate meals are not added (the active menu already has them).

---

## Task 3: Rounded Input Fields & Sentence Capitalization Everywhere

### Root Cause
Input fields use `MaterialTheme.shapes.medium` or `MaterialTheme.shapes.small` but don't explicitly define rounded shapes. Keyboard options don't include `KeyboardCapitalization.Sentences`.

### Files to Modify
- `app/src/main/java/com/sranker/mealmate/ui/screens/MealEditScreen.kt`
- `app/src/main/java/com/sranker/mealmate/ui/screens/MealListScreen.kt`
- `app/src/main/java/com/sranker/mealmate/ui/screens/SettingsScreen.kt`
- `app/src/main/java/com/sranker/mealmate/ui/screens/TagManageScreen.kt`
- `app/src/main/java/com/sranker/mealmate/ui/screens/MenuHistoryDetailScreen.kt`

### Implementation
1. Create a reusable rounded shape: `RoundedCornerShape(12.dp)` (or use `MaterialTheme.shapes.medium` which should already be rounded if configured in theme; if not, use explicit `RoundedCornerShape`).
2. For every `OutlinedTextField`, add `shape = RoundedCornerShape(12.dp)` where it's missing.
3. For text input fields (name, recipe, ingredients, tag name, search), add `keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)`.
4. For URL/number fields, keep the existing `KeyboardType` but still add the shape.

### Acceptance Criteria
- All text input fields have visibly rounded corners.
- The keyboard starts with sentence-style capitalization (first letter capitalized) for all text input fields (not for numbers/URLs).

---

## Task 4: Remove Direct Deletion from Food List

### Root Cause
`MealListScreen.kt`'s `MealListItem` composable (line 275-280) has a delete `IconButton` with a trash icon that directly calls `onDelete`. Food items should only be deletable from their detail page.

### Files to Modify
- `app/src/main/java/com/sranker/mealmate/ui/screens/MealListScreen.kt`
- `app/src/main/java/com/sranker/mealmate/navigation/AppNavGraph.kt`

### Implementation
1. Remove the delete `IconButton` from `MealListItem` composable (lines 275-280 and related).
2. Update the `onDelete` parameter in `MealListItem` and its callers.
3. Remove the `onDeleteMeal` parameter from `MealListScreen` if no longer needed.
4. In `AppNavGraph`, remove the `onDeleteMeal = { viewModel.deleteMeal(it) }` line.

### Acceptance Criteria
- The food list shows no delete buttons on individual items.
- Deletion is only possible from the Meal Detail screen (via the delete icon in the header).
- (Deletion from detail already has undo — see Task 9 if not.)

---

## Task 5: Prevent Adding New Ingredient When Empty Fields Exist

### Root Cause
In `MealEditScreen.kt`, the "Add ingredient" button (line 219-236) calls `viewModel::onAddIngredient` unconditionally. In `MealEditViewModel.kt`, `onAddIngredient()` (line 176-181) always adds a new empty row regardless of whether existing ingredient fields are empty.

### Files to Modify
- `app/src/main/java/com/sranker/mealmate/ui/viewmodel/MealEditViewModel.kt`
- `app/src/main/java/com/sranker/mealmate/ui/screens/MealEditScreen.kt`

### Implementation
1. In `MealEditViewModel`, add a computed property or logic: `areAllIngredientsFilled` that checks if all current ingredient names (trimmed) are non-blank.
2. In `onAddIngredient()`, guard the addition: if any ingredient has a blank/whitespace-only name, do not add and optionally set an error.
3. In `MealEditScreen`, disable the "Add ingredient" button when `areAllIngredientsFilled` is false, or show a validation message.

### Acceptance Criteria
- The "Add ingredient" button is disabled when any ingredient field is empty or contains only spaces.
- Once all fields have non-empty text, the button becomes enabled again.
- A visual hint (e.g., supporting text or button disabled state) indicates why the button is disabled.

---

## Task 6: Back Button Confirmation on Unsaved Changes in Meal Edit

### Root Cause
In `MealEditScreen.kt`, the back `IconButton` (line 82) directly calls `onBackClick` without checking if there are unsaved changes. In `AppNavGraph.kt` (line 137), `onBackClick = { navController.popBackStack() }` just navigates back immediately.

### Files to Modify
- `app/src/main/java/com/sranker/mealmate/ui/screens/MealEditScreen.kt`
- `app/src/main/java/com/sranker/mealmate/ui/viewmodel/MealEditViewModel.kt`
- `app/src/main/res/values/strings.xml`

### Implementation
1. Add a `hasUnsavedChanges()` function to `MealEditViewModel` that compares the current form state against the initial loaded state or checks if any field differs from defaults.
2. In `MealEditScreen`, when the back button is pressed:
   - If there are no unsaved changes, proceed with `onBackClick` immediately.
   - If there are unsaved changes, show an `AlertDialog` with three options:
     - "Save & Exit" → calls `viewModel.saveMeal()`, then navigates back on success.
     - "Discard & Exit" → calls `onBackClick`.
     - "Cancel" → dismisses the dialog.
3. Add new string resources for the dialog title and buttons.

### Acceptance Criteria
- Pressing back on the edit screen with no changes exits immediately.
- Pressing back with unsaved changes shows a dialog with Save/Exit, Discard/Exit, and Cancel options.
- "Save & Exit" saves the meal and then navigates back.
- "Discard & Exit" navigates back without saving.
- Works for both new meals and editing existing ones.

---

## Task 7: Reduce Food Details Text Size

### Root Cause
In `MealDetailScreen.kt`, the recipe text uses `MaterialTheme.typography.bodyLarge` (line 251, 16sp at 1.0x scale) and ingredients also use `bodyLarge` (line 291). The user wants these to be smaller.

### Files to Modify
- `app/src/main/java/com/sranker/mealmate/ui/screens/MealDetailScreen.kt`

### Implementation
1. Change the recipe text style from `bodyLarge` to `bodyMedium` (14sp at 1.0x scale).
2. Change the ingredient text style from `bodyLarge` to `bodyMedium`.
3. Optionally adjust the `StatItem` value text from `titleLarge` to `titleMedium` if it looks too large.

### Acceptance Criteria
- Recipe text is visibly smaller on the detail page.
- Ingredient names are visibly smaller.
- Overall readability is maintained.

---

## Task 8: Reorder Fields on Food Detail Page

### Root Cause
In `MealDetailScreen.kt`, the current field order is:
1. Name & Tags (lines 179-202)
2. Source link (lines 205-236)
3. Recipe (lines 238-256)
4. Ingredients (lines 258-296)
5. Stats (lines 298-325)

Required order (confirmed with user):
1. **Name** (at the top)
2. **Recipe**
3. **Ingredients**
4. **Type** (tags)
5. **Source**
6. **Stats** (Servings, Times Cooked, Skips) at the bottom

### Files to Modify
- `app/src/main/java/com/sranker/mealmate/ui/screens/MealDetailScreen.kt`

### Implementation
1. Keep the meal name section at the very top.
2. Reorder the remaining sections in the scrollable column (inside the `else` branch starting at line 170): move Recipe up (right after name), then Ingredients, then Tags (labeled as "Type"), then Source, then Stats at the bottom.
3. Use the tags label as "Type" concept — adjust the heading string if needed.
4. Keep the same visual styling for each section (HorizontalDivider separators, etc.).

### Acceptance Criteria
- The detail page displays fields in this order: Name → Recipe → Ingredients → Type (Tags) → Source → Stats (Servings, Times Cooked, Skips).

---

## Task 9: Add Undo Support for All Deletions

### Root Cause
Deletion undo is currently only implemented for meals (in `MealListViewModel` and `MealDetailViewModel`). Tag deletion in `TagManageViewModel` and `TagManageScreen` has no undo — it deletes immediately without any snackbar/confirmation.

### Files to Modify
- `app/src/main/java/com/sranker/mealmate/ui/viewmodel/TagManageViewModel.kt`
- `app/src/main/java/com/sranker/mealmate/ui/screens/TagManageScreen.kt`
- `app/src/main/res/values/strings.xml`

### Implementation
1. In `TagManageViewModel`:
   - Add a `lastDeletedTag: TagEntity?` field.
   - Modify `deleteTag()` to save the deleted tag, emit a one-shot event, and actually delete.
   - Add `undoDeleteTag()` that re-inserts the tag.
   - Add a `sealed interface TagManageEvent` with a `TagDeleted` event.
2. In `TagManageScreen`:
   - Add a `SnackbarHostState` and `LaunchedEffect` to collect events.
   - On `TagDeleted`, show a snackbar with "Címke törölve" (Tag deleted) and an "Undo" action.
   - On undo, call `viewModel.undoDeleteTag()`.
3. Add string resources for the tag deleted message and undo action.

### Acceptance Criteria
- Deleting a tag shows a snackbar with "Tag deleted" and an undo button.
- Tapping undo restores the deleted tag.
- Not tapping undo permanently deletes the tag.

---

## Task 10: Fix Pinned Food Disappearing After Edit & Save

### Root Cause
In `MealRepository.saveMeal()` (line 44-63), the method uses `mealDao.insert(meal)` with `OnConflictStrategy.REPLACE`. When `@Insert` with `REPLACE` is used on a row with an existing ID, Room deletes the old row and inserts a new one, which can change the row ID. This breaks the foreign key relationship in `menu_meal_cross_ref` because the old meal ID no longer exists.

The fix is to use `@Update` when the meal already exists (`meal.id > 0`) and `@Insert` when it's new, or use `@Upsert` in newer Room versions.

### Files to Modify
- `app/src/main/java/com/sranker/mealmate/data/MealDao.kt`
- `app/src/main/java/com/sranker/mealmate/data/MealRepository.kt`

### Implementation
1. In `MealDao`, add an `@Upsert` method (Room 2.6.0+) that properly handles insert-or-update without destroying the row ID:
   ```kotlin
   @Upsert
   suspend fun upsert(meal: MealEntity): Long
   ```
   (Note: `@Upsert` is from `androidx.room.Upsert` — verify Room version supports it. If not, split into explicit insert/update.)
2. In `MealRepository.saveMeal()`, use the appropriate method:
   ```kotlin
   val mealId = if (meal.id > 0L) {
       mealDao.update(meal)
       meal.id
   } else {
       mealDao.insert(meal)
   }
   ```
3. Update `IngredientDao` and `TagDao` operations similarly to ensure the correct meal ID is used.

### Acceptance Criteria
- Editing a pinned meal and saving does not remove it from the pinned list.
- The meal remains in the active menu after editing.
- All meal data (ingredients, tags) is correctly updated.

---

## Task 11: Prevent Deleting Pinned Foods

### Root Cause
`MealDetailScreen.kt` shows a delete button (line 135-141) unconditionally when a meal ID is present. There is no check to see if the meal is currently pinned in the active menu.

### Files to Modify
- `app/src/main/java/com/sranker/mealmate/ui/viewmodel/MealDetailViewModel.kt`
- `app/src/main/java/com/sranker/mealmate/ui/screens/MealDetailScreen.kt`
- `app/src/main/java/com/sranker/mealmate/data/MenuRepository.kt`

### Implementation
1. In `MealDetailUiState`, add an `isPinned: Boolean` field.
2. In `MealDetailViewModel.loadMeal()`, also check if the meal is in the active menu:
   - Inject `MenuRepository`.
   - Check `menuRepository.getActiveMenuCrossRefs().any { it.mealId == mealId && it.isPinned }`.
3. In `MealDetailScreen`, conditionally hide the delete button (or disable it with a tooltip) when `state.isPinned` is true.
4. If the user tries to delete but can't, optionally show a snackbar saying "Cannot delete a pinned meal."

### Acceptance Criteria
- The delete button on the meal detail page is hidden or disabled when the meal is currently pinned in the active menu.
- Non-pinned meals can still be deleted normally (with undo support).

---

## Task 12: Fix Color Picker Colors to Match Applied Colors

### Root Cause
In `SettingsScreen.kt`, the `SettingAccentColorCard` composable (lines 376-384) defines color swatches with hardcoded hex values that DO NOT match the actual `AccentColor` values defined in `Color.kt`:

| Picker Color (SettingsScreen.kt)    | Actual Color (Color.kt / Theme.kt)           |
|-------------------------------------|----------------------------------------------|
| `teal` → `0xFF00897B` (dark teal)   | `OceanTeal` → `0xFF14B8A6` (lighter teal)    |
| `green` → `0xFF2E7D32` (dark green) | `ForestGreen` → `0xFF4ADE80` (lighter green) |
| `pink` → `0xFFC2185B` (dark pink)   | `SunsetPink` → `0xFFD34273` (different pink) |
| `slate` → `0xFF546E7A` (dark slate) | `SnowSlate` → `0xFF94A3B8` (lighter slate)   |
| `sky` → `0xFF0277BD` (dark blue)    | `SkyPrimary` → `0xFF7DD3FC` (light blue)     |
| `rose` → `0xFFAD1457` (dark rose)   | `RosePrimary` → `0xFFFDA4AF` (light pink)    |
| `sand` → `0xFFA1887F` (brownish)    | `SandPrimary` → `0xFFD4D4D8` (greyish)       |

### Files to Modify
- `app/src/main/java/com/sranker/mealmate/ui/screens/SettingsScreen.kt`

### Implementation
1. Update the `listOf` in `SettingAccentColorCard` to use actual `AccentColor` object color properties instead of hardcoded hex values. This ties the picker swatches directly to the source of truth:
   - `"teal"` → `AccentColor.Teal.darkPrimary` (OceanTeal: `0xFF14B8A6`)
   - `"green"` → `AccentColor.Green.darkPrimary` (ForestGreen: `0xFF4ADE80`)
   - `"pink"` → `AccentColor.Pink.darkPrimary` (SunsetPink: `0xFFD34273`)
   - `"slate"` → `AccentColor.Slate.darkPrimary` (SnowSlate: `0xFF94A3B8`)
   - `"sky"` → `AccentColor.Sky.darkPrimary` (SkyPrimary: `0xFF7DD3FC`)
   - `"rose"` → `AccentColor.Rose.darkPrimary` (RosePrimary: `0xFFFDA4AF`)
   - `"sand"` → `AccentColor.Sand.darkPrimary` (SandPrimary: `0xFFD4D4D8`)
2. Add the necessary import for `AccentColor` in `SettingsScreen.kt`.
3. This approach ensures future color changes in `Color.kt` automatically propagate to the picker without manual updates.

### Acceptance Criteria
- The color swatches in the Settings color picker visually match the colors that are actually applied when selected.
- Selecting a color applies that exact shade to the app theme.
- Future color changes in `Color.kt` automatically stay in sync with the picker.

---

## Task 13: Fix Import to Only Add New Tags

### Root Cause
In `BackupRepository.kt`, `importFromJson()` (lines 96-148) resolves tags using `tagDao.getTagByName(tagName)` to check if a tag exists before inserting. While the lookup logic is correct, there are two potential issues:

1. **Case sensitivity**: `getTagByName` uses `WHERE name = :name` which is case-sensitive in SQLite. If the database has a tag "Pasta" but the import file contains "pasta", a duplicate tag is created instead of reusing the existing one.

2. **`OnConflictStrategy.REPLACE` on TagDao**: `TagDao.insert()` uses `@Insert(onConflict = OnConflictStrategy.REPLACE)`. If a tag with the same name already exists but with a different auto-generated ID, REPLACE would delete the old tag row and insert a new one with a different ID. This breaks existing `meal_tag_cross_ref` associations that referenced the old tag ID.

### Files to Modify
- `app/src/main/java/com/sranker/mealmate/data/BackupRepository.kt`
- `app/src/main/java/com/sranker/mealmate/data/TagDao.kt` (if adding case-insensitive query)

### Implementation
1. Make `getTagByName` case-insensitive: `WHERE LOWER(name) = LOWER(:name) LIMIT 1`.
2. Track newly-created tags in a local map during import to avoid re-querying and potential ID mismatch.
3. Use `@Insert(onConflict = OnConflictStrategy.IGNORE)` for tags or handle insert/selection more carefully to avoid replacing existing tags.

### Acceptance Criteria
- Importing a JSON file only creates tags that don't already exist in the database.
- Existing tags are reused, not duplicated.
- Existing meal-tag associations are not broken by the import.

---

## Task 14: Add "Load Archived Menu" Button Access from Planner

### Root Cause
There's no direct way from the Planner screen to load an archived menu. The user must navigate to Menu History, find the menu, tap it, then tap "Load into Planner" (after Task 2 is implemented).

This task adds a shortcut button in the Planner screen to quickly access and load archived menus.

### Files to Modify
- `app/src/main/java/com/sranker/mealmate/ui/screens/PlannerScreen.kt`
- `app/src/main/java/com/sranker/mealmate/navigation/AppNavGraph.kt`
- `app/src/main/res/values/strings.xml`

### Implementation
1. Add an `onLoadArchivedMenu: () -> Unit` callback parameter to `PlannerScreen`.
2. Add a small "Load Menu" button/link in the planner header area (next to the filter icon).
3. In `AppNavGraph`, wire this callback to navigate to the `MENU_HISTORY` route (or a dedicated picker).
4. (Alternatively) Implement an in-planner dialog that lists archived menus for quick loading.

### Acceptance Criteria
- The planner screen has a way to load an archived menu.
- Tapping it allows the user to select from archived menus.
- Selected menu's meals are added to the active planner.

---

## Task 15: Wire Navigation for "Load Into Planner" from Menu History Detail

### Root Cause
`AppNavGraph.kt` doesn't pass an `onLoadIntoPlanner` callback to `MenuHistoryDetailScreen`. The menu history detail screen needs to know how to navigate back to the planner after loading.

### Files to Modify
- `app/src/main/java/com/sranker/mealmate/navigation/AppNavGraph.kt`

### Implementation
1. In the `MENU_HISTORY_DETAIL` composable block, pass an `onLoadIntoPlanner` lambda to `MenuHistoryDetailScreen`.
2. The lambda should call `menuRepository.loadMenuIntoPlanner(menuId)` (or delegate through the ViewModel), then navigate to the planner route.
3. Use `navController.popBackStack(Routes.PLANNER, inclusive = false)` to navigate back to planner after loading.

### Acceptance Criteria
- Tapping "Load into Planner" on a menu history detail screen loads the meals and navigates back to the planner.
- The planner shows the newly loaded meals as pinned items.
- A snackbar confirms the action.
