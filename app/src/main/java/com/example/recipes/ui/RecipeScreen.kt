package com.example.recipes.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.recipes.data.CheckStore
import com.example.recipes.data.RecipeDetails

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeScreen(
    recipeId: Int,
    onBack: () -> Unit,
    onEdit: (typeId: Int, daytimeId: Int) -> Unit,
    refreshSignal: Boolean,
    onRefreshHandled: () -> Unit,
    viewModel: RecipeViewModel = viewModel()
) {
    LaunchedEffect(recipeId) { viewModel.loadIfNeeded(recipeId) }
    LaunchedEffect(refreshSignal) {
        if (refreshSignal) {
            viewModel.load(recipeId)
            onRefreshHandled()
        }
    }

    val state = viewModel.state
    val scheme = MaterialTheme.colorScheme
    var showFireworks by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = scheme.background,
            topBar = {
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        FilledTonalIconButton(
                            onClick = onBack,
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                        }
                    },
                    actions = {
                        val loaded = (state as? RecipeState.Success)?.recipe
                        if (loaded != null) {
                            FilledTonalIconButton(
                                onClick = { onEdit(loaded.typeId ?: -1, loaded.daytimeTypeId ?: -1) },
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Icon(Icons.Filled.Edit, contentDescription = "Изменить")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = scheme.background)
                )
            }
        ) { padding ->
            when (state) {
                is RecipeState.Loading -> Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }

                is RecipeState.Error -> Column(
                    Modifier.fillMaxSize().padding(padding).padding(24.dp),
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
                    Button(onClick = { viewModel.load(recipeId) }) { Text("Повторить") }
                }

                is RecipeState.Success -> RecipeContent(
                    recipe = state.recipe,
                    contentPadding = padding,
                    onCelebrate = { showFireworks = true }
                )
            }
        }

        if (showFireworks) {
            Fireworks(onFinished = { showFireworks = false })
        }
    }
}

@Composable
private fun RecipeContent(
    recipe: RecipeDetails,
    contentPadding: PaddingValues,
    onCelebrate: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val dark = scheme.background.luminance() < 0.5f
    val green = if (dark) Color(0xFF81C784) else Color(0xFF2E7D32)
    val onGreen = if (dark) Color(0xFF0B3D1E) else Color.White

    val ingredients = remember(recipe) {
        recipe.ingredients.orEmpty().map { it.trim() }.filter { it.isNotEmpty() }
    }
    var checked by remember(recipe.id) { mutableStateOf(CheckStore.load(context, recipe.id)) }

    fun update(new: Set<String>) {
        val wasAll = ingredients.isNotEmpty() && ingredients.all { it in checked }
        checked = new
        CheckStore.save(context, recipe.id, new)
        val nowAll = ingredients.isNotEmpty() && ingredients.all { it in new }
        if (!wasAll && nowAll) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onCelebrate()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        HeroCard(name = recipe.name, ingredientCount = ingredients.size)

        if (ingredients.isNotEmpty()) {
            IngredientsCard(
                ingredients = ingredients,
                checked = checked,
                green = green,
                onGreen = onGreen,
                onToggle = { item, on -> update(if (on) checked + item else checked - item) },
                onReset = { update(emptySet()) }
            )
        }

        StepsCard(text = recipe.recipeText)
    }
}

@Composable
private fun HeroCard(name: String, ingredientCount: Int) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(32.dp)

    Box(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.linearGradient(listOf(scheme.primaryContainer, scheme.surfaceContainerLow))
            )
            .border(1.dp, scheme.outlineVariant.copy(alpha = 0.6f), shape)
            .padding(24.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                name,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = scheme.onSurface
            )
            if (ingredientCount > 0) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = scheme.surface.copy(alpha = 0.6f)
                ) {
                    Text(
                        "$ingredientCount ${ingredientsWord(ingredientCount)}",
                        style = MaterialTheme.typography.labelLarge,
                        color = scheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun IngredientsCard(
    ingredients: List<String>,
    checked: Set<String>,
    green: Color,
    onGreen: Color,
    onToggle: (String, Boolean) -> Unit,
    onReset: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val done = ingredients.count { it in checked }
    val all = done == ingredients.size
    val progress by animateFloatAsState(done / ingredients.size.toFloat(), label = "progress")

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = scheme.surfaceContainer,
        contentColor = scheme.onSurface
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.ShoppingBasket,
                    contentDescription = null,
                    tint = scheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    "Ингредиенты",
                    style = MaterialTheme.typography.titleMedium,
                    color = scheme.primary,
                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                )
                Text(
                    if (all) "Всё собрано" else "$done / ${ingredients.size}",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (all) green else scheme.onSurfaceVariant
                )
            }

            LinearProgressIndicator(
                progress = { progress },
                color = green,
                trackColor = scheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 14.dp)
                    .height(8.dp)
                    .clip(CircleShape)
            )

            ingredients.forEach { item ->
                IngredientRow(
                    text = item,
                    checked = item in checked,
                    green = green,
                    onGreen = onGreen,
                    onChange = { onToggle(item, it) }
                )
            }

            if (done > 0) {
                TextButton(
                    onClick = onReset,
                    modifier = Modifier.align(Alignment.End)
                ) { Text("Сбросить отметки") }
            }
        }
    }
}

@Composable
private fun IngredientRow(
    text: String,
    checked: Boolean,
    green: Color,
    onGreen: Color,
    onChange: (Boolean) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val textColor by animateColorAsState(if (checked) green else scheme.onSurface, label = "text")
    val bg by animateColorAsState(green.copy(alpha = if (checked) 0.14f else 0f), label = "bg")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            colors = CheckboxDefaults.colors(
                checkedColor = green,
                uncheckedColor = scheme.outline,
                checkmarkColor = onGreen
            ),
            modifier = Modifier.padding(12.dp)
        )
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (checked) FontWeight.Medium else FontWeight.Normal,
            color = textColor,
            modifier = Modifier.weight(1f).padding(end = 12.dp)
        )
    }
}

@Composable
private fun StepsCard(text: String) {
    val scheme = MaterialTheme.colorScheme

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = scheme.surfaceContainer,
        contentColor = scheme.onSurface
    ) {
        Column(
            Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = null,
                    tint = scheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    "Приготовление",
                    style = MaterialTheme.typography.titleMedium,
                    color = scheme.primary,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            Text(
                text,
                style = MaterialTheme.typography.bodyLarge.copy(
                    lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.25f
                ),
                color = scheme.onSurface
            )
        }
    }
}

private fun ingredientsWord(n: Int): String {
    val m10 = n % 10
    val m100 = n % 100
    return when {
        m100 in 11..14 -> "ингредиентов"
        m10 == 1 -> "ингредиент"
        m10 in 2..4 -> "ингредиента"
        else -> "ингредиентов"
    }
}