package com.example.recipes.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RecipeEditScreen(
    recipeId: Int,
    typeId: Int,
    daytimeId: Int,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: RecipeEditViewModel = viewModel()
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    LaunchedEffect(Unit) { viewModel.start(recipeId, typeId, daytimeId) }

    val message = viewModel.message
    LaunchedEffect(message) {
        if (message != null) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.message = null
        }
    }

    val ready = !viewModel.loading && viewModel.loadError == null
    val imeVisible = WindowInsets.isImeVisible

    Scaffold(
        modifier = Modifier.imePadding(),
        containerColor = scheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (recipeId < 0) "Новый рецепт" else "Изменение рецепта",
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    FilledTonalIconButton(
                        onClick = onBack,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = scheme.background,
                    titleContentColor = scheme.onSurfaceVariant
                )
            )
        },
        bottomBar = {
            if (ready && !imeVisible) {
                Surface(color = scheme.background) {
                    Button(
                        onClick = { viewModel.save(recipeId, onSaved) },
                        enabled = viewModel.isValid && !viewModel.saving,
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .height(56.dp)
                    ) {
                        if (viewModel.saving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 3.dp,
                                color = scheme.onPrimary
                            )
                        } else {
                            Icon(Icons.Filled.Check, contentDescription = null)
                            Text(
                                "Сохранить",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        when {
            viewModel.loading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            viewModel.loadError != null -> Column(
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
                    viewModel.loadError.orEmpty(),
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Button(onClick = { viewModel.load(recipeId, typeId, daytimeId) }) {
                    Text("Повторить")
                }
            }

            else -> Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Название
                NameCard(
                    value = viewModel.name,
                    onChange = { viewModel.name = it }
                )

                // Тип и время приёма пищи
                EditSection(icon = Icons.Filled.Category, title = "Категория") {
                    Text(
                        "Тип",
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        viewModel.types.forEach { t ->
                            AppChip(
                                label = typeTitle(t.name),
                                selected = viewModel.typeId == t.id,
                                onClick = { viewModel.typeId = t.id }
                            )
                        }
                    }
                    Text(
                        "Время приёма пищи",
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        viewModel.daytimeTypes.forEach { d ->
                            AppChip(
                                label = d.name.replaceFirstChar { it.uppercase() },
                                selected = viewModel.daytimeId == d.id,
                                onClick = { viewModel.daytimeId = d.id },
                                secondary = true
                            )
                        }
                    }
                }

                // Ингредиенты
                EditSection(
                    icon = Icons.Filled.ShoppingBasket,
                    title = "Ингредиенты",
                    trailing = viewModel.ingredients
                        .count { it.isNotBlank() }
                        .takeIf { it > 0 }
                        ?.toString()
                ) {
                    viewModel.ingredients.forEachIndexed { index, value ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(scheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "${index + 1}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = scheme.onPrimaryContainer
                                )
                            }
                            OutlinedTextField(
                                value = value,
                                onValueChange = { viewModel.ingredients[index] = it },
                                placeholder = { Text("Например: Свёкла, 2 шт") },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                colors = fieldColors(),
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Sentences
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp)
                            )
                            IconButton(onClick = { viewModel.ingredients.removeAt(index) }) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "Убрать ингредиент",
                                    tint = scheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    FilledTonalButton(
                        onClick = { viewModel.ingredients.add("") },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Text("Добавить ингредиент", modifier = Modifier.padding(start = 8.dp))
                    }
                }

                // Приготовление
                EditSection(icon = Icons.AutoMirrored.Filled.MenuBook, title = "Приготовление") {
                    OutlinedTextField(
                        value = viewModel.text,
                        onValueChange = { viewModel.text = it },
                        placeholder = { Text("Опишите шаги приготовления") },
                        minLines = 8,
                        shape = RoundedCornerShape(16.dp),
                        colors = fieldColors(),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun NameCard(value: String, onChange: (String) -> Unit) {
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
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Название блюда",
                    style = MaterialTheme.typography.titleMedium,
                    color = scheme.onPrimaryContainer,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }
            OutlinedTextField(
                value = value,
                onValueChange = onChange,
                placeholder = { Text("Например, Борщ") },
                textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                colors = fieldColors(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun EditSection(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    trailing: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
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
                    icon,
                    contentDescription = null,
                    tint = scheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = scheme.primary,
                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                )
                if (trailing != null) {
                    Text(
                        trailing,
                        style = MaterialTheme.typography.labelLarge,
                        color = scheme.onSurfaceVariant
                    )
                }
            }
            content()
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
)