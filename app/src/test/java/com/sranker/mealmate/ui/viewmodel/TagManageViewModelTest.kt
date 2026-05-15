package com.sranker.mealmate.ui.viewmodel

import com.google.common.truth.Truth.assertThat
import com.sranker.mealmate.data.MealRepository
import com.sranker.mealmate.data.TagEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [TagManageViewModel].
 *
 * Verifies tag CRUD operations and validation logic.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TagManageViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val mealRepository: MealRepository = mockk()

    private val existingTags = listOf(
        TagEntity(id = 1L, name = "Soup"),
        TagEntity(id = 2L, name = "Pasta")
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { mealRepository.getAllTags() } returns MutableStateFlow(existingTags)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // region Initial State

    @Test
    fun `initial state shows existing tags`() = runTest(testDispatcher) {
        val viewModel = TagManageViewModel(mealRepository)

        val state = viewModel.uiState.value
        assertThat(state.tags).hasSize(2)
        assertThat(state.tags.map { it.name }).containsExactly("Soup", "Pasta")
    }

    // endregion

    // region Add Tag

    @Test
    fun `addTag calls repository and clears input`() = runTest(testDispatcher) {
        coEvery { mealRepository.createTag("Salad") } returns 3L

        val viewModel = TagManageViewModel(mealRepository)
        viewModel.onNewTagNameChanged("Salad")
        viewModel.addTag()

        coVerify { mealRepository.createTag("Salad") }
        assertThat(viewModel.uiState.value.newTagName).isEmpty()
    }

    @Test
    fun `addTag rejects blank name`() = runTest(testDispatcher) {
        val viewModel = TagManageViewModel(mealRepository)
        viewModel.onNewTagNameChanged("   ")
        viewModel.addTag()

        coVerify(inverse = true) { mealRepository.createTag(any()) }
        assertThat(viewModel.uiState.value.errorMessage).isNull()
        assertThat(viewModel.uiState.value.errorMessageResId).isEqualTo(com.sranker.mealmate.R.string.tag_manage_name_required)
    }

    @Test
    fun `addTag rejects duplicate name case-insensitively`() = runTest(testDispatcher) {
        val viewModel = TagManageViewModel(mealRepository)
        viewModel.onNewTagNameChanged("soup")
        viewModel.addTag()

        coVerify(inverse = true) { mealRepository.createTag(any()) }
        assertThat(viewModel.uiState.value.errorMessage).isNull()
        assertThat(viewModel.uiState.value.errorMessageResId).isEqualTo(com.sranker.mealmate.R.string.tag_manage_duplicate_name)
    }

    @Test
    fun `addTag rejects exact duplicate name`() = runTest(testDispatcher) {
        val viewModel = TagManageViewModel(mealRepository)
        viewModel.onNewTagNameChanged("Pasta")
        viewModel.addTag()

        coVerify(inverse = true) { mealRepository.createTag(any()) }
        assertThat(viewModel.uiState.value.errorMessage).isNull()
        assertThat(viewModel.uiState.value.errorMessageResId).isEqualTo(com.sranker.mealmate.R.string.tag_manage_duplicate_name)
    }

    @Test
    fun `changing new tag name clears previous error`() = runTest(testDispatcher) {
        val viewModel = TagManageViewModel(mealRepository)
        viewModel.onNewTagNameChanged("   ")
        viewModel.addTag()
        assertThat(viewModel.uiState.value.errorMessageResId).isNotNull()

        viewModel.onNewTagNameChanged("Salad")
        assertThat(viewModel.uiState.value.errorMessage).isNull()
        assertThat(viewModel.uiState.value.errorMessageResId).isNull()
    }

    // endregion

    // region Delete Tag

    @Test
    fun `deleteTag calls repository`() = runTest(testDispatcher) {
        coEvery { mealRepository.deleteTag(any()) } returns Unit

        val viewModel = TagManageViewModel(mealRepository)
        val tagToDelete = existingTags[0]
        viewModel.deleteTag(tagToDelete)

        coVerify { mealRepository.deleteTag(tagToDelete) }
    }

    // endregion
}
