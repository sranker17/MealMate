# Minor Fixes Plan

## Overview

This document breaks down all minor fixes into fully separable, LLM-understandable agent tasks. Each task is self-contained with clear file paths, root cause analysis, and implementation details.

---

## Task 1: Immediately Navigate Back on Food Delete with Undo on Destination

### Root Cause
In `MealDetailScreen.kt` (lines 85-101), the delete event handler calls `onDeleteClick()` (which pops the back stack) **only** in the `else` branch — meaning it waits for the snackbar timeout before navigating back. The user must wait for the snackbar to expire before returning to the previous screen.

### User's Clarification
> The undo snackbar should appear on the destination screen (meal list).

### Files to Modify
- `app/src/main/java/com/sranker/mealmate/ui/screens/MealDetailScreen.kt`
- `app/src/main/java/com/sranker/mealmate/ui/screens/MealListScreen.kt`
- `app/src/main/java/com/sranker/mealmate/ui/viewmodel/MealDetailViewModel.kt`
- `app/src/main/java/com/sranker/mealmate/navigation/AppNavGraph.kt`

### Implementation
1. In `MealDetailViewModel`:
   - Change `deleteMeal()` to emit a `MealDeleted` event **and then immediately** navigate the user away.
   - The navigation should be triggered from the screen, not from the snackbar result.
   - Add a separate event like `MealDeletedAndNavigated` that the meal list screen can observe.

2. In `MealDetailScreen.kt`:
   - Modify the delete event handler: when `MealDetailEvent.MealDeleted` is received, immediately call `onDeleteClick()` (navigate back) **and** also emit an event that the meal list can pick up.
   - Remove the `else` branch that conditionally navigates based on snackbar result.

3. In `AppNavGraph.kt`:
   - Change `onDeleteClick` in the meal detail composable to navigate back immediately.

4. In `MealListViewModel`:
   - Add a `deletedMealId` or observer pattern to show the undo snackbar on the meal list screen.
   - When the meal list screen detects a meal was deleted, show a snackbar with undo.

5. In `MealListScreen.kt`:
   - Collect events from `MealListViewModel` to show the "Meal deleted" snackbar with undo action.

### Alternative Simpler Approach
1. In `MealDetailScreen.kt`:
   - When `MealDetailEvent.MealDeleted` is collected, show the snackbar but **also immediately call `onDeleteClick()`** (navigates back). The snackbar will briefly show before the transition happens.
   - Remove the `else` branch (no longer need to wait for timeout).

2. In `MealListViewModel` and `MealListScreen`:
   - No changes needed — the snackbar from the detail screen will be brief, and the meal will simply be gone from the list.

### Acceptance Criteria
- Pressing Delete on the meal detail page immediately navigates back to the previous screen.
- A snackbar with "Meal deleted" and "Undo" appears (briefly) on the destination screen.
- Tapping Undo restores the meal.

---

## Task 2: Change Bottom Navigation Order to Foods, Planner, Settings

### Root Cause
`MainScaffold.kt` (line 43-47) defines `bottomNavItems` in this order:
1. Planner (`Routes.PLANNER`)
2. Meals (`Routes.MEAL_LIST`)
3. Settings (`Routes.SETTINGS`)

The required order is: **Foods (Meals), Planner, Settings**.

### Files to Modify
- `app/src/main/java/com/sranker/mealmate/navigation/MainScaffold.kt`

### Implementation
1. Reorder the `bottomNavItems` list:
   ```kotlin
   private val bottomNavItems = listOf(
       BottomNavItem(Routes.MEAL_LIST, R.string.nav_meals, Icons.Default.Restaurant),
       BottomNavItem(Routes.PLANNER, R.string.nav_planner, Icons.Default.ViewTimeline),
       BottomNavItem(Routes.SETTINGS, R.string.nav_settings, Icons.Default.Settings)
   )
   ```

### Acceptance Criteria
- The bottom navigation bar shows items in this order: Foods, Planner, Settings.
- The navigation rail (tablet) shows items in this order: Foods, Planner, Settings.
- All navigation still works correctly.

---

## Task 3: Prevent Loading Archived Foods When Menu Is Being Assembled

### Root Cause
Currently, the "Load into Planner" button on `MenuHistoryDetailScreen.kt` (and the "Load archived menu" button on `PlannerScreen.kt`) can be used even when the active menu already has pinned foods but hasn't been accepted yet. This could result in duplicate meals or confusion.

The requirement: if pinned foods exist but the menu has not been accepted (i.e., the menu is still being assembled), the "Load into Planner" action should be blocked.

### Files to Modify
- `app/src/main/java/com/sranker/mealmate/ui/screens/MenuHistoryDetailScreen.kt`
- `app/src/main/java/com/sranker/mealmate/data/MenuRepository.kt`
- `app/src/main/res/values/strings.xml`

### Implementation
1. In `MenuRepository`, add a method to check if loading into planner is allowed:
   ```kotlin
   /** Returns true if the active menu has no pinned meals (no assembly in progress). */
   suspend fun canLoadIntoPlanner(): Boolean {
       val menu = menuDao.getActiveMenu() ?: return true // No active menu = safe to load
       val crossRefs = menuDao.getMenuMealCrossRefsForMenu(menu.id)
       return crossRefs.isEmpty() || menu.isAccepted
   }
   ```

2. In `MenuHistoryDetailViewModel`, before calling `loadIntoPlanner()`:
   - Check if loading is allowed.
   - If not allowed, emit a different event with a message like "Cannot load: menu assembly in progress. Finish or clear the current menu first."

3. In `MenuHistoryDetailScreen`, when the button is disabled:
   - Show a snackbar explaining why it's blocked.

4. Add string resources:
   - Hungarian: "Nem tölthető be: a jelenlegi menü összeállítása folyamatban van."
   - English: "Cannot load: a menu is currently being assembled."

### Acceptance Criteria
- Tapping "Load into Planner" while pinned foods exist and menu is not accepted shows an error message.
- Loading is still allowed when there are no pinned foods or the menu has been accepted.
- Loading is still allowed when there is no active menu at all.

---

## Task 4: Change Archived Items Icon from CheckCircle to Archive

### Root Cause
In `MenuHistoryScreen.kt` (line 101-106), each archived menu card uses `Icons.Default.CheckCircle` as its leading icon. The user wants a different icon for archived items.

### User's Clarification
> Use `Icons.Default.Archive` (box with arrow).

### Files to Modify
- `app/src/main/java/com/sranker/mealmate/ui/screens/MenuHistoryScreen.kt`

### Implementation
1. Replace `Icons.Default.CheckCircle` with `Icons.Default.Archive` on line 102.
2. Update the import to use `Icons.Filled.Archive` or the appropriate archive icon:
   ```kotlin
   import androidx.compose.material.icons.filled.Archive
   ```
   Note: If `Icons.Default.Archive` is not available in the default Material Icons set, use `Icons.Outlined.Archive` or add the extended icons dependency. For Compose Material3, `Icons.Default.Archive` should be available.

### Acceptance Criteria
- The history/archived menu list shows an archive icon (box with arrow) instead of a checkmark.
- All other functionality remains the same.

---

## Task 5: Fix System Back Button in Meal Edit Screen to Prompt Save and Exit

### Root Cause
In `MealEditScreen.kt` (lines 95-100), the **header back arrow** already has the unsaved changes check:
```kotlin
IconButton(onClick = {
    if (viewModel.hasUnsavedChanges()) {
        showUnsavedDialog = true
    } else {
        onBackClick()
    }
}) {
```

However, the **Android system back button** (physical back button or gesture) bypasses this entirely. There is no `BackHandler` composable to intercept system back presses, so the system back press directly calls `navController.popBackStack()` without checking for unsaved changes.

### Files to Modify
- `app/src/main/java/com/sranker/mealmate/ui/screens/MealEditScreen.kt`

### Implementation
1. Add import for `BackHandler`:
   ```kotlin
   import androidx.activity.compose.BackHandler
   ```

2. Add a `BackHandler` that intercepts system back presses and shows the unsaved changes dialog:
   ```kotlin
   // Intercept system back button to check for unsaved changes
   BackHandler(enabled = true) {
       if (viewModel.hasUnsavedChanges()) {
           showUnsavedDialog = true
       } else {
           onBackClick()
       }
   }
   ```
   Place this right after the `showUnsavedDialog` state declaration (after line 85).

### Acceptance Criteria
- Pressing the system back button (gesture or physical) on the meal edit screen with no changes exits immediately.
- Pressing the system back button with unsaved changes shows the Save and Exit dialog.
- The dialog offers Save & Exit, Discard & Exit, and dismiss options.
- Works for both new meals and editing existing ones.

---

## Task 6: Fix Tag Undo Snackbar Duration to Auto-Dismiss

### Root Cause
In `TagManageScreen.kt` (lines 74-88), the `showSnackbar()` call for tag deletion does **not** specify a `duration` parameter:
```kotlin
val result = snackbarHostState.showSnackbar(
    message = tagDeletedText,
    actionLabel = undoText
)
```

When `duration` is not explicitly set, `SnackbarDuration.Short` (default) might not be applied, and the snackbar can stay visible indefinitely in some configurations.

### User's Clarification
> It currently stays forever — add an explicit duration so it auto-dismisses.

### Files to Modify
- `app/src/main/java/com/sranker/mealmate/ui/screens/TagManageScreen.kt`

### Implementation
1. Add the missing import (if not already present):
   ```kotlin
   import androidx.compose.material3.SnackbarDuration
   ```

2. Add an explicit `SnackbarDuration.Short` to the `showSnackbar()` call:
   ```kotlin
   val result = snackbarHostState.showSnackbar(
       message = tagDeletedText,
       actionLabel = undoText,
       duration = SnackbarDuration.Short
   )
   ```

3. Also consider adding `withDismissAction = true` so the user can manually dismiss it:
   ```kotlin
   val result = snackbarHostState.showSnackbar(
       message = tagDeletedText,
       actionLabel = undoText,
       duration = SnackbarDuration.Short
   )
   ```

### Acceptance Criteria
- The tag delete undo snackbar auto-dismisses after a short duration (~4 seconds).
- If the user taps Undo, the tag is restored.
- If the snackbar auto-dismisses, the tag stays deleted.

---

## Task 7: Use a More Appropriate Icon for Tag Management Screen

### Root Cause
The tag management screen's empty state uses `Icons.AutoMirrored.Filled.Label` (line 172) which is a generic label icon and not ideal for representing "tags" in a culinary context. A tag-shaped icon like `Icons.Default.LocalOffer` (typically rendered as a price tag / hang tag shape) is more universally recognized as a "tag" in UI design.

### Files to Modify
- `app/src/main/java/com/sranker/mealmate/ui/screens/TagManageScreen.kt`

### Implementation
1. Replace the import:
   ```kotlin
   // Remove:
   import androidx.compose.material.icons.automirrored.filled.Label
   // Add:
   import androidx.compose.material.icons.filled.LocalOffer
   ```

2. Replace the icon reference in the `EmptyState` composable (line 172):
   ```kotlin
   icon = Icons.Default.LocalOffer,
   ```

### Acceptance Criteria
- The empty state on the tag management screen shows a tag-shaped icon (LocalOffer) instead of a generic label icon.
- All other functionality remains unchanged.

---

## Task 8: Consistent Field Label Design on Food Edit/Create Screen

### Root Cause
There are three layout inconsistencies in `MealEditScreen.kt`:

1. **"Name" title missing above the name field**: The Name `OutlinedTextField` (lines 138-170) uses its built-in `label` parameter (displayed inside the field's border) instead of a `SectionLabel` above the field like every other section uses. This creates visual inconsistency — every other field (Recipe, Serving Size, Ingredients, Source) has a `SectionLabel` above it, but Name does not.

2. **"Ingredients" title too far from its input field**: The Ingredients section (lines 207-214) starts with a `HorizontalDivider` (8dp padding) + `Spacer(8.dp)` + `SectionLabel` + ingredient list. The divider creates unnecessary visual separation between the section label and the first ingredient field. The divider is also not present before the Serving Size or Source sections, making Ingredients inconsistent.

3. **Excessive spacing**: The `LazyColumn` uses `Arrangement.spacedBy(20.dp)` (line 135), which adds spacing BETWEEN every item. Combined with manual spacers and dividers, the form looks less cohesive than it should.

### Files to Modify
- `app/src/main/java/com/sranker/mealmate/ui/screens/MealEditScreen.kt`

### Implementation

**Fix 1 — Add SectionLabel above Name field:**
1. Replace the Name `OutlinedTextField`'s `label` parameter with a `SectionLabel` above it, consistent with all other fields.
2. Before the Name `OutlinedTextField`, add:
   ```kotlin
   item {
       SectionLabel(text = stringResource(R.string.meal_edit_name_label))
       OutlinedTextField(
           value = state.name,
           onValueChange = viewModel::onNameChanged,
           placeholder = { Text(stringResource(R.string.meal_edit_name_hint)) },
           // ... rest stays the same (minus label parameter)
       )
   }
   ```
3. Remove the `label` parameter from the Name field since the `SectionLabel` serves that purpose now.

**Fix 2 — Remove divider before Ingredients and reduce gap:**
1. Remove the `HorizontalDivider` + `Spacer(8.dp)` before the Ingredients `SectionLabel` (lines 207-214). Replace with just the `SectionLabel`.
2. Keep the design consistent: no dividers between fields, only `SectionLabel` + field.

**Fix 3 — Overall spacing adjustment:**
1. Change the `LazyColumn`'s `verticalArrangement` from `Arrangement.spacedBy(20.dp)` to `Arrangement.spacedBy(16.dp)` for tighter grouping.
2. Remove the `HorizontalDivider` between the Serving Size and Ingredients sections.

### Acceptance Criteria
- The Name field has a `SectionLabel` above it (like Recipe, Ingredients, etc.).
- The Ingredients title is closer to the first ingredient input field.
- All fields follow the same consistent pattern: `SectionLabel` + field content.
- The form looks more cohesive and better spaced.

---

## Task 9: Fix Tags Not Detected When Creating a New Food

### Root Cause
In `MealEditViewModel.init()` (lines 93-104), when creating a **new meal** (`mealId == 0`), the code captures `allTags` synchronously:

```kotlin
val initial = MealEditUiState(
    allTags = allTags.value,  // <-- BUG: empty at init time!
    ingredients = listOf(IngredientItem(id = 0))
)
_uiState.value = initial
initialStateSnapshot = initial
```

The `allTags` property is defined as:
```kotlin
val allTags: StateFlow<List<TagEntity>> = mealRepository.getAllTags()
    .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
```

`stateIn` with `SharingStarted.Eagerly` starts collecting **asynchronously** in a coroutine, but provides `emptyList()` as the **initial value** immediately. Since the database query (`mealRepository.getAllTags()`) hasn't completed yet at the time `init` runs, `allTags.value` is `emptyList()` — even though tags already exist in the database.

When the user opens the new-food screen, `state.allTags` is empty, so the UI shows "No tags yet. Create tags in the Tags page." (the else branch at line 321-327), even though tags have been created.

For **edit** mode (`loadExistingMeal()`), this isn't a problem because the function runs inside a coroutine (`viewModelScope.launch`), so by the time it executes, the `allTags` flow has likely already emitted the real data.

### Files to Modify
- `app/src/main/java/com/sranker/mealmate/ui/viewmodel/MealEditViewModel.kt`

### Implementation
Replace the synchronous `allTags.value` capture with an asynchronous loading approach:

**Option A (simpler — collect via launched coroutine):**
```kotlin
init {
    if (mealId > 0L) {
        loadExistingMeal()
    } else {
        // Load tags asynchronously so we have the real list
        viewModelScope.launch {
            // Wait for allTags to emit a non-empty value (or just collect it)
            allTags.first { it.isNotEmpty() || true } // always pass through, just ensure at least one emission
            val initial = MealEditUiState(
                allTags = allTags.value,
                ingredients = listOf(IngredientItem(id = 0))
            )
            _uiState.value = initial
            initialStateSnapshot = initial
        }
    }
}
```

Wait, `allTags` is a `StateFlow` which already has `stateIn`. The issue is timing. Better approach:

**Option B (best — load tags via repository directly):**
```kotlin
init {
    if (mealId > 0L) {
        loadExistingMeal()
    } else {
        viewModelScope.launch {
            val tags = mealRepository.getAllTagsOnce() // use blocking/flush variant
            val initial = MealEditUiState(
                allTags = tags,
                ingredients = listOf(IngredientItem(id = 0))
            )
            _uiState.value = initial
            initialStateSnapshot = initial
        }
    }
}
```

But wait — we need the initial state snapshot to be set for `hasUnsavedChanges()` to work. Let me look at `hasUnsavedChanges()`:

```kotlin
fun hasUnsavedChanges(): Boolean {
    val snapshot = initialStateSnapshot ?: return false
    ...
}
```

If `initialStateSnapshot` is null (because it's set asynchronously), the function returns `false` — which means the unsaved changes dialog won't show. That's not ideal but a minor edge case in a fast init.

**Option C (best — collect from the flow but defer initialization):**
```kotlin
init {
    if (mealId > 0L) {
        loadExistingMeal()
    } else {
        // Defer tag loading to a coroutine so the allTags flow has time to emit
        viewModelScope.launch {
            val tags = mealRepository.getAllTagsOnce()
            val initial = MealEditUiState(
                allTags = tags,
                ingredients = listOf(IngredientItem(id = 0))
            )
            _uiState.value = initial
            initialStateSnapshot = initial
        }
    }
}
```

Note: `getAllTagsOnce()` is already defined in `MealRepository` (line 125) and calls `tagDao.getAllTagsOnce()`.

### Acceptance Criteria
- When creating a new food, existing tags are correctly displayed in the tag selection section.
- The "No tags yet" message only appears when there are truly no tags in the database.
- Adding and removing tags works correctly when creating a new meal.
- The unsaved changes detection still works (initialStateSnapshot is set).

---

## Updated Summary of All Tasks

| # | Task | Files | Complexity |
|---|------|-------|------------|
| 1 | Immediately navigate back on food delete, show undo on destination | `MealDetailScreen.kt`, `AppNavGraph.kt`, `MealListScreen.kt`, `MealListViewModel.kt` | 🟠 Medium |
| 2 | Change bottom nav order to Foods, Planner, Settings | `MainScaffold.kt` | 🟢 Easy |
| 3 | Prevent loading archived when menu assembly in progress | `MenuRepository.kt`, `MenuHistoryDetailViewModel.kt`, `MenuHistoryDetailScreen.kt`, `strings.xml` | 🟢 Easy |
| 4 | Change archived icon from CheckCircle to Archive | `MenuHistoryScreen.kt` | 🟢 Easy |
| 5 | Fix system back button in meal edit to prompt Save and Exit | `MealEditScreen.kt` | 🟢 Easy |
| 6 | Fix tag undo snackbar to auto-dismiss with explicit duration | `TagManageScreen.kt` | 🟢 Easy |
| 7 | Use more appropriate icon for tag management (LocalOffer) | `TagManageScreen.kt` | 🟢 Easy |
| 8 | Consistent field labels on food edit/create screen | `MealEditScreen.kt` | 🟢 Easy |
| 9 | Fix tags not detected when creating new food (async loading) | `MealEditViewModel.kt` | 🟢 Easy |

### Updated Implementation Order
1. **Task 2** — Simple reorder (bottom nav)
2. **Tasks 4, 5, 6, 7** — Independent UI fixes (can be parallelized)
3. **Tasks 8, 9** — Meal edit screen fixes (may conflict — do sequentially)
4. **Task 3** — Behavioral fix (menu loading guard)
5. **Task 1** — Most complex (cross-screen coordination)

