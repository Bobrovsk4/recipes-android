package com.example.recipes.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recipes.data.ApiClient
import com.example.recipes.data.DaytimeType
import com.example.recipes.data.Recipe
import com.example.recipes.data.Type
import com.example.recipes.data.TypeRequest
import com.example.recipes.data.toUserMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

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

    var state by mutableStateOf<TypesState>(TypesState.Loading)
        private set
    var recipesState by mutableStateOf<RecipesState>(RecipesState.Loading)
        private set
    var typeFilter by mutableStateOf<Int?>(null)
        private set
    var daytimeFilter by mutableStateOf<Int?>(null)
        private set

    var daytimeTypes by mutableStateOf<List<DaytimeType>>(emptyList())
        private set

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
    var message by mutableStateOf<String?>(null)

    private var recipesJob: Job? = null

    init {
        load()
    }

    val hasFilter: Boolean get() = typeFilter != null || daytimeFilter != null

    fun messageShown() { message = null }

    fun load() {
        state = TypesState.Loading
        viewModelScope.launch {
            try {
                coroutineScope {
                    val typesJob = async { ApiClient.api.getTypes() }
                    val daytimesJob = async { ApiClient.api.getDaytimeTypes() }
                    val types = typesJob.await()
                    daytimeTypes = daytimesJob.await()

                    if (typeFilter != null && types.none { it.id == typeFilter }) typeFilter = null
                    if (daytimeFilter != null && daytimeTypes.none { it.id == daytimeFilter }) {
                        daytimeFilter = null
                    }
                    state = TypesState.Success(types)
                }
                loadRecipes()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                state = TypesState.Error(e.toUserMessage())
            }
        }
    }

    fun loadRecipes() {
        recipesJob?.cancel()
        recipesState = RecipesState.Loading
        val t = typeFilter
        val d = daytimeFilter
        recipesJob = viewModelScope.launch {
            recipesState = try {
                RecipesState.Success(ApiClient.api.getRecipes(t, d))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                RecipesState.Error(e.toUserMessage())
            }
        }
    }

    fun reload() {
        typeFilter = null
        daytimeFilter = null
        load()
    }

    fun refresh() {
        if (isRefreshing) return
        viewModelScope.launch {
            isRefreshing = true
            try {
                val types = ApiClient.api.getTypes()
                val dts = ApiClient.api.getDaytimeTypes()
                val t = typeFilter?.takeIf { id -> types.any { it.id == id } }
                val d = daytimeFilter?.takeIf { id -> dts.any { it.id == id } }
                val list = ApiClient.api.getRecipes(t, d)

                state = TypesState.Success(types)
                daytimeTypes = dts
                typeFilter = t
                daytimeFilter = d
                recipesState = RecipesState.Success(list)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                message = e.toUserMessage()
            }
            isRefreshing = false
        }
    }

    fun toggleType(id: Int) {
        typeFilter = if (typeFilter == id) null else id
        loadRecipes()
    }

    fun toggleDaytime(id: Int) {
        daytimeFilter = if (daytimeFilter == id) null else id
        loadRecipes()
    }

    fun clearFilters() {
        if (!hasFilter) return
        typeFilter = null
        daytimeFilter = null
        loadRecipes()
    }

    fun createArgs(): Pair<Int, Int>? {
        val types = (state as? TypesState.Success)?.types.orEmpty()
        val t = typeFilter ?: types.firstOrNull()?.id
        val d = daytimeFilter
            ?: daytimeTypes.firstOrNull { it.name.equals("общее", ignoreCase = true) }?.id
            ?: daytimeTypes.firstOrNull()?.id
        if (t == null || d == null) {
            message = "Сначала добавьте тип"
            return null
        }
        return t to d
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
                loadRecipes()
                message = "Рецепт удалён"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                deleting = null
                message = e.toUserMessage()
            }
            busy = false
        }
    }

    private suspend fun refreshTypes() {
        state = TypesState.Success(ApiClient.api.getTypes())
    }

    fun startAddType() { addingType = true }
    fun dismissAddType() { addingType = false }

    fun saveType(name: String) {
        viewModelScope.launch {
            busy = true
            try {
                ApiClient.api.createType(TypeRequest(name.trim()))
                addingType = false
                refreshTypes()
                message = "Тип добавлен"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                message = e.toUserMessage()
            }
            busy = false
        }
    }

    fun startRenameType(t: Type) { renamingType = t }
    fun dismissRenameType() { renamingType = null }

    fun renameType(newName: String) {
        val t = renamingType ?: return
        viewModelScope.launch {
            busy = true
            try {
                ApiClient.api.updateType(t.id, TypeRequest(newName.trim()))
                renamingType = null
                refreshTypes()
                message = "Тип переименован"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                message = e.toUserMessage()
            }
            busy = false
        }
    }

    fun askDeleteType(t: Type) {
        deletingType = t
        deletingTypeRecipeCount = null
        viewModelScope.launch {
            val count = try {
                ApiClient.api.getRecipes(t.id, null).size
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                -1
            }
            if (deletingType?.id == t.id) deletingTypeRecipeCount = count
        }
    }

    fun cancelDeleteType() {
        deletingType = null
        deletingTypeRecipeCount = null
    }

    fun confirmDeleteType() {
        val t = deletingType ?: return
        viewModelScope.launch {
            busy = true
            try {
                ApiClient.api.deleteType(t.id)
                deletingType = null
                deletingTypeRecipeCount = null
                refreshTypes()
                if (typeFilter == t.id) {
                    typeFilter = null
                    loadRecipes()
                }
                message = "Тип удалён"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                deletingType = null
                deletingTypeRecipeCount = null
                message = e.toUserMessage()
            }
            busy = false
        }
    }
}