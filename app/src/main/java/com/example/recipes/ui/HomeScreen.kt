package com.example.recipes.ui

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.recipes.data.ApiClient
import com.example.recipes.data.Recipe
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalViewConfiguration
import com.example.recipes.data.Type
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onRecipeClick: (Int) -> Unit,
    onCreate: (typeId: Int, daytimeId: Int) -> Unit,
    refreshSignal: Boolean,
    onRefreshHandled: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val context = LocalContext.current

    // Перезагрузка при смене адреса сервера
    val apiVersion = ApiClient.version
    var lastVersion by remember { mutableIntStateOf(apiVersion) }
    LaunchedEffect(apiVersion) {
        if (apiVersion != lastVersion) {
            lastVersion = apiVersion
            viewModel.reload()
        }
    }

    // Тосты с результатом операций
    val message = viewModel.message
    LaunchedEffect(message) {
        if (message != null) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.messageShown()
        }
    }

    // Автовыбор первого типа, когда ничего не выбрано
    val state = viewModel.state
    LaunchedEffect(state) {
        if (state is TypesState.Success &&
            viewModel.expandedTypeId == null &&
            state.types.isNotEmpty()
        ) {
            viewModel.toggleType(state.types.first().id)
        }
    }

    LaunchedEffect(refreshSignal) {
        if (refreshSignal) {
            viewModel.refresh()
            onRefreshHandled()
        }
    }

    // Диалоги
    viewModel.deleting?.let { r ->
        DeleteDialog(
            recipe = r,
            busy = viewModel.busy,
            onConfirm = { viewModel.confirmDelete() },
            onDismiss = { viewModel.cancelDelete() }
        )
    }
    if (viewModel.addingType) {
        AddTypeDialog(
            busy = viewModel.busy,
            onDismiss = { viewModel.dismissAddType() },
            onSave = { viewModel.saveType(it) }
        )
    }
    viewModel.renamingType?.let { t ->
        AddTypeDialog(
            busy = viewModel.busy,
            title = "Переименовать тип",
            initialName = t.name,
            onDismiss = { viewModel.dismissRenameType() },
            onSave = { viewModel.renameType(it) }
        )
    }
    viewModel.deletingType?.let { t ->
        DeleteTypeDialog(
            type = t,
            recipeCount = viewModel.deletingTypeRecipeCount,
            busy = viewModel.busy,
            onConfirm = { viewModel.confirmDeleteType() },
            onDismiss = { viewModel.cancelDeleteType() }
        )
    }

    when (state) {
        is TypesState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        is TypesState.Error -> {
            Column(
                Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Ошибка загрузки",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    state.message,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Button(onClick = { viewModel.load() }) { Text("Повторить") }
            }
        }

        is TypesState.Success -> {
            val selectedId = viewModel.expandedTypeId
            val recipesState = selectedId?.let { viewModel.recipes[it] ?: RecipesState.Loading }
            PullToRefreshBox(
                isRefreshing = viewModel.isRefreshing,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Заголовок с кнопками
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Главная",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Row {
                                var menuOpen by remember { mutableStateOf(false) }
                                Box {
                                    IconButton(onClick = { menuOpen = true }) {
                                        Icon(
                                            Icons.Filled.Add,
                                            contentDescription = "Добавить",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = menuOpen,
                                        onDismissRequest = { menuOpen = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Новый рецепт") },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Filled.Restaurant,
                                                    contentDescription = null
                                                )
                                            },
                                            onClick = {
                                                menuOpen = false
                                                viewModel.createArgs()?.let { (t, d) -> onCreate(t, d) }
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Новый тип") },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Filled.Category,
                                                    contentDescription = null
                                                )
                                            },
                                            onClick = {
                                                menuOpen = false
                                                viewModel.startAddType()
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            ScrollableChipRow {
                                viewModel.daytimeTypes.forEach { d ->
                                    FilterChip(
                                        selected = viewModel.daytimeFilter == d.id,
                                        onClick = { viewModel.selectDaytime(d.id) },
                                        label = { Text(d.name.replaceFirstChar { it.uppercase() }) },
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                            }
                            // Чипсы типов
                            if (state.types.isEmpty()) {
                                Text(
                                    "Типов пока нет. Добавьте через «+».",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                ScrollableChipRow {
                                    state.types.forEach { type ->
                                        TypeChip(
                                            type = type,
                                            selected = type.id == selectedId,
                                            onSelect = { viewModel.toggleType(type.id) },
                                            onRename = { viewModel.startRenameType(type) },
                                            onDelete = { viewModel.askDeleteType(type) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Блюда выбранного типа
                    when (recipesState) {
                        null -> Unit

                        is RecipesState.Loading -> item {
                            Box(
                                Modifier.fillMaxWidth().padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) { CircularProgressIndicator() }
                        }

                        is RecipesState.Error -> item {
                            Column {
                                Text(
                                    recipesState.message,
                                    color = MaterialTheme.colorScheme.error
                                )
                                TextButton(onClick = { viewModel.loadRecipes(selectedId!!) }) {
                                    Text("Повторить")
                                }
                            }
                        }

                        is RecipesState.Success -> {
                            if (recipesState.recipes.isEmpty()) {
                                item {
                                    Text(
                                        "Пока нет блюд",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }
                            items(recipesState.recipes, key = { it.id }) { recipe ->
                                RecipeItem(
                                    recipe = recipe,
                                    onClick = { onRecipeClick(recipe.id) },
                                    onDelete = { viewModel.askDelete(recipe) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecipeItem(
    recipe: Recipe,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val shape = RoundedCornerShape(20.dp)

    Box {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        menuOpen = true
                    }
                ),
            shape = shape,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Text(
                text = recipe.name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 18.dp)
            )
        }

        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = {
                    Text(
                        "Удалить",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                },
                onClick = { menuOpen = false; onDelete() },
                modifier = Modifier.height(36.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
            )
        }
    }
}

fun typeTitle(key: String): String = key.replaceFirstChar { it.uppercase() }

@Composable
private fun TypeChip(
    type: Type,
    selected: Boolean,
    onSelect: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    var longPressed by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val longPressTimeout = LocalViewConfiguration.current.longPressTimeoutMillis

    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collectLatest { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    delay(longPressTimeout)
                    longPressed = true
                    menuOpen = true
                }
                is PressInteraction.Cancel -> longPressed = false
                else -> Unit
            }
        }
    }

    Box {
        FilterChip(
            selected = selected,
            onClick = {
                if (longPressed) longPressed = false
                else if (!selected) onSelect()
            },
            label = {
                Text(typeTitle(type.name), style = MaterialTheme.typography.titleSmall)
            },
            shape = RoundedCornerShape(12.dp),
            interactionSource = interactionSource
        )
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text("Переименовать") },
                onClick = { menuOpen = false; onRename() },
            )
            DropdownMenuItem(
                text = { Text("Удалить", color = MaterialTheme.colorScheme.error) },
                onClick = { menuOpen = false; onDelete() }
            )
        }
    }
}

@Composable
private fun ScrollableChipRow(
    sidePadding: Dp = 16.dp,
    content: @Composable RowScope.() -> Unit
) {
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
        Row(
            modifier = Modifier
                .layout { measurable, constraints ->
                    val extra = sidePadding.roundToPx() * 2
                    val placeable = measurable.measure(
                        constraints.copy(maxWidth = constraints.maxWidth + extra)
                    )
                    layout(constraints.maxWidth, placeable.height) {
                        placeable.place(-extra / 2, 0)
                    }
                }
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = sidePadding),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}