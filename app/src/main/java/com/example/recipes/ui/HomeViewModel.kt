package com.example.recipes.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recipes.data.ApiClient
import com.example.recipes.data.DaytimeType
import com.example.recipes.data.Recipe
import com.example.recipes.data.Type
import com.example.recipes.data.TypeRequest
import kotlinx.coroutines.launch
import com.example.recipes.data.toUserMessage

sealed interface TypesState {
    data object Loading : TypesState
    data class Success(val types: List<Type>) : TypesState
    data class Error(val message: String) : TypesState
}

sealed interface RecipesState {
    data object Loading : RecipesState
    data class Success(val recipes: List<Recipe>) : RecipesState
    data class Error(val message: String) : RecipesState
}

class HomeViewModel : ViewModel() {
    var deleting by mutableStateOf<Recipe?>(null)
        private set
    var busy by mutableStateOf(false)
        private set

    var addingType by mutableStateOf(false)
        private set

    var isRefreshing by mutableStateOf(false)
        private set

    var renamingType by mutableStateOf<Type?>(null)
        private set

    var deletingType by mutableStateOf<Type?>(null)
        private set

    var deletingTypeRecipeCount by mutableStateOf<Int?>(null)
        private set

    var daytimeTypes by mutableStateOf<List<DaytimeType>>(emptyList())
        private set

    var daytimeFilter by mutableStateOf<Int?>(null)
        private set

    var message by mutableStateOf<String?>(null)

    fun messageShown() { message = null }
    fun startAddType() { addingType = true }
    fun dismissAddType() { addingType = false }
    fun startRenameType(t: Type) { renamingType = t }
    fun dismissRenameType() { renamingType = null }
    fun askDeleteType(t: Type) {
        deletingType = t
        deletingTypeRecipeCount = null
        deletingTypeRecipeCount = (recipes[t.id] as? RecipesState.Success)?.recipes?.size
        viewModelScope.launch {
            val count = try {
                ApiClient.api.getRecipes(t.id, daytimeTypes.firstOrNull()?.id).size
            } catch (e: Exception) {
                -1
            }
            if (deletingType?.id == t.id) deletingTypeRecipeCount = count
        }
    }

    fun createArgs(): Pair<Int, Int>? {
        val types = (state as? TypesState.Success)?.types ?: return null
        val t = expandedTypeId ?: types.firstOrNull()?.id
        val d = daytimeFilter ?: daytimeTypes.firstOrNull()?.id
        if (t == null || d == null) {
            message = "Сначала добавьте тип"
            return null
        }
        return t to d
    }

    fun cancelDeleteType() {
        deletingType = null
        deletingTypeRecipeCount = null
    }

    private suspend fun refreshTypes() {
        state = TypesState.Success(ApiClient.api.getTypes())
    }

    private fun loadDaytimeTypes() {
        viewModelScope.launch {
            try {
                daytimeTypes = ApiClient.api.getDaytimeTypes()
            } catch (e: Exception) {
                message = e.toUserMessage()
            }
        }
    }

    fun selectDaytime(id: Int?) {
        if (daytimeFilter == id) return
        daytimeFilter = id
        recipes.clear()
        expandedTypeId?.let { loadRecipes(it) }
    }

    fun saveType(name: String) {
        viewModelScope.launch {
            busy = true
            try {
                ApiClient.api.createType(TypeRequest(name.trim()))
                addingType = false
                load()   // перезагружаем список типов
                message = "Тип добавлен"
            } catch (e: Exception) {
                message = e.toUserMessage()
            }
            busy = false
        }
    }

    fun askDelete(recipe: Recipe) { deleting = recipe }
    fun cancelDelete() { deleting = null }

    fun confirmDelete() {
        val recipe = deleting ?: return
        viewModelScope.launch {
            busy = true
            try {
                ApiClient.api.deleteRecipe(recipe.id)
                deleting = null
                val open = expandedTypeId
                recipes.clear()
                if (open != null) loadRecipes(open)
                message = "Рецепт удалён"
            } catch (e: Exception) {
                deleting = null
                message = e.toUserMessage()
            }
            busy = false
        }
    }
    var state by mutableStateOf<TypesState>(TypesState.Loading)
        private set

    // id раскрытой кухни (раскрыта может быть только одна)
    var expandedTypeId by mutableStateOf<Int?>(null)
        private set

    // состояние списка блюд для каждой кухни
    val recipes = mutableStateMapOf<Int, RecipesState>()

    init {
        load()
    }

    fun load() {
        state = TypesState.Loading
        viewModelScope.launch {
            state = try {
                val dts = ApiClient.api.getDaytimeTypes()
                daytimeTypes = dts
                if (dts.none { it.id == daytimeFilter }) {
                    daytimeFilter = dts.firstOrNull { it.name.equals("Общее", ignoreCase = true) }?.id
                        ?: dts.firstOrNull()?.id
                }
                TypesState.Success(ApiClient.api.getTypes())
            } catch (e: Exception) {
                TypesState.Error(e.toUserMessage())
            }
        }
    }

    fun toggleType(typeId: Int) {
        if (expandedTypeId == typeId) {
            expandedTypeId = null
            return
        }
        expandedTypeId = typeId
        if (recipes[typeId] !is RecipesState.Success) {
            loadRecipes(typeId)
        }
    }

    fun loadRecipes(typeId: Int) {
        recipes[typeId] = RecipesState.Loading
        viewModelScope.launch {
            recipes[typeId] = try {
                RecipesState.Success(ApiClient.api.getRecipes(typeId, daytimeFilter))
            } catch (e: Exception) {
                RecipesState.Error(e.toUserMessage())
            }
        }
    }

    fun reload() {
        expandedTypeId = null
        daytimeFilter = null
        recipes.clear()
        load()
    }

    fun refresh() {
        if (isRefreshing) return
        viewModelScope.launch {
            isRefreshing = true
            try {
                val types = ApiClient.api.getTypes()
                val selected = expandedTypeId?.takeIf { id -> types.any { it.id == id } }
                    ?: types.firstOrNull()?.id
                val list = selected?.let { ApiClient.api.getRecipes(it, daytimeFilter) }

                state = TypesState.Success(types)
                recipes.clear()
                expandedTypeId = selected
                if (selected != null && list != null) {
                    recipes[selected] = RecipesState.Success(list)
                }
            } catch (e: Exception) {
                message = e.toUserMessage()
            }
            isRefreshing = false
        }
    }

    fun renameType(newName: String) {
        val t = renamingType ?: return
        viewModelScope.launch {
            busy = true
            try {
                ApiClient.api.updateType(t.id, TypeRequest(newName.trim()))
                renamingType = null
                refreshTypes()
                message = "Тип переименован"
            } catch (e: Exception) {
                message = e.toUserMessage()
            }
            busy = false
        }
    }

    fun confirmDeleteType() {
        val t = deletingType ?: return
        viewModelScope.launch {
            busy = true
            try {
                ApiClient.api.deleteType(t.id)
                deletingType = null
                deletingTypeRecipeCount = null
                recipes.remove(t.id)
                if (expandedTypeId == t.id) expandedTypeId = null
                refreshTypes()
                message = "Тип удалён"
            } catch (e: Exception) {
                deletingType = null
                deletingTypeRecipeCount = null
                message = e.toUserMessage()
            }
            busy = false
        }
    }
}