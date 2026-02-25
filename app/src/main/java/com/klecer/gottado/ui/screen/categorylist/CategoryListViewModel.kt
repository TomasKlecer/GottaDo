package com.klecer.gottado.ui.screen.categorylist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.klecer.gottado.data.db.entity.CategoryEntity
import com.klecer.gottado.domain.repository.CategoryRepository
import com.klecer.gottado.domain.usecase.GetCategoriesUseCase
import com.klecer.gottado.domain.usecase.SaveCategoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoryListViewModel @Inject constructor(
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val saveCategoryUseCase: SaveCategoryUseCase,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _categories = MutableStateFlow<List<CategoryEntity>>(emptyList())
    val categories: StateFlow<List<CategoryEntity>> = _categories.asStateFlow()

    private val _createdCategoryId = MutableStateFlow<Long?>(null)
    val createdCategoryId: StateFlow<Long?> = _createdCategoryId.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _categories.value = getCategoriesUseCase()
        }
    }

    fun addCategory(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val maxOrder = _categories.value.maxOfOrNull { it.sortOrder } ?: -1
            val newId = saveCategoryUseCase(
                CategoryEntity(
                    id = 0L,
                    name = name.trim(),
                    sortOrder = maxOrder + 1,
                    showCheckboxInsteadOfBullet = false,
                    tasksWithTimeFirst = true,
                    categoryType = "normal",
                    color = 0xFFFFFFFF.toInt()
                )
            )
            load()
            _createdCategoryId.value = newId
        }
    }

    fun clearCreatedCategoryId() {
        _createdCategoryId.value = null
    }
}
