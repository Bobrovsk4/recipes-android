package com.example.recipes.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recipes.data.ApiClient
import com.example.recipes.data.Recipe
import com.example.recipes.data.toUserMessage
import kotlinx.coroutines.launch
import retrofit2.HttpException

class RandomViewModel : ViewModel() {

    var recipe by mutableStateOf<Recipe?>(null)
        private set
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    fun roll() {
        if (loading) return
        viewModelScope.launch {
            loading = true
            error = null
            try {
                val d = ApiClient.api.getRandomRecipe()
                recipe = Recipe(d.id, d.name, d.type ?: "")
            } catch (e: Exception) {
                // Сервер отвечает 404, когда в базе нет ни одного рецепта
                error = if (e is HttpException && e.code() == 404) {
                    "Рецептов пока нет"
                } else {
                    e.toUserMessage()
                }
            }
            loading = false
        }
    }
}