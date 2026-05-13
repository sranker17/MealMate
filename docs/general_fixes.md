# General Fixes Plan

This document breaks down all required changes into completely separable, LLM-understandable agent tasks. Each task is self-contained, has clear entry points, and specifies exactly which files to modify.

---

## Task 1: Add English Localization and Language Support

### Description
Create English string resources, set up locale-based language selection, add a language setting option to the Settings page, and replace all hardcoded Hungarian strings in composables with references to `strings.xml`.

### Files to Create
- `app/src/main/res/values-en/strings.xml` — English translations of all strings
- (No new file needed for Hungarian detection; Android handles this via resource qualifiers)

### Files to Modify
- `app/src/main/res/values/strings.xml` — Keep as Hungarian (values/ is default, or rename to values-hu/)
- `app/src/main/java/com/sranker/mealmate/data/SettingsRepository.kt` — Add `language` preference (e.g., "system" | "hu" | "en")
- `app/src/main/java/com/sranker/mealmate/data/Settings.kt` (inside SettingsRepository.kt) — Add `language` field
- `app/src/main/java/com/sranker/mealmate/ui/viewmodel/SettingsViewModel.kt` — Add `setLanguage()` function
- `app/src/main/java/com/sranker/mealmate/ui/screens/SettingsScreen.kt` — Add language picker (SegmentedButton for "System / Magyar / English")
- `app/src/main/java/com/sranker/mealmate/MainActivity.kt` — Apply locale from settings before `setContent`
- All screen files — Replace hardcoded Hungarian strings with `stringResource(R.string.xxx)` references

### Steps
1. Create `values/strings.xml` (keep existing Hungarian content — this is the default/fallback).
2. Create `values-en/strings.xml` with English translations for every string key.
3. Add `language` preference to `SettingsRepository.kt` (key, flow, setter, default = "system").
4. Add language setting UI in `SettingsScreen.kt` (e.g., a segmented button row under accent color).
5. In `MainActivity.kt`, read the language setting and apply it via `AppCompatDelegate.setApplicationLocale()` or a custom `ContextWrapper` before composing the UI.
6. Update all screen composables (`PlannerScreen`, `MealListScreen`, `MealDetailScreen`, `MealEditScreen`, `SettingsScreen`, `TagManageScreen`, `MenuHistoryScreen`, `MenuHistoryDetailScreen`, `MainScaffold`, etc.) to use `stringResource(R.string.xxx)` instead of hardcoded Hungarian strings.
7. Update `MealListViewModel.kt` snackbar messages to use string resources (emit string resource IDs instead of hardcoded text).

---

## Task 2: Fix Food List — Remove from Plan Toggle

### Description
Currently, when a food is already in the weekly plan, the bookmark button is non-functional (shows "Már hozzá van adva" snackbar). It should instead **remove** the food from the plan.

### Files to Modify
- `app/src/main/java/com/sranker/mealmate/data/MenuDao.kt` — Already has `removeMealFromMenu()` — no changes needed.
- `app/src/main/java/com/sranker/mealmate/data/MenuRepository.kt` — Already has `unpinMealFromActiveMenu()` — no changes needed.
- `app/src/main/java/com/sranker/mealmate/ui/viewmodel/MealListViewModel.kt` — In `addToActivePlan()`:
  - If the meal is already in the plan AND menu is not locked, **remove** it by calling `menuRepository.unpinMealFromActiveMenu(mealId)` and emit a new `MealListEvent.RemovedFromPlan`.
  - Only emit `AlreadyInPlan` if the menu is locked (can't modify).
- `app/src/main/java/com/sranker/mealmate/ui/screens/MealListScreen.kt`:
  - Update `MealListEvent` handling to show "Eltávolítva a heti tervből" (or string resource) snackbar for `RemovedFromPlan`.
  - Change the bookmark icon behavior: when in plan AND not locked, tapping should remove it.

### Steps
1. Add `RemovedFromPlan` to the `MealListEvent` sealed interface.
2. Modify `addToActivePlan()` in `MealListViewModel.kt`: if `mealId in mealIdsInActivePlan` and menu is not locked, call `menuRepository.unpinMealFromActiveMenu(mealId)` and emit `RemovedFromPlan`.
3. Update `MealListScreen.kt` snackbar handling to include the new event.
4. Add string resource for "Removed from weekly plan".

---

## Task 3: Fix Add Food — Tags Not Selectable

### Description
When adding a new food, tags cannot be selected even though tags exist. The issue is likely that `allTags` is loaded in the init block but the empty tags list from the initial state overrides it, or the tag chips aren't properly interactive. Also, ensure the tag selection area is fully functional.

### Files to Modify
- `app/src/main/java/com/sranker/mealmate/ui/viewmodel/MealEditViewModel.kt`:
  - In the `init` block for new meals (mealId == 0), ensure `allTags` is properly populated from the `allTags` flow.
  - Wait for tags to load before setting the initial state.
- `app/src/main/java/com/sranker/mealmate/ui/screens/MealEditScreen.kt`:
  - Ensure `FlowRow` with tag chips is clickable and responsive.

### Steps
1. In `MealEditViewModel.init`, when creating a new meal, collect the `allTags` flow to properly populate `state.allTags` before setting initial UI state.
2. Verify that `onTagToggled` works correctly for new meals (no edit mode issues).
3. Test that the tag chips in `MealEditScreen` toggle selection properly.

---

## Task 4: Reorder Fields in Meal Edit Screen

### Description
Change the field order in `MealEditScreen.kt` from:
- Current: Name, Tags, Recipe, Serving Size, Source URL, Ingredients
- New: Name, Recipe, Servings, Ingredients, Tags, Source

### Files to Modify
- `app/src/main/java/com/sranker/mealmate/ui/screens/MealEditScreen.kt`

### Steps
1. Reorder the `item {}` blocks inside the `LazyColumn` in `MealEditScreen.kt`:
   - Name (existing, keeps position)
   - Recipe (moved up from 3rd to 2nd)
   - Serving Size (keeps relative position to recipe)
   - Ingredients section with list + add button (moved up from last to 4th)
   - Tags (moved down from 2nd to 5th)
   - Source URL (moved down from 5th to 6th)
2. Ensure the `Divider` before ingredients is still present.

---

## Task 5: Add Validation — Name and One Ingredient Required

### Description
Currently only the name is validated as required. Add validation that at least one ingredient (with non-blank text) must be present before saving.

### Files to Modify
- `app/src/main/java/com/sranker/mealmate/ui/viewmodel/MealEditViewModel.kt`
- `app/src/main/res/values/strings.xml` + `values-en/strings.xml`

### Steps
1. In `MealEditViewModel.validate()`:
   - Add check: if no ingredient has non-blank text, set an `ingredientsError` (or general error) and return false.
   - Add a new `ingredientsError` property to `MealEditUiState` (nullable String).
2. In `MealEditScreen.kt`:
   - Show the ingredient error message below the ingredients section when `state.ingredientsError != null`.
3. Add string resource: "At least one ingredient is required".

---

## Task 6: Undo Delete Food

### Description
When a food is deleted from the food list, show a Snackbar with an "Undo" action. Tapping undo restores the meal.

### Files to Modify
- `app/src/main/java/com/sranker/mealmate/data/MealDao.kt` — No changes needed.
- `app/src/main/java/com/sranker/mealmate/data/MealRepository.kt` — Add a `deleteMealWithUndo()` that returns the deleted entities for potential restoration, or add a temporary cache.
- `app/src/main/java/com/sranker/mealmate/ui/viewmodel/MealListViewModel.kt`:
  - Modify `deleteMeal()` to save the deleted meal info in-memory, then emit a `MealDeleted` event with undo capability.
  - Add `undoDeleteMeal()` function.
- `app/src/main/java/com/sranker/mealmate/ui/screens/MealListScreen.kt`:
  - Show a Snackbar with "Undo" action when a meal is deleted.
  - Add `MealListEvent.MealDeleted` with the `MealWithTags` data.
- `app/src/main/java/com/sranker/mealmate/navigation/AppNavGraph.kt` — Pass snackbar host state through or handle deletion in the screen.

### Steps
1. In `MealListViewModel`, change `deleteMeal()` to:
   - Save the `MealWithTags` data in a `deletedMeal` variable.
   - Call `mealRepository.deleteMeal()` as before.
   - Emit `MealListEvent.MealDeleted` containing the meal info.
2. Add `undoDeleteMeal()` which re-inserts the meal via `mealRepository.saveMeal()`.
3. In `MealListScreen`, handle `MealDeleted` event with a Snackbar that has an "Undo" action button.
4. Add string resources for "Food deleted" and "Undo".

---

## Task 7: Modernize Icons and Illustrations

### Description
Review all icons used throughout the app and replace them with more modern, elegant alternatives from Material Icons (or add appropriate styling).

### Files to Modify
- `app/src/main/java/com/sranker/mealmate/ui/screens/**/*.kt` — All screen files
- `app/src/main/java/com/sranker/mealmate/navigation/MainScaffold.kt` — Bottom nav icons
- `app/src/main/java/com/sranker/mealmate/ui/components/EmptyState.kt`

### Steps
1. Review all `Icons.Default.*` usages across screens.
2. Replace outdated icons with Material 3 recommended alternatives:
   - `Icons.Default.Restaurant` → `Icons.Outlined.RamenDining` or `Icons.Outlined.RestaurantMenu`
   - `Icons.Default.Search` → keep or use `Icons.Outlined.Search`
   - `Icons.Default.Delete` → `Icons.Outlined.DeleteOutline` or `Icons.Outlined.Delete`
   - `Icons.Default.Add` → `Icons.Outlined.Add`
   - `Icons.Default.Edit` → `Icons.Outlined.Edit`
   - `Icons.Default.FilterList` → `Icons.Outlined.FilterList`
   - `Icons.Default.PushPin` → `Icons.Outlined.PushPin`
   - `Icons.Default.CheckCircle` → `Icons.Outlined.CheckCircle`
   - `Icons.Default.Star` → `Icons.Outlined.Star`
   - `Icons.Default.AutoAwesome` → `Icons.Outlined.AutoAwesome`
   - `Icons.Default.SkipNext` → `Icons.Outlined.SkipNext`
   - `Icons.Default.Shuffle` → `Icons.Outlined.Shuffle`
   - `Icons.Default.BookmarkAdd`/`Icons.Default.BookmarkAdded` → `Icons.Outlined.BookmarkAdd`/`Icons.Outlined.BookmarkAdded`
   - `Icons.Default.Schedule` → `Icons.Outlined.Schedule` or `Icons.Outlined.Circle`
   - Bottom nav: Use outlined style icons for consistency.
3. For `EmptyState` component, consider adding a subtle background tint or elevation.

---

## Task 8: Settings — Remove "Cooking History" Separate Section

### Description
The "Cooking History" section card in Settings should be removed. Only keep the reset cooldowns button with the `settings_reset_cooldowns` string resource, placed more compactly (e.g., as a simple button row or within another card).

### Files to Modify
- `app/src/main/java/com/sranker/mealmate/ui/screens/SettingsScreen.kt`

### Steps
1. Remove the entire "Főzési előzmények" (`Cooking History`) Card block (lines ~342-373).
2. Move the reset cooldowns button to a more compact location — either:
   - Place it as a standalone `OutlinedButton` below the cooldown card, or
   - Add it as a row within the cooldown Card itself.
3. Use the `stringResource(R.string.settings_reset_cooldowns)` for the button text.
4. Remove any hardcoded "Főzési előzmények" text references.

---

## Task 9: Hardcoded Strings → strings.xml

### Description
Replace ALL hardcoded Hungarian strings in composable files with references to string resources.

### Files to Modify
- All screen composables in `app/src/main/java/com/sranker/mealmate/ui/screens/*.kt`
- `app/src/main/java/com/sranker/mealmate/navigation/MainScaffold.kt`
- All ViewModel files that contain hardcoded strings
- Add missing string resources to `strings.xml`

### Known Hardcoded Strings to Fix
- `PlannerScreen.kt`: "Heti Tervező", "Szűrők", "Kitűzött ételek", "Menü elfogadása", "Menü befejezése", "Kihagyás", "Kitűzés", "Ajánlás", "Véletlenszerű", "Nincs megjeleníthető ajánlás", "Ajánlás keresése…", "Szűrés címkék szerint", "Törlés" (filter clear), "Nincs elérhető étel az adatbázisban" etc.
- `MealListScreen.kt`: "Ételek", "Címkék kezelése", "Új étel", "Keresés…", "Nincs még étel hozzáadva", snackbar messages
- `MealDetailScreen.kt`: "Étel Részletei", "Szerkesztés", "Törlés", "Betöltés…", "Hiba történt", "Főzések", "Kihagyások", "Adag", "Címkék", "Forrás", "Recept", "Hozzávalók", "Nincs recept megadva", "Nincsenek hozzávalók", "Étel nem található"
- `MealEditScreen.kt`: "Vissza", "Étel szerkesztése", "Új étel", "Mentés", "Név", "Pl. Csirkepaprikás", "Címkék", "Recept", "Elkészítési útmutató…", "Adag", "Fő", "Forrás", "https://…", "Hozzávalók", "Pl. 200g liszt", "Eltávolítás", "Hozzávaló hozzáadása", "Még nincsenek címkék. Hozz létre címkéket a Címkék oldalon."
- `SettingsScreen.kt`: "Vissza", "Beállítások", "Ajánlási szünet (menük száma)", "Ennyi menünek kell eltelnie…", "Sötét téma", "Betűméret", "Kicsi", "Közepes", "Nagy", "Akcentus szín", "Főzési előzmények", "Előzmények törlése", "Elfelejted az összes étel főzési előzményét?", "Igen", "Mégse", "Importálás", "Exportálás"
- `MenuHistoryScreen.kt`: "Vissza", "Előzmények", "Még nincs befejezett menü"
- `MenuHistoryDetailScreen.kt`: "Vissza", "Menü Részletei", "Átnevezés", "Betöltés...", "A menü nem található", "Menü átnevezése", "Menü címe", "Mentés", "Mégse", "étel"
- `TagManageScreen.kt`: (if applicable)
- `MainScaffold.kt`: "Tervező", "Ételek", "Előzmények", "Beállítások"
- `PlannerViewModel.kt`: "Nincs elérhető étel a megadott szűrőkkel"

### Steps
1. Add all missing string keys to `strings.xml`.
2. Replace every hardcoded string in composables with `stringResource(R.string.key_name)`.
3. Update ViewModel hardcoded strings to use string resources (or emit resource IDs).
4. Verify no hardcoded user-facing strings remain.

---

## Task 10: Prevent Re-recommending Pinned / Already Recommended Foods

### Description
The recommendation engine should not recommend a food that is already pinned in the active menu, or that has already been recommended (and not yet pinned/skipped) during the current planning session.

### Files to Modify
- `app/src/main/java/com/sranker/mealmate/ui/viewmodel/PlannerViewModel.kt`
- `app/src/main/java/com/sranker/mealmate/data/MealDao.kt` — Update the random selection queries to exclude meals already in the active menu.
- `app/src/main/java/com/sranker/mealmate/data/MealRepository.kt` — Update cooldown queries or add new ones.

### Steps
1. Add a new DAO query method (or parameter) that accepts a list of meal IDs to exclude:
   - `getRandomMealNotInCooldown(cooldown, currentIndex, excludeIds: List<Long>)`
   - `getRandomMealNotInCooldownByTags(cooldown, currentIndex, tagIds, excludeIds: List<Long>)`
2. Update the SQL in `MealDao` to add `AND meals.id NOT IN (:excludeIds)`.
3. In `PlannerViewModel.recommendMeal()`, collect the list of already-pinned meal IDs from `state.activeMenu.meals` and the current `lastRecommendedMealId`, pass them as exclusions.
4. Update `MealRepository` to pass through the exclusion list.

---

## Task 11: Single Recommendation Button (Remove Random Button)

### Description
In the planner, remove the separate "Véletlenszerű" (Random) button. Keep only the "Ajánlás" (Recommend) button. The recommendation itself should still be random.

### Files to Modify
- `app/src/main/java/com/sranker/mealmate/ui/screens/PlannerScreen.kt` — `RecommendationCard` composable

### Steps
1. In `RecommendationCard`, when `recommendedMeal == null`:
   - Remove the second "Véletlenszerű" (Shuffle icon) button entirely.
   - Keep only the "Ajánlás" (AutoAwesome icon) button.
   - Both previously called `onRecommend`, so no functionality change.

---

## Task 12: Show Recommended Food in Pinned List Area (Not Yet Pinned)

### Description
The recommended food should appear in the pinned meals list area with a visual indication that it's not yet pinned. It should use the same card style as other pinned items but with a pin button to actually pin it.

### Files to Modify
- `app/src/main/java/com/sranker/mealmate/ui/screens/PlannerScreen.kt`
- `app/src/main/java/com/sranker/mealmate/ui/viewmodel/PlannerViewModel.kt`

### Steps
1. In `PlannerViewModel.PlannerUiState`, ensure `recommendedMeal` is represented as an item in the pinned meals list (temporarily, until pinned or skipped).
2. In `PlannerScreen`, render the recommended meal as a card in the pinned meals area:
   - Show it at the top of the list with a distinct visual (e.g., dashed border or "Ajánlott" label).
   - Show its name, tags, and serving size.
   - Include "Pin" and "Skip" action buttons within the card.
   - Remove the separate recommendation card section.
3. After pinning, the card transitions to a normal pinned meal card.
4. After skipping, the card disappears and a new recommendation appears.

---

## Task 13: Remove "No Displayable Recommendation" Block

### Description
The block containing the "Nincs megjeleníthető ajánlás" (No displayable recommendation) text and buttons should be removed when there's no recommendation available. Instead, just show nothing (or keep the pinned meals list).

### Files to Modify
- `app/src/main/java/com/sranker/mealmate/ui/screens/PlannerScreen.kt` — The `else` branch in `RecommendationCard`

### Steps
1. In `RecommendationCard`, when `recommendedMeal == null`, instead of showing the placeholder icon, "Nincs megjeleníthető ajánlás" text, "Ajánlás" button, and "Véletlenszerű" button:
   - Show nothing (or just the recommend button if appropriate).
2. If there's no active recommendation and no pinned meals, show only the recommend button at the top of the pinned area.
3. Remove the restaurant icon placeholder and "Nincs megjeleníthető ajánlás" text.

---

## Task 14: Hide Recommendation Button After Menu Finalized

### Description
Once the menu has been accepted/pinned (isAccepted = true), the recommendation button should no longer be visible.

### Files to Modify
- `app/src/main/java/com/sranker/mealmate/ui/screens/PlannerScreen.kt`

### Steps
1. In the planner layout, conditionally hide the recommendation section (including the recommend button) when `state.isAccepted == true`.
2. Only show the pinned meals list, completion checkboxes, and the "Finish Menu" button after acceptance.

---

## Task 15: Prevent Menu Acceptance Until Current Recommendation Is Handled

### Description
The menu should not be pinnable (accept button should be disabled/hidden) until the user has either skipped or pinned the currently recommended food.

### Files to Modify
- `app/src/main/java/com/sranker/mealmate/ui/screens/PlannerScreen.kt`
- `app/src/main/java/com/sranker/mealmate/ui/viewmodel/PlannerViewModel.kt`

### Steps
1. In `PlannerUiState`, add a flag `hasOutstandingRecommendation` (true when `recommendedMeal != null`).
2. In the UI, disable or hide the "Accept Menu" button when `hasOutstandingRecommendation == true`.
3. After pinning or skipping the recommended meal, re-enable the accept button (if there are pinned meals).

---

## Task 16: Post-Acceptance Selection/Deselection and Food Detail Navigation

### Description
After the menu is accepted (pinned), foods in the pinned list should still be selectable/deselectable via checkbox. Additionally, tapping the food item itself (not the checkbox) should open the food details page.

### Files to Modify
- `app/src/main/java/com/sranker/mealmate/ui/screens/PlannerScreen.kt` — `PinnedMealCard` and `PinnedMealCardContent`
- `app/src/main/java/com/sranker/mealmate/navigation/AppNavGraph.kt` — Pass meal click handler to `PlannerScreen`

### Steps
1. Add an `onMealClick` parameter to `PlannerScreen` (with mealId: Long).
2. In `PinnedMealCardContent`, add a `clickable` modifier on the card that calls `onMealClick`.
3. In `AppNavGraph`, pass `onNavigateToMealDetail = { mealId -> navController.navigate(Routes.mealDetail(mealId)) }` to `PlannerScreen`.
4. Keep the checkbox for completion toggling (isAccepted state).
5. Ensure the card is clickable regardless of acceptance state.

---

## Task 17: History View — Open Food Details

### Description
In the menu history detail screen, foods listed should be clickable to open the food details page, just like in the meal list.

### Files to Modify
- `app/src/main/java/com/sranker/mealmate/ui/screens/MenuHistoryDetailScreen.kt`
- `app/src/main/java/com/sranker/mealmate/navigation/AppNavGraph.kt` — Pass meal click to `MenuHistoryDetailScreen`

### Steps
1. Add an `onMealClick: (Long) -> Unit` parameter to `MenuHistoryDetailScreen`.
2. In the `MenuContent` composable, make each meal card clickable (add `onClick = { onMealClick(meal.id) }`).
3. In `AppNavGraph`, pass `onMealClick = { mealId -> navController.navigate(Routes.mealDetail(mealId)) }`.
4. Verify the back navigation works (meal detail → history detail → history list).

---

## Summary of All Tasks

| #  | Task                                      | Priority | Dependencies                                |
|----|-------------------------------------------|----------|---------------------------------------------|
| 1  | English localization + language setting   | High     | None                                        |
| 2  | Food list toggle remove from plan         | High     | None                                        |
| 3  | Fix tag selection in add food             | High     | None                                        |
| 4  | Reorder fields in meal edit               | Medium   | None                                        |
| 5  | Add ingredient validation                 | Medium   | Task 4 (optional, to avoid merge conflicts) |
| 6  | Undo delete food                          | Medium   | None                                        |
| 7  | Modernize icons                           | Low      | None                                        |
| 8  | Settings cooking history cleanup          | Medium   | Task 1 (strings)                            |
| 9  | Hardcoded strings → strings.xml           | High     | Task 1                                      |
| 10 | Prevent re-recommending pinned foods      | Medium   | None                                        |
| 11 | Single recommendation button              | Medium   | Tasks 12-15 (related planner changes)       |
| 12 | Recommended food in pinned list           | High     | Tasks 11, 13, 14, 15                        |
| 13 | Remove "no recommendation" block          | Medium   | Task 11                                     |
| 14 | Hide recommendation after finalized       | Medium   | Task 12                                     |
| 15 | Block accept until recommendation handled | Medium   | Task 12                                     |
| 16 | Post-acceptance navigation to detail      | Medium   | None                                        |
| 17 | History food details navigation           | Medium   | None                                        |


---

## Implementation Progress

| # | Task | Status | Notes |
|---|------|--------|-------|
| 1 | English localization + language setting | ✅ **95%** | English strings created, SettingsRepository+SettingsViewModel done, SettingsScreen language picker done, MainActivity locale switching done. PlannerScreen now uses stringResources |
| 2 | Food list toggle remove from plan | ✅ **Done** | MealListViewModel.addToActivePlan now removes instead of showing "already in plan" |
| 3 | Fix tag selection in add food | ✅ **Done** | MealEditViewModel init properly loads allTags from flow |
| 4 | Reorder fields in meal edit | ✅ **Done** | New order: Name → Recipe → Servings → Ingredients → Tags → Source |
| 5 | Add ingredient validation | ✅ **Done** | validate() checks for non-blank ingredient. ingredientsError added to MealEditUiState |
| 6 | Undo delete food | ✅ **Done** | Snackbar with undo action on meal delete |
| 7 | Modernize icons | ⬜ **Not started** | Low priority - cosmetic only |
| 8 | Settings cooking history cleanup | ✅ **Done** | Compact button replacing separate card |
| 9 | Hardcoded strings → strings.xml | ✅ **95%** | All screen files use stringResource. ViewModels still have a few hardcoded error strings |
| 10 | Prevent re-recommending pinned foods | ✅ **Done** | MealDao + MealRepository + PlannerViewModel updated with excludeIds |
| 11 | Single recommendation button | ✅ **Done** | Removed random button, only recommend button remains |
| 12 | Recommended food in pinned list | ✅ **Done** | RecommendedMealCard shown in pinned meals area with distinct look |
| 13 | Remove "no recommendation" block | ✅ **Done** | No placeholder UI when no recommendation available; only recommend button shown |
| 14 | Hide recommendation after finalized | ✅ **Done** | Recommend button hidden when isAccepted=true |
| 15 | Block accept until recommendation handled | ✅ **Done** | hasOutstandingRecommendation flag blocks accept button until pin/skip |
| 16 | Post-acceptance navigation to detail | ✅ **Done** | PinnedMealCard clickable, onMealClick passed from PlannerScreen |
| 17 | History food details navigation | ✅ **Done** | MenuHistoryDetailScreen meals clickable with onMealClick |
