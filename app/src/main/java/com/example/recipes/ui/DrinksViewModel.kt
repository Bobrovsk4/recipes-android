package com.example.recipes.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recipes.data.ApiClient
import com.example.recipes.data.toUserMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import com.example.recipes.data.Recipe

class DrinksViewModel : ViewModel() {

    var state by mutableStateOf<TypesState>(TypesState.Loading)
        private set
    var drinksState by mutableStateOf<RecipesState>(RecipesState.Loading)
        private set
    var typeFilter by mutableStateOf<Int?>(null)
        private set
    var isRefreshing by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)
    var deleting by mutableStateOf<Recipe?>(null)
        private set
    var busy by mutableStateOf(false)
        private set

    val hasFilter: Boolean get() = typeFilter != null

    private var drinksJob: Job? = null
    private var serverVersion = ApiClient.version

    init {
        load()
    }

    fun newDrinkType(): Int? {
        val types = (state as? TypesState.Success)?.types.orEmpty()
        val t = typeFilter ?: types.firstOrNull()?.id
        if (t == null) message = "Типов напитков пока нет"
        return t
    }

    fun askDelete(drink: Recipe) { deleting = drink }

    fun cancelDelete() { deleting = null }

    fun confirmDelete() {
        val drink = deleting ?: return
        viewModelScope.launch {
            busy = true
            try {
                ApiClient.api.deleteDrink(drink.id)
                deleting = null
                loadDrinks()
                message = "Напиток удалён"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                deleting = null
                message = e.toUserMessage()
            }
            busy = false
        }
    }

    fun messageShown() { message = null }

    fun syncServer(version: Int) {
        if (version != serverVersion) {
            serverVersion = version
            reload()
        }
    }

    fun load() {
        state = TypesState.Loading
        viewModelScope.launch {
            try {
                val types = ApiClient.api.getDrinkTypes()
                if (typeFilter != null && types.none { it.id == typeFilter }) typeFilter = null
                state = TypesState.Success(types)
                loadDrinks()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                state = TypesState.Error(e.toUserMessage())
            }
        }
    }

    fun loadDrinks() {
        drinksJob?.cancel()
        drinksState = RecipesState.Loading
        val t = typeFilter
        drinksJob = viewModelScope.launch {
            drinksState = try {
                RecipesState.Success(ApiClient.api.getDrinks(t))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                RecipesState.Error(e.toUserMessage())
            }
        }
    }

    fun reload() {
        typeFilter = null
        load()
    }

    fun refresh() {
        if (isRefreshing) return
        drinksJob?.cancel()
        viewModelScope.launch {
            isRefreshing = true
            try {
                val types = ApiClient.api.getDrinkTypes()
                val t = typeFilter?.takeIf { id -> types.any { it.id == id } }
                val list = ApiClient.api.getDrinks(t)
                state = TypesState.Success(types)
                typeFilter = t
                drinksState = RecipesState.Success(list)
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
        loadDrinks()
    }

    fun clearFilter() {
        if (typeFilter == null) return
        typeFilter = null
        loadDrinks()
    }
}