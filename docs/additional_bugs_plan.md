# Additional Bugs Fix Plan

## Overview

This document breaks down all remaining reported issues into fully separable, LLM-understandable agent tasks. Each task is self-contained with clear file paths, root cause analysis, and implementation details.

> **Note:** The back-button "Save and Exit" dialog on the meal edit screen and the color picker fix were already implemented in a previous pass. Those items are marked as **✅ Already Done** and are not included as separate tasks.

---

## Task 1: Auto-Navigate to Planner After Loading Archived Menu

### Root Cause
In `MenuHistoryDetailViewModel.loadIntoPlanner()` (line 88-97), the function loads the menu's meals into the planner and emits a `LoadedIntoPlanner` event, but does not trigger any navigation. The user must manually press back to return to the planner.

### Files to Modify
- `app/src/main/java/com/sranker/mealmate/navigation/AppNavGraph.kt`

### Implementation
1. In the `MENU_HISTORY_DETAIL` composable block (line 173-191), update the `onLoadIntoPlanner` callback to navigate back to the planner after loading.
2. Currently (line 187-189):
   ```kotlin
   onLoadIntoPlanner = {
       viewModel.loadIntoPlanner()
   }
   ```
3. Change to:
   ```kotlin
   onLoadIntoPlanner = {
       viewModel.loadIntoPlanner()
       navController.popBackStack(Routes.PLANNER, inclusive = false)
   }
   ```

### Acceptance Criteria
- Tapping "Load into Planner" on a menu history detail screen loads the meals and automatically navigates back to the Planner screen.
- The planner shows the newly loaded meals.

---

## Task 2: Add Rounded Corners to Meal List Search Field

### Root Cause
In `MealListScreen.kt` (lines 127-144), the search `OutlinedTextField` uses default border shape — it doesn't have the `RoundedCornerShape(12.dp)` applied like the name field on the meal edit screen does.

### Files to Modify
- `app/src/main/java/com/sranker/mealmate/ui/screens/MealListScreen.kt`

### Implementation
1. Import `RoundedCornerShape` at the top of the file:
   ```kotlin
   import androidx.compose.foundation.shape.RoundedCornerShape
   ```
2. Add a `private val` for the rounded shape (or inline it):
   ```kotlin
   private val roundedShape = RoundedCornerShape(12.dp)
   ```
3. Add `shape = roundedShape` to the search `OutlinedTextField` (line 139 area).
4. Import `KeyboardCapitalization` and `KeyboardOptions` and add sentence-style capitalization:
   ```kotlin
   import androidx.compose.foundation.text.KeyboardOptions
   import androidx.compose.ui.text.input.ImeAction
   import androidx.compose.ui.text.input.KeyboardCapitalization
   ```
   ```kotlin
   keyboardOptions = KeyboardOptions(
       capitalization = KeyboardCapitalization.Sentences,
       imeAction = ImeAction.Search
   ),
   ```

### Acceptance Criteria
- The search field on the meal list has the same rounded corners as the name field on the edit screen.
- The keyboard starts with sentence-style capitalization.

---

## Task 3: Add Rounded Corners to Settings Recommendation Pause Field

### Root Cause
In `SettingsScreen.kt`, the `SettingCooldownCard` composable's `OutlinedTextField` (lines 262-276) uses default border shape — it doesn't have the `RoundedCornerShape(12.dp)` applied.

### Files to Modify
- `app/src/main/java/com/sranker/mealmate/ui/screens/SettingsScreen.kt`

### Implementation
1. Add import at the top:
   ```kotlin
   import androidx.compose.foundation.shape.RoundedCornerShape
   ```
2. Add a `private val` for the rounded shape (or use an existing shape from another file):
   ```kotlin
   private val roundedShape = RoundedCornerShape(12.dp)
   ```
3. Add `shape = roundedShape` to the cooldown `OutlinedTextField` (around line 270).

### Acceptance Criteria
- The recommendation pause (cooldown) text field in Settings has the same rounded corners as the name field on the meal edit screen.

---

## Task 4: Delete Meal & Navigate Back from Detail Page

### Root Cause
In `MealDetailScreen.kt` (lines 85-101), the delete event handler shows a snackbar with "Undo". If the user does NOT undo, the snackbar times out and `onDeleteClick()` is called (which pops the back stack). However, the current logic navigates back ONLY in the `else` branch of the snackbar result. The requirement is that pressing Delete should always delete and then navigate back, with undo as an option.

The current flow is actually already correct: the meal is deleted immediately (line 118 in ViewModel), the snackbar offers undo, and if not undone, navigation happens. But the delete button is hidden for pinned meals already (from previous fix). The issue might be that after undo, the user stays on the screen which is correct behavior.

No changes needed here — this is already working as expected.

### Status
**✅ Already Working** — no changes needed.

---

## Task 5: Clear Import/Export Messages on Screen Re-Entry

### Root Cause
In `SettingsViewModel`, `importResult` and `exportResult` are set after import/export and never cleared. The ViewModel is scoped to the navigation back stack entry, so if the user navigates to the planner and back, the same ViewModel instance is reused and the message persists.

### Files to Modify
- `app/src/main/java/com/sranker/mealmate/ui/viewmodel/SettingsViewModel.kt`
- `app/src/main/java/com/sranker/mealmate/ui/screens/SettingsScreen.kt`

### Implementation
1. In `SettingsViewModel`, add a method to clear result messages:
   ```kotlin
   /** Clear import/export result messages. */
   fun clearResults() {
       _uiState.value = _uiState.value.copy(
           importResult = null,
           exportResult = null,
           importResultResId = null,
           exportResultResId = null,
           importResultArg = null
       )
   }
   ```
2. In `SettingsScreen`, call `clearResults()` when the composable first enters composition so the message only appears on the current screen lifecycle:
   ```kotlin
   LaunchedEffect(Unit) {
       viewModel.clearResults()
   }
   ```
   Place this right after the state/val declarations but before the `if (showResetCooldownDialog)` block.

### Acceptance Criteria
- After import/export, the success/error message is shown on the Settings screen.
- Navigating to Planner and back clears the message (it is no longer visible).
- Navigating within Settings (e.g., to manage tags and back) also clears it.

---

## Task 6: Add Missing English Translations to strings.xml

### Root Cause
The English `strings.xml` (`values-en/strings.xml`) is missing several string resources that exist in the Hungarian `strings.xml` (`values/strings.xml`).

### Missing Strings
The following strings exist in Hungarian but are missing from English:

| String Key | Hungarian Value | English Translation |
|---|---|---|
| `planner_load_archived` | Archivált menü betöltése | Load archived menu |
| `meal_edit_unsaved_title` | El nem mentett változtatások | Unsaved changes |
| `meal_edit_unsaved_message` | Vannak el nem mentett változtatásaid. Mit szeretnél tenni? | You have unsaved changes. What would you like to do? |
| `meal_edit_save_exit` | Mentés és kilépés | Save and exit |
| `meal_edit_discard_exit` | Elvetés és kilépés | Discard and exit |
| `tag_manage_deleted` | Címke törölve | Tag deleted |
| `tag_manage_undo` | Visszavonás | Undo |
| `history_load_into_planner` | Betöltés a tervezőbe | Load into planner |
| `history_loaded_into_planner` | Ételek betöltve a tervezőbe | Meals loaded into planner |

### Files to Modify
- `app/src/main/res/values-en/strings.xml`

### Implementation
Add the missing string entries to the English `strings.xml` in their appropriate sections.

### Acceptance Criteria
- Every string resource defined in the Hungarian `strings.xml` has a corresponding English translation in `values-en/strings.xml`.
- The app does not show any missing string resource errors when English is selected.

---

## Task 7: Localize Color Names in Settings Color Picker

### Root Cause
In `SettingsScreen.kt` (line 403), the color swatch label is derived from the accent name programmatically:
```kotlin
text = name.replaceFirstChar { it.uppercase() },
```
This always shows English color names regardless of the selected language.

### Files to Modify
- `app/src/main/res/values/strings.xml` (Hungarian)
- `app/src/main/res/values-en/strings.xml` (English)
- `app/src/main/java/com/sranker/mealmate/ui/screens/SettingsScreen.kt`

### Implementation
1. Add string resources for each color name in both languages:

   **Hungarian** (`values/strings.xml`):
   ```xml
   <!-- Color names -->
   <string name="color_teal">Kékeszöld</string>
   <string name="color_green">Zöld</string>
   <string name="color_pink">Rózsaszín</string>
   <string name="color_slate">Palaszürke</string>
   <string name="color_sky">Égkék</string>
   <string name="color_rose">Rózsapiros</string>
   <string name="color_sand">Homok</string>
   ```

   **English** (`values-en/strings.xml`):
   ```xml
   <string name="color_teal">Teal</string>
   <string name="color_green">Green</string>
   <string name="color_pink">Pink</string>
   <string name="color_slate">Slate</string>
   <string name="color_sky">Sky</string>
   <string name="color_rose">Rose</string>
   <string name="color_sand">Sand</string>
   ```

2. In `SettingsScreen.kt`, update the `listOf` in `SettingAccentColorCard` to include string resource IDs:
   ```kotlin
   listOf(
       "teal" to AccentColor.Teal.darkPrimary to R.string.color_teal,
       "green" to AccentColor.Green.darkPrimary to R.string.color_green,
       // ... etc
   ).forEach { (nameColor, colorNameRes) ->
       val (name, color) = nameColor
       // ...
       Text(
           text = stringResource(colorNameRes),
           // ...
       )
   }
   ```
   Replace the `.replaceFirstChar` line with `stringResource(colorNameRes)`.

### Acceptance Criteria
- Color names in the Settings color picker are translated according to the app's language setting.
- Hungarian locale shows Hungarian color names.
- English locale shows English color names.

---

## Task 8: Remove History Option from Bottom Navigation

### Root Cause
`MainScaffold.kt` (line 43-48) defines `bottomNavItems` with 4 items: Planner, Meals, **History**, Settings. History should be removed from the bottom navigation bar and navigation rail.

### Files to Modify
- `app/src/main/java/com/sranker/mealmate/navigation/MainScaffold.kt`

### Implementation
1. Remove the `Routes.MENU_HISTORY` entry from `bottomNavItems`:
   ```kotlin
   private val bottomNavItems = listOf(
       BottomNavItem(Routes.PLANNER, R.string.nav_planner, Icons.Default.ViewTimeline),
       BottomNavItem(Routes.MEAL_LIST, R.string.nav_meals, Icons.Default.Restaurant),
       BottomNavItem(Routes.SETTINGS, R.string.nav_settings, Icons.Default.Settings)
   )
   ```
2. Optionally remove the unused `Icons.Default.CalendarMonth` import if no longer referenced (but verify it's still used for the planner's load archived button — line 152 of PlannerScreen.kt uses it, so keep the import).
3. Update the `isTopLevel` check — already uses `bottomNavItems.any { ... }` so it adapts automatically.

### Acceptance Criteria
- The bottom navigation bar shows only 3 items: Planner, Meals, Settings.
- The navigation rail (tablet) shows only 3 items: Planner, Meals, Settings.
- The History screen is still accessible from the Planner's "Load archived menu" button (it navigates via the nav graph, not the bottom bar).
- All existing navigation still works correctly.

---

## Summary of All Tasks

| # | Task | Files | Complexity |
|---|------|-------|------------|
| 1 | Auto-navigate to planner after loading archived menu | `AppNavGraph.kt` | 🟢 Easy |
| 2 | Add rounded corners + capitalization to meal list search | `MealListScreen.kt` | 🟢 Easy |
| 3 | Add rounded corners to settings cooldown field | `SettingsScreen.kt` | 🟢 Easy |
| 4 | Delete + navigate back from detail | Already working | ✅ Done |
| 5 | Clear import/export messages on screen re-entry | `SettingsViewModel.kt`, `SettingsScreen.kt` | 🟢 Easy |
| 6 | Add missing English translations | `values-en/strings.xml` | 🟢 Easy |
| 7 | Localize color names in picker | `strings.xml` (both), `SettingsScreen.kt` | 🟢 Easy |
| 8 | Remove History from bottom navigation | `MainScaffold.kt` | 🟢 Easy |

### Implementation Order (recommended)
1. **Task 8** — Structural change (removes nav item)
2. **Tasks 2 & 3** — Visual fixes (rounded corners)
3. **Tasks 1 & 5** — Functional fixes
4. **Tasks 6 & 7** — Localization
5. **Task 4** — ✅ Already verified working

