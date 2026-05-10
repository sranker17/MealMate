# MealMate — Project Plan

## Concept Overview
A Hungarian-language Android application designed for 6-inch phones and 12-inch tablets. The app helps users plan their weekly meals by recommending recipes one-by-one. Users can pin recommended meals, group them, accept the group as the current plan, and later mark them as completed. Completed menus are archived, and the meals within them are placed on a configurable "cooldown" so they don't appear in recommendations for a certain number of subsequent menu plans.

## Feature Set
- **Meal Management**: Add, edit, view, and list meals.
- **Meal Properties**: Name, Recipe, Ingredients (list of items), Tags, Times Cooked, Times Skipped, Serving Size, Source Link.
- **Meal Tags**: Predefined but user-expandable tags for flexible filtering (e.g., pasta, stew, healthy). A meal can have multiple tags.
- **Recommendation Engine**: Randomly suggests one unpinned meal at a time, respecting user-defined filters (e.g., ingredient or type) and cooldown rules.
- **Menu Planning**: Pin recommended meals, accept the group to form a planned menu, mark meals as completed.
- **Menu History**: View past completed menus (date-based, renameable).
- **Cooldown System**: Configurable "rotation count". A meal cooked in menu $N$ will not be recommended again until menu $N + Cooldown$ is completed.
- **Import/Export**: Bulk import and export of recipes in JSON format.
- **Settings**: Theme, text size, cooldown parameter, reset cooldowns, import/export.
- **UI/UX**: Minimalist, clean design ported from `ShoppingListManager` (dark background, distinct accent colors). Responsive layouts for both phones and tablets.

## Data Models (Proposed)
- `Meal`: id, name, recipe, timesCooked, timesSkipped, servingSize, sourceUrl, lastCompletedMenuIndex.
- `Ingredient`: id, mealId, name.
- `Tag`: id, name.
- `MealTagCrossRef`: mealId, tagId.
- `Menu`: id, title (date-based default), isCompleted, completionIndex.
- `MenuMealCrossRef`: menuId, mealId, isPinned, isCompleted.
- `Settings`: DataStore (cooldownWeeks, completedMenuCount, theme, fontSize, etc.).

---

## Agent Tasks

The following tasks are broken down to be fully separable and LLM-digestible.

### Task 1 — Project Setup & Architecture
**Context**: Initialize the Android project with necessary dependencies and structural layers.
**Deliverables**:
- Build configuration (`build.gradle.kts`) with Compose, Room, Hilt, Navigation, and DataStore.
- `MainActivity.kt` and `MealMateApp.kt` (Hilt application class).
- Dependency injection modules stubbed out.
- Base package structure (`data`, `ui`, `di`, `domain`, `util`).
**Acceptance Criteria**: Project compiles successfully.

### Task 2 — Data Layer: Room Entities & DAOs
**Context**: Define the SQLite database schema and data access objects.
**Deliverables**:
- Entities: `MealEntity`, `IngredientEntity`, `TagEntity`, `MealTagCrossRef`, `MenuEntity`, `MenuMealCrossRef`.
- DAOs: `MealDao` (CRUD, plus queries for filtered/unblocked random selection), `IngredientDao`, `TagDao`, `MenuDao` (active menu, history menus).
- Room Database class `MealMateDatabase`.
**Acceptance Criteria**: DAOs compile and schema can be exported. Proper relationships are defined (e.g., One-to-Many for Types to Meals, Many-to-Many for Menus to Meals).

### Task 3 — Data Layer: Repositories & Import/Export
**Context**: Implement repository interfaces for data access and JSON import/export logic.
**Deliverables**:
- `MealRepository`: Manage meals, types, and stats (times cooked/skipped).
- `MenuRepository`: Manage active planning, pinning, accepting, and completing menus. Handles the `completedMenuCount` logic.
- `SettingsRepository`: DataStore for app settings (cooldown parameter, theme, etc.).
- `BackupRepository`: Logic to serialize/deserialize all meals to/from JSON.
**Acceptance Criteria**: Repositories abstract Room and DataStore. JSON backup logic works and handles conflicts/duplicates gracefully.

### Task 4 — UI Foundation & Theming
**Context**: Port the visual style from `ShoppingListManager` and setup localization.
**Deliverables**:
- `Color.kt`, `Theme.kt`, `Type.kt` using the `ShoppingListManager` porting guide (docs/ui_porting_guide.md) (Outfit font, minimalist dark themes, accent colors).
- Common UI components: `CustomHeader`, `EmptyState`, custom buttons, and text fields.
- `strings.xml` (Hungarian primary language).
**Acceptance Criteria**: Compose Previews of common components match the minimalist design system.

### Task 5 — ViewModel: Meal Management
**Context**: Business logic for viewing, adding, and editing meals and types.
**Deliverables**:
- `MealListViewModel`: Fetching all meals, searching, filtering by tags.
- `MealDetailViewModel`: Viewing meal stats.
- `MealEditViewModel`: Creating/Editing a meal, its individual ingredients, and assigning tags.
- `TagManageViewModel`: Adding/Managing predefined tags.
**Acceptance Criteria**: ViewModels expose StateFlows. No UI imports. Logic handles data validation.

### Task 6 — ViewModel: Menu Planning & Engine
**Context**: The core recommendation and planning logic.
**Deliverables**:
- `PlannerViewModel`: 
  - Tracks active unpinned recommendation.
  - Handles "Recommend Meal" (fetches random meal not in cooldown, matching active filters).
  - Handles "Pin", "Unpin", "Skip" (increments skipped stat instantly when recommending next).
  - Handles "Accept Menu" (locks the group strictly; cannot be edited after acceptance).
  - Tracks completed meals within the accepted menu.
  - Handles "Finish Menu" (increments global `completedMenuCount`, updates meals' `lastCompletedMenuIndex`, archives menu).
**Acceptance Criteria**: Recommendation engine correctly respects the cooldown parameter and active filters.

### Task 7 — UI: Meal Management Screens
**Context**: Screens to manage the meal database.
**Deliverables**:
- `MealListScreen`: Search bar, filter chips, list of meals.
- `MealDetailScreen`: Displays all properties, stats, clickable source link, edit button.
- `MealEditScreen`: Form for meal creation/editing (Name, Recipe, dynamic list of individual Ingredients, Tag selection, Serving Size, Link).
  - `TagManageScreen`: Simple list with add functionality for predefined tags.
**Acceptance Criteria**: Screens are responsive (adapt to Tablet/Phone). Form validation works.

### Task 8 — UI: Menu Planning Screen (Main Screen)
**Context**: The primary interactive screen of the app.
**Deliverables**:
- `PlannerScreen`:
  - Empty state: If the database is empty, visually guide the user to navigate to the "Add Meal" screen to get started.
  - Filter trigger (opens bottom sheet or dialog).
  - Large card displaying the currently recommended meal (if any).
  - Buttons: "Pin", "Skip", "Recommend".
  - List of currently pinned meals (swipe to unpin if not yet accepted).
  - "Accept Menu" button (becomes visible/active when meals are pinned).
  - Once accepted: List changes to show checkboxes. "Finish Menu" button appears when all checked.
**Acceptance Criteria**: Smooth animations when recommending or pinning meals. Tablet layout optimizes screen real estate (e.g., recommendation card on left, pinned list on right).

### Task 9 — UI: History & Settings Screens
**Context**: Archival and configuration interfaces.
**Deliverables**:
- `MenuHistoryScreen`: List of past menus.
- `MenuHistoryDetailScreen`: Read-only view of a past menu.
- `SettingsScreen`: 
  - Cooldown weeks stepper/input.
  - Reset cooldowns button.
  - Import/Export JSON buttons.
  - Theme and font size selectors.
**Acceptance Criteria**: Settings changes reflect immediately. Export/Import triggers appropriate system file pickers.

### Task 10 — Navigation & App Assembly
**Context**: Tie everything together with Navigation Compose.
**Deliverables**:
- `AppNavGraph.kt`: Define all routes.
- `MainScaffold.kt`: Bottom navigation or Navigation Rail (for tablets).
- Tie ViewModels to Screens.
**Acceptance Criteria**: App is fully navigable. Tablet users see a Nav Rail, Phone users see a Bottom Bar.

### Task 11 — Polish & Polish
**Context**: Final touch-ups, animations, and app icon.
**Deliverables**:
- Add screen transition animations.
- Implement the App Icon (dark background, food-related minimalist logo).
- Verify all Hungarian translations are natural and accurate.
- Unit testing core logic (especially the recommendation engine and cooldown logic).
**Acceptance Criteria**: App feels premium and responsive. Tests pass.
