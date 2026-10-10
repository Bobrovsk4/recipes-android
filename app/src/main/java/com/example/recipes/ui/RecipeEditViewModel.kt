package com.example.recipes.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recipes.data.ApiClient
import com.example.recipes.data.DaytimeType
import com.example.recipes.data.ItemRequest
import com.example.recipes.data.Type
import com.example.recipes.data.toUserMessage
import kotlinx.coroutines.launch

class RecipeEditViewModel : ViewModel() {

    var loading by mutableStateOf(true)
        private set
    var loadError by mutableStateOf<String?>(null)
        private set
    var saving by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)

    var types by mutableStateOf<List<Type>>(emptyList())
        private set
    var daytimeTypes by mutableStateOf<List<DaytimeType>>(emptyList())
        private set

    // Поля формы
    var typeId by mutableIntStateOf(-1)
    var daytimeId by mutableIntStateOf(-1)
    var name by mutableStateOf("")
    var text by mutableStateOf("")
    val ingredients = mutableStateListOf<String>()

    private var started = false

    val isValid: Boolean
        get() = name.isNotBlank() && text.isNotBlank() && typeId >= 0 && daytimeId >= 0

    // recipeId = -1 значит создание нового рецепта
    fun start(recipeId: Int, initType: Int, initDaytime: Int) {
        if (started) return
        started = true
        load(recipeId, initType, initDaytime)
    }

    fun load(recipeId: Int, initType: Int, initDaytime: Int) {
        viewModelScope.launch {
            loading = true
            loadError = null
            try {
                types = ApiClient.api.getTypes()
                daytimeTypes = ApiClient.api.getDaytimeTypes()

                var dt = initDaytime
                if (recipeId >= 0) {
                    val d = ApiClient.api.getRecipe(recipeId)
                    name = d.name
                    text = d.recipeText
                    ingredients.clear()
                    ingredients.addAll(d.ingredients.orEmpty())
                    dt = d.daytimeTypeId ?: initDaytime
                }
                typeId = initType.takeIf { t -> types.any { it.id == t } }
                    ?: types.firstOrNull()?.id ?: -1
                daytimeId = dt.takeIf { t -> daytimeTypes.any { it.id == t } }
                    ?: daytimeTypes.firstOrNull()?.id ?: -1
                if (ingredients.isEmpty()) ingredients.add("")
            } catch (e: Exception) {
                loadError = e.toUserMessage()
            }
            loading = false
        }
    }

    fun save(recipeId: Int, onDone: () -> Unit) {
        if (!isValid || saving) return
        val body = ItemRequest(
            typeId = typeId,
            daytimeTypeId = daytimeId,
            name = name.trim(),
            ingredients = ingredients.map { it.trim() }.filter { it.isNotEmpty() },
            recipeText = text.trim()
        )
        viewModelScope.launch {
            saving = true
            try {
                if (recipeId < 0) ApiClient.api.createRecipe(body)
                else ApiClient.api.updateRecipe(recipeId, body)
                onDone()
            } catch (e: Exception) {
                message = e.toUserMessage()
            }
            saving = false
        }
    }
}