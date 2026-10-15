package com.example.recipes.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButtonDefaults.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.recipes.data.ApiClient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrinksScreen(
    onDrinkClick: (Int) -> Unit,
    onCreate: (typeId: Int) -> Unit,
    viewModel: DrinksViewModel = viewModel()
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme

    val apiVersion = ApiClient.version
    LaunchedEffect(apiVersion) { viewModel.syncServer(apiVersion) }

    val message = viewModel.message
    LaunchedEffect(message) {
        if (message != null) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.messageShown()
        }
    }

    viewModel.deleting?.let { d ->
        DeleteDialog(
            recipe = d,
            busy = viewModel.busy,
            title = "Удалить напиток?",
            onConfirm = { viewModel.confirmDelete() },
            onDismiss = { viewModel.cancelDelete() }
        )
    }

    when (val state = viewModel.state) {
        is TypesState.Loading -> Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) { CircularProgressIndicator() }

        is TypesState.Error -> Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Ошибка загрузки",
                style = MaterialTheme.typography.titleLarge,
                color = scheme.onBackground
            )
            Text(
                state.message,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Button(onClick = { viewModel.load() }) { Text("Повторить") }
        }

        is TypesState.Success -> PullToRefreshBox(
            isRefreshing = viewModel.isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Заголовок
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Напитки",
                            style = MaterialTheme.typography.headlineMedium,
                            color = scheme.onBackground
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (viewModel.hasFilter) {
                                TextButton(onClick = { viewModel.clearFilter() }) { Text("Сбросить") }
                            }
                            IconButton(onClick = { viewModel.newDrinkType()?.let(onCreate) }) {
                                Icon(
                                    Icons.Filled.Add,
                                    contentDescription = "Добавить напиток",
                                    tint = scheme.primary
                                )
                            }
                        }
                    }
                }

                // Чипсы типов напитков
                item {
                    if (state.types.isEmpty()) {
                        Text(
                            "Типов напитков пока нет",
                            color = scheme.onSurfaceVariant
                        )
                    } else {
                        ScrollableChipRow {
                            state.types.forEach { type ->
                                AppChip(
                                    label = typeTitle(type.name),
                                    selected = viewModel.typeFilter == type.id,
                                    onClick = { viewModel.toggleType(type.id) }
                                )
                            }
                        }
                    }
                }

                // Список напитков
                when (val ds = viewModel.drinksState) {
                    is RecipesState.Loading -> item {
                        Box(
                            Modifier.fillMaxWidth().padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) { CircularProgressIndicator() }
                    }

                    is RecipesState.Error -> item {
                        Column {
                            Text(ds.message, color = scheme.error)
                            TextButton(onClick = { viewModel.loadDrinks() }) {
                                Text("Повторить")
                            }
                        }
                    }

                    is RecipesState.Success -> {
                        if (ds.recipes.isEmpty()) {
                            item {
                                Text(
                                    if (viewModel.hasFilter) "В этом типе напитков нет"
                                    else "Пока нет напитков",
                                    color = scheme.onSurfaceVariant,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                        items(ds.recipes, key = { it.id }) { drink ->
                            RecipeItem(
                                recipe = drink,
                                onClick = { onDrinkClick(drink.id) },
                                onDelete = { viewModel.askDelete(drink) },
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                }
            }
        }
    }
}