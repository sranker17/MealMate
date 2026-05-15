package com.sranker.mealmate.data

import com.sranker.mealmate.util.formatDateTitle
import kotlinx.coroutines.flow.Flow
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository that manages the menu planning lifecycle.
 *
 * Responsibilities include:
 * - Creating and retrieving the active (in-progress) menu.
 * - Pin / unpin meals within the active menu.
 * - Accept (lock) and finish (archive) a menu.
 * - Tracking completed menu count and updating meal cooldown indices.
 */
@Singleton
class MenuRepository @Inject constructor(
    private val menuDao: MenuDao,
    private val mealRepository: MealRepository
) {

    /** Observe the active (non-completed) menu with its meals. */
    fun getActiveMenuWithMealsFlow(): Flow<MenuWithMeals?> = menuDao.getActiveMenuWithMealsFlow()

    /** Get the active menu with meals once. */
    suspend fun getActiveMenuWithMeals(): MenuWithMeals? = menuDao.getActiveMenuWithMeals()

    /**
     * Returns the existing active menu, or creates a new one with a date-based title
     * if none exists.
     */
    suspend fun getOrCreateActiveMenu(): MenuEntity {
        val existing = menuDao.getActiveMenu()
        if (existing != null) return existing

        val dateTitle = formatDateTitle(Date())
        val newMenu = MenuEntity(title = dateTitle)
        val id = menuDao.insert(newMenu)
        return newMenu.copy(id = id)
    }

    /**
     * Pin a meal to the active menu. Creates the active menu first if needed.
     *
     * @param mealId The meal to pin.
     */
    suspend fun pinMealToActiveMenu(mealId: Long) {
        val menu = getOrCreateActiveMenu()
        val existing = menuDao.getMenuMealCrossRef(menu.id, mealId)
        if (existing == null) {
            menuDao.insertMenuMealCrossRef(
                MenuMealCrossRef(menuId = menu.id, mealId = mealId, isPinned = true)
            )
        } else if (!existing.isPinned) {
            menuDao.updateMenuMealCrossRef(existing.copy(isPinned = true))
        }
    }

    /**
     * Unpin a meal from the active menu (removes the cross-ref row entirely).
     */
    suspend fun unpinMealFromActiveMenu(mealId: Long) {
        val menu = menuDao.getActiveMenu() ?: return
        menuDao.removeMealFromMenu(menu.id, mealId)
    }

    /**
     * Accept (lock) the active menu.
     * All currently pinned meals become fixed — no further edits allowed.
     * If no meals are pinned, this is a no-op.
     */
    suspend fun acceptMenu(): Boolean {
        val menu = menuDao.getActiveMenu() ?: return false
        val crossRefs = menuDao.getMenuMealCrossRefsForMenu(menu.id)
        if (crossRefs.isEmpty()) return false
        menuDao.acceptActiveMenu()
        return true
    }

    /**
     * Returns whether the active menu has been accepted (locked).
     */
    suspend fun isActiveMenuAccepted(): Boolean {
        val menu = menuDao.getActiveMenu() ?: return false
        return menu.isAccepted
    }

    /**
     * Finish (complete) the active menu.
     *
     * - Marks all unpinned meals as skipped.
     * - Increments each pinned meal's [timesCooked] and sets [lastCompletedMenuIndex].
     * - Archives the menu with a sequential [completionIndex].
     *
     * @return The completion index assigned to the menu, or -1 if no active menu exists.
     */
    suspend fun finishMenu(): Int {
        val menu = menuDao.getActiveMenu() ?: return -1
        val crossRefs = menuDao.getMenuMealCrossRefsForMenu(menu.id)

        val nextIndex = (menuDao.getMaxCompletionIndex() ?: 0) + 1

        crossRefs.forEach { ref ->
            val mealId = ref.mealId
            if (ref.isCompleted) {
                // Meal was cooked, record it
                mealRepository.recordMealCooked(mealId, nextIndex)
            } else {
                // Meal was pinned but not completed, skip it
                mealRepository.recordMealSkipped(mealId)
            }
        }

        menuDao.completeActiveMenu(nextIndex)
        return nextIndex
    }

    /**
     * Toggle a meal's completed state in the active menu.
     * If already completed, unmark it; otherwise mark it as completed.
     */
    suspend fun markMealCompleted(mealId: Long) {
        val menu = menuDao.getActiveMenu() ?: return
        val existing = menuDao.getMenuMealCrossRef(menu.id, mealId) ?: return
        if (existing.isCompleted) {
            menuDao.unmarkMealCompleted(menu.id, mealId)
        } else {
            menuDao.markMealCompleted(menu.id, mealId)
        }
    }

    /** Observe completed menus with their meals. */
    fun getCompletedMenusWithMealsFlow(): Flow<List<MenuWithMeals>> =
        menuDao.getCompletedMenusWithMealsFlow()

    /** Get all completed menus once. */
    suspend fun getCompletedMenusOnce(): List<MenuEntity> = menuDao.getCompletedMenusOnce()

    /** Get a specific menu with meals by ID. */
    suspend fun getMenuWithMeals(menuId: Long): MenuWithMeals? = menuDao.getMenuWithMeals(menuId)

    /**
     * Rename a menu (used for giving custom titles to past menus).
     *
     * @param menuId The ID of the menu to rename.
     * @param newTitle The new title for the menu.
     */
    suspend fun renameMenu(menuId: Long, newTitle: String) {
        menuDao.updateMenuTitle(menuId, newTitle)
    }

    /**
     * Returns the [MenuMealCrossRef] rows for the currently active menu,
     * or an empty list if no active menu exists.
     */
    suspend fun getActiveMenuCrossRefs(): List<MenuMealCrossRef> {
        val menu = menuDao.getActiveMenu() ?: return emptyList()
        return menuDao.getMenuMealCrossRefsForMenu(menu.id)
    }

    /**
     * Returns the highest completion index among completed menus,
     * or 0 if no menus have been completed.
     */
    suspend fun getCurrentCompletionIndex(): Int = menuDao.getMaxCompletionIndex() ?: 0

    /**
     * Add a meal directly to the active menu (pinned) without going through
     * the recommendation engine. Creates the active menu first if needed.
     * Guards against duplicate entries for the same meal in the same menu.
     *
     * @param mealId The meal to add.
     */
    suspend fun addMealToActiveMenu(mealId: Long) {
        val menu = getOrCreateActiveMenu()
        val existing = menuDao.getMenuMealCrossRef(menu.id, mealId)
        if (existing == null) {
            menuDao.insertMenuMealCrossRef(
                MenuMealCrossRef(menuId = menu.id, mealId = mealId, isPinned = true)
            )
        }
    }
}
