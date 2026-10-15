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
import com.example.recipes.data.DrinkRequest
import com.example.recipes.data.ItemRequest
import com.example.recipes.data.Type
import com.example.recipes.data.toUserMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
    private var drinkMode = false

    val isValid: Boolean
        get() = name.isNotBlank() && text.isNotBlank() && typeId >= 0 &&
                (drinkMode || daytimeId >= 0)

    fun start(recipeId: Int, initType: Int, initDaytime: Int, drink: Boolean) {
        if (started) return
        started = true
        drinkMode = drink
        load(recipeId, initType, initDaytime)
    }

    fun load(recipeId: Int, initType: Int, initDaytime: Int) {
        viewModelScope.launch {
            loading = true
            loadError = null
            try {
                coroutineScope {
                    val typesJob = async {
                        if (drinkMode) ApiClient.api.getDrinkTypes() else ApiClient.api.getTypes()
                    }
                    val daytimesJob =
                        if (drinkMode) null else async { ApiClient.api.getDaytimeTypes() }
                    val recipeJob = if (recipeId >= 0) async {
                        if (drinkMode) ApiClient.api.getDrink(recipeId)
                        else ApiClient.api.getRecipe(recipeId)
                    } else null

                    types = typesJob.await()
                    daytimeTypes = daytimesJob?.await().orEmpty()

                    var tp = initType
                    var dt = initDaytime
                    recipeJob?.await()?.let { d ->
                        name = d.name
                        text = d.recipeText
                        ingredients.clear()
                        ingredients.addAll(d.ingredients.orEmpty())
                        tp = d.typeId ?: initType
                        dt = d.daytimeTypeId ?: initDaytime
                    }
                    typeId = tp.takeIf { t -> types.any { it.id == t } }
                        ?: types.firstOrNull()?.id ?: -1
                    daytimeId = if (drinkMode) -1 else {
                        dt.takeIf { t -> daytimeTypes.any { it.id == t } }
                            ?: daytimeTypes.firstOrNull()?.id ?: -1
                    }
                    if (ingredients.isEmpty()) ingredients.add("")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                loadError = e.toUserMessage()
            }
            loading = false
        }
    }

    fun save(recipeId: Int, onDone: () -> Unit) {
        if (!isValid || saving) return
        val list = ingredients.map { it.trim() }.filter { it.isNotEmpty() }
        viewModelScope.launch {
            saving = true
            try {
                if (drinkMode) {
                    val body = DrinkRequest(typeId, name.trim(), list, text.trim())
                    if (recipeId < 0) ApiClient.api.createDrink(body)
                    else ApiClient.api.updateDrink(recipeId, body)
                } else {
                    val body = ItemRequest(
                        typeId = typeId,
                        daytimeTypeId = daytimeId,
                        name = name.trim(),
                        ingredients = list,
                        recipeText = text.trim()
                    )
                    if (recipeId < 0) ApiClient.api.createRecipe(body)
                    else ApiClient.api.updateRecipe(recipeId, body)
                }
                onDone()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                message = e.toUserMessage()
            }
            saving = false
        }
    }
}