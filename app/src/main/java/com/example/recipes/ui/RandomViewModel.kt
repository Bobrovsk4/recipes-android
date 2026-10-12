package com.example.recipes.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recipes.data.ApiClient
import com.example.recipes.data.DaytimeType
import com.example.recipes.data.Recipe
import com.example.recipes.data.RecipeDetails
import com.example.recipes.data.Type
import com.example.recipes.data.toUserMessage
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import retrofit2.HttpException

private class NoRecipesException : Exception()

private fun RecipeDetails.toRecipe() = Recipe(id, name, type ?: "")

class RandomViewModel : ViewModel() {

    var recipe by mutableStateOf<Recipe?>(null)
        private set
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    var types by mutableStateOf<List<Type>>(emptyList())
        private set
    var daytimeTypes by mutableStateOf<List<DaytimeType>>(emptyList())
        private set

    var typeFilter by mutableStateOf<Int?>(null)
        private set
    var daytimeFilter by mutableStateOf<Int?>(null)
        private set

    init {
        loadFilters()
    }

    private fun loadFilters() {
        viewModelScope.launch {
            try {
                coroutineScope {
                    val t = async { ApiClient.api.getTypes() }
                    val d = async { ApiClient.api.getDaytimeTypes() }
                    types = t.await()
                    daytimeTypes = d.await()
                }
            } catch (e: Exception) {
                error = e.toUserMessage()
            }
        }
    }

    fun toggleType(id: Int) {
        typeFilter = if (typeFilter == id) null else id
    }

    fun toggleDaytime(id: Int) {
        daytimeFilter = if (daytimeFilter == id) null else id
    }

    fun clearFilters() {
        typeFilter = null
        daytimeFilter = null
    }

    fun roll() {
        if (loading) return
        viewModelScope.launch {
            loading = true
            error = null
            try {
                val t = typeFilter
                val d = daytimeFilter
                val previousId = recipe?.id
                var next = fetch(t, d)
                if (next.id == previousId) next = fetch(t, d)
                recipe = next
            } catch (e: Exception) {
            val empty = e is HttpException && e.code() == 404
            error = when {
                !empty -> e.toUserMessage()
                typeFilter != null || daytimeFilter != null ->
                    "В выбранной категории пока нет рецептов"
                else -> "Рецептов пока нет"
            }
        }
            loading = false
        }
    }

    private suspend fun fetch(t: Int?, d: Int?): Recipe =
        ApiClient.api.getRandomRecipe(t, d).toRecipe()
}