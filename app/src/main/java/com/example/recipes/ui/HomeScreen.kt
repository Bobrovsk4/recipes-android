package com.example.recipes.ui

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Button
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
import androidx.compose.animation.animateColorAsState
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.Color

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
                                    AppChip(
                                        label = d.name.replaceFirstChar { it.uppercase() },
                                        selected = viewModel.daytimeFilter == d.id,
                                        onClick = { viewModel.selectDaytime(d.id) },
                                        secondary = true
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
fun RecipeItem(
    recipe: Recipe,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var menuOpen by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(28.dp)

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, label = "press")

    Box(modifier.scale(scale)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            scheme.primaryContainer.copy(alpha = 0.55f),
                            scheme.surfaceContainerLow
                        )
                    )
                )
                .border(1.dp, scheme.outlineVariant.copy(alpha = 0.6f), shape)
                .combinedClickable(
                    interactionSource = interaction,
                    indication = LocalIndication.current,
                    onClick = onClick,
                    onLongClick = if (onDelete != null) {
                        {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            menuOpen = true
                        }
                    } else null
                )
        ) {
            Row(
                modifier = Modifier.padding(start = 20.dp, end = 14.dp, top = 16.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Акцентная чёрточка
                Box(
                    Modifier
                        .width(4.dp)
                        .height(28.dp)
                        .clip(CircleShape)
                        .background(scheme.primary)
                )
                Text(
                    text = recipe.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(horizontal = 16.dp)
                )
                // Круглая кнопка-стрелка
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(scheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = scheme.onPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            if (onDelete != null) {
                DropdownMenuItem(
                    text = {
                        Text(
                            "Удалить",
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.error
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = null,
                            tint = scheme.error,
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
        AppChip(
            label = typeTitle(type.name),
            selected = selected,
            onClick = {
                if (longPressed) longPressed = false
                else if (!selected) onSelect()
            },
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

@Composable
fun AppChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    secondary: Boolean = false,
    interactionSource: MutableInteractionSource? = null
) {
    val scheme = MaterialTheme.colorScheme
    val accent = if (secondary) scheme.secondary else scheme.primary
    val onAccent = if (secondary) scheme.onSecondary else scheme.onPrimary

    val container by animateColorAsState(
        if (selected) accent else scheme.surfaceContainer, label = "chipBg"
    )
    val content by animateColorAsState(
        if (selected) onAccent else scheme.onSurfaceVariant, label = "chipFg"
    )

    FilterChip(
        selected = selected,
        onClick = onClick,
        modifier = modifier.height(38.dp),
        interactionSource = interactionSource,
        shape = RoundedCornerShape(50),
        leadingIcon = if (selected) {
            { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
        } else null,
        label = {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = container,
            labelColor = content,
            iconColor = content,
            selectedContainerColor = container,
            selectedLabelColor = content,
            selectedLeadingIconColor = content
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = scheme.outlineVariant,
            selectedBorderColor = Color.Transparent,
            borderWidth = 1.dp,
            selectedBorderWidth = 0.dp
        )
    )
}