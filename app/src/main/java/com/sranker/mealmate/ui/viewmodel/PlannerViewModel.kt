package com.sranker.mealmate.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sranker.mealmate.data.MealRepository
import com.sranker.mealmate.data.MealWithTags
import com.sranker.mealmate.data.MenuMealCrossRef
import com.sranker.mealmate.data.MenuRepository
import com.sranker.mealmate.data.MenuWithMeals
import com.sranker.mealmate.data.SettingsRepository
import com.sranker.mealmate.data.TagEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * One-shot UI events emitted by [PlannerViewModel].
 */
sealed interface PlannerEvent {
    data class MealUnpinned(val mealId: Long) : PlannerEvent
    data object NoRecommendationAvailable : PlannerEvent
}

/**
 * UI state for the planner screen — the main interactive screen of the app.
 *
 * @property recommendedMeal The currently recommended meal with its tags, or null.
 * @property isRecommending Whether a recommendation is being fetched.
 * @property activeMenu The active (non-completed) menu with its meals, or null.
 * @property activeMenuCrossRefs Pivot state for each meal in the active menu.
 * @property isAccepted Whether the active menu has been accepted (locked).
 * @property selectedTagIds Tag IDs selected as filters for the recommendation engine.
 * @property allTags All available tags for filter selection.
 * @property cooldown The current cooldown parameter from settings.
 * @property errorMessage An error or informational message.
 * @property errorMessageResId An error or informational message resource ID.
 */
data class PlannerUiState(
    val recommendedMeal: MealWithTags? = null,
    val isRecommending: Boolean = false,
    val activeMenu: MenuWithMeals? = null,
    val activeMenuCrossRefs: List<MenuMealCrossRef> = emptyList(),
    val isAccepted: Boolean = false,
    val selectedTagIds: Set<Long> = emptySet(),
    val allTags: List<TagEntity> = emptyList(),
    val cooldown: Int = 3,
    val errorMessage: String? = null,
    val errorMessageResId: Int? = null
) {
    /** True when there is a recommended meal that hasn't been pinned or skipped yet. */
    val hasOutstandingRecommendation: Boolean get() = recommendedMeal != null
}

/**
 * ViewModel for the planner (main) screen.
 *
 * Manages the recommendation engine, pinning/skipping, menu acceptance,
 * and completion tracking.
 *
 * The recommendation engine:
 * - Fetches a random meal that is not in cooldown.
 * - Respects active [selectedTagIds] filters (if any).
 * - When a meal is skipped, increments its [timesSkipped] stat.
 * - When a meal is pinned, adds it to the active menu.
 * - Excludes skipped meals until the menu cycle is complete.
 */
@HiltViewModel
class PlannerViewModel @Inject constructor(
    private val menuRepository: MenuRepository,
    private val mealRepository: MealRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlannerUiState())
    val uiState: StateFlow<PlannerUiState> = _uiState.asStateFlow()

    /** All available tags for filter selection. */
    val allTags: StateFlow<List<TagEntity>> = mealRepository.getAllTags()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** The last recommended meal ID, used to record skipped stats when replacing. */
    private var lastRecommendedMealId: Long? = null

    /** Set of meal IDs skipped in the current session (cleared on finish). */
    private var skippedMealIds: Set<Long> = emptySet()

    /** One-shot UI events (snackbar). */
    private val _events = MutableSharedFlow<PlannerEvent>()
    val events: SharedFlow<PlannerEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            // Collect settings for cooldown
            settingsRepository.settings.collect { settings ->
                _uiState.value = _uiState.value.copy(cooldown = settings.cooldown)
            }
        }
        viewModelScope.launch {
            // Collect active menu changes reactively
            menuRepository.getActiveMenuWithMealsFlow().collect { menu ->
                val crossRefs = if (menu != null) {
                    menuRepository.getActiveMenuCrossRefs()
                } else {
                    emptyList()
                }
                _uiState.value = _uiState.value.copy(
                    activeMenu = menu,
                    activeMenuCrossRefs = crossRefs,
                    isAccepted = menu?.menu?.isAccepted ?: false
                )
            }
        }
        viewModelScope.launch {
            // Collect tags for filter display
            mealRepository.getAllTags().collect { tags ->
                _uiState.value = _uiState.value.copy(allTags = tags)
            }
        }
    }

    // region Tag Filters

    /** Toggle a tag filter on or off for the recommendation engine. */
    fun onTagFilterToggled(tagId: Long) {
        _uiState.value = _uiState.value.copy(
            selectedTagIds = _uiState.value.selectedTagIds.let { current ->
                if (tagId in current) current - tagId else current + tagId
            }
        )
    }

    /** Clear all tag filters. */
    fun onClearFilters() {
        _uiState.value = _uiState.value.copy(selectedTagIds = emptySet())
    }

    // endregion

    // region Recommendation Engine

    /**
     * Fetch a new random meal recommendation respecting cooldown and active filters.
     *
     * If there was a previous recommendation that was not pinned, its [timesSkipped]
     * counter is incremented automatically.
     */
    fun recommendMeal() {
        val state = _uiState.value

        viewModelScope.launch {
            _uiState.value = state.copy(isRecommending = true, errorMessage = null, errorMessageResId = null)

            // Record skip for the previously recommended meal if it wasn't pinned
            val previousId = lastRecommendedMealId
            if (previousId != null) {
                val isPinned = state.activeMenuCrossRefs.any { it.mealId == previousId && it.isPinned }
                if (!isPinned) {
                    mealRepository.recordMealSkipped(previousId)
                    // Track skipped meal so it won't be recommended again until menu cycle completes
                    skippedMealIds = skippedMealIds + previousId
                }
            }

            val cooldown = _uiState.value.cooldown
            val currentIndex = menuRepository.getCurrentCompletionIndex()
            val tagIds = _uiState.value.selectedTagIds.toList()

            // Collect IDs to exclude: pinned + last recommended (if pending) + skipped
            val excludeIds = buildSet {
                state.activeMenu?.meals?.forEach { add(it.id) }
                if (previousId != null && state.activeMenuCrossRefs.none { it.mealId == previousId && it.isPinned }) {
                    add(previousId)
                }
                addAll(skippedMealIds)
            }.toList()

            val meal = if (tagIds.isEmpty()) {
                mealRepository.getRandomMealNotInCooldown(cooldown, currentIndex, excludeIds)
            } else {
                mealRepository.getRandomMealNotInCooldownByTags(cooldown, currentIndex, tagIds, excludeIds)
            }

            if (meal != null) {
                val mealWithTags = mealRepository.getMealWithTags(meal.id)
                lastRecommendedMealId = meal.id
                _uiState.value = _uiState.value.copy(
                    recommendedMeal = mealWithTags,
                    isRecommending = false
                )
            } else {
                _events.emit(PlannerEvent.NoRecommendationAvailable)
                lastRecommendedMealId = null
                _uiState.value = _uiState.value.copy(
                    recommendedMeal = null,
                    isRecommending = false,
                    errorMessage = null,
                    errorMessageResId = null
                )
            }
        }
    }

    /**
     * Skip the current recommendation and fetch a new one.
     * The skipped meal's [timesSkipped] is incremented via [recommendMeal].
     */
    fun skipMeal() {
        recommendMeal()
    }

    // endregion

    // region Pin / Unpin

    /**
     * Pin the currently recommended meal to the active menu.
     */
    fun pinMeal() {
        val meal = _uiState.value.recommendedMeal ?: return
        viewModelScope.launch {
            menuRepository.pinMealToActiveMenu(meal.meal.id)
            _uiState.value = _uiState.value.copy(
                recommendedMeal = null,
                errorMessage = null
            )
            lastRecommendedMealId = null
        }
    }

    /**
     * Unpin a meal from the active menu.
     * Emits a [PlannerEvent.MealUnpinned] event for undo support.
     */
    fun unpinMeal(mealId: Long) {
        val state = _uiState.value
        val isPinned = state.activeMenuCrossRefs.any { it.mealId == mealId && it.isPinned }
        if (!isPinned) return

        viewModelScope.launch {
            menuRepository.unpinMealFromActiveMenu(mealId)
            _events.emit(PlannerEvent.MealUnpinned(mealId))
        }
    }

    /**
     * Undo the last unpin by re-pinning the meal to the active menu.
     */
    fun undoUnpinMeal(mealId: Long) {
        viewModelScope.launch {
            menuRepository.pinMealToActiveMenu(mealId)
        }
    }

    // endregion

    // region Menu Lifecycle

    /**
     * Accept (lock) the active menu so that meal completion tracking begins.
     * Clears any active tag filters.
     */
    fun acceptMenu() {
        viewModelScope.launch {
            val success = menuRepository.acceptMenu()
            if (success) {
                _uiState.value = _uiState.value.copy(
                    isAccepted = true,
                    selectedTagIds = emptySet()
                )
            }
        }
    }

    /**
     * Toggle a meal's completed state in the active menu.
     */
    fun onMealCompletedToggled(mealId: Long) {
        viewModelScope.launch {
            menuRepository.markMealCompleted(mealId)
        }
    }

    /**
     * Finish (archive) the active menu and create a fresh active menu for the next planning cycle.
     *
     * After finishing, the recommended meal is cleared, the menu resets, and skipped meals
     * are cleared so they can be recommended again in future cycles.
     */
    fun finishMenu() {
        viewModelScope.launch {
            val index = menuRepository.finishMenu()
            if (index != -1) {
                lastRecommendedMealId = null
                skippedMealIds = emptySet()
                _uiState.value = _uiState.value.copy(
                    recommendedMeal = null,
                    isAccepted = false
                )
            }
        }
    }

    // endregion
}
