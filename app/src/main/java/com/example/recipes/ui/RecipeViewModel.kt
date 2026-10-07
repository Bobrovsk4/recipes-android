package com.example.recipes.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recipes.data.ApiClient
import com.example.recipes.data.RecipeDetails
import kotlinx.coroutines.launch

sealed interface RecipeState {
    data object Loading : RecipeState
    data class Success(val recipe: RecipeDetails) : RecipeState
    data class Error(val message: String) : RecipeState
}

class RecipeViewModel : ViewModel() {

    var state by mutableStateOf<RecipeState>(RecipeState.Loading)
        private set

    private var loadedId: Int? = null

    fun load(id: Int) {
        loadedId = id
        state = RecipeState.Loading
        viewModelScope.launch {
            state = try {
                RecipeState.Success(ApiClient.api.getRecipe(id))
            } catch (e: Exception) {
                RecipeState.Error(e.message ?: "Не удалось загрузить рецепт")
            }
        }
    }

    fun loadIfNeeded(id: Int) {
        if (loadedId != id) load(id)
    }
}