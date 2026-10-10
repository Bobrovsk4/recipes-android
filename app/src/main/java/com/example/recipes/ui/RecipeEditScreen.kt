package com.example.recipes.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

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
    LaunchedEffect(Unit) { viewModel.start(recipeId, typeId, daytimeId) }

    val message = viewModel.message
    LaunchedEffect(message) {
        if (message != null) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.message = null
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(if (recipeId < 0) "Новый рецепт" else "Изменение рецепта")
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
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
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    viewModel.loadError.orEmpty(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Название
                OutlinedTextField(
                    value = viewModel.name,
                    onValueChange = { viewModel.name = it },
                    label = { Text("Название") },
                    textStyle = MaterialTheme.typography.titleLarge,
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Тип и время приёма пищи
                EditCard(title = "Категория") {
                    Text("Тип", style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        viewModel.types.forEach { t ->
                            FilterChip(
                                selected = viewModel.typeId == t.id,
                                onClick = { viewModel.typeId = t.id },
                                label = { Text(typeTitle(t.name)) }
                            )
                        }
                    }
                    Text(
                        "Время приёма пищи",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        viewModel.daytimeTypes.forEach { d ->
                            FilterChip(
                                selected = viewModel.daytimeId == d.id,
                                onClick = { viewModel.daytimeId = d.id },
                                label = { Text(d.name.replaceFirstChar { it.uppercase() }) }
                            )
                        }
                    }
                }

                // Ингредиенты
                EditCard(title = "Ингредиенты") {
                    viewModel.ingredients.forEachIndexed { index, value ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = value,
                                onValueChange = { viewModel.ingredients[index] = it },
                                placeholder = { Text("Например: Свёкла, 2 шт") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { viewModel.ingredients.removeAt(index) }) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "Убрать ингредиент",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    TextButton(onClick = { viewModel.ingredients.add("") }) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Text("Добавить ингредиент", modifier = Modifier.padding(start = 8.dp))
                    }
                }

                // Текст рецепта
                EditCard(title = "Рецепт") {
                    OutlinedTextField(
                        value = viewModel.text,
                        onValueChange = { viewModel.text = it },
                        placeholder = { Text("Опишите шаги приготовления") },
                        minLines = 6,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Button(
                    onClick = { viewModel.save(recipeId, onSaved) },
                    enabled = viewModel.isValid && !viewModel.saving,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (viewModel.saving) "Сохранение…" else "Сохранить")
                }
            }
        }
    }
}

@Composable
private fun EditCard(title: String, content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            content()
        }
    }
}