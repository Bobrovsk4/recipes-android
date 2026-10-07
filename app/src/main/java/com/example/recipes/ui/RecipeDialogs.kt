package com.example.recipes.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.recipes.data.Recipe
import com.example.recipes.data.Type

@Composable
fun RecipeEditDialog(
    draft: RecipeDraft,
    types: List<Type>,
    busy: Boolean,
    onDismiss: () -> Unit,
    onSave: (RecipeDraft) -> Unit
) {
    var typeId by remember(draft) { mutableStateOf(draft.typeId) }
    var name by remember(draft) { mutableStateOf(draft.name) }
    var text by remember(draft) { mutableStateOf(draft.text) }
    val valid = name.isNotBlank() && text.isNotBlank()

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        shape = RoundedCornerShape(28.dp),
        title = { Text(if (draft.id == null) "Новый рецепт" else "Изменить рецепт") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Кухня", style = MaterialTheme.typography.labelLarge)
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    types.forEach { t ->
                        FilterChip(
                            selected = typeId == t.id,
                            onClick = { typeId = t.id },
                            label = { Text(typeTitle(t.name)) }
                        )
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Текст рецепта") },
                    minLines = 4,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
//                OutlinedTextField(
//                    value = image,
//                    onValueChange = { image = it },
//                    label = { Text("Картинка (имя файла или URL)") },
//                    singleLine = true,
//                    shape = RoundedCornerShape(12.dp),
//                    modifier = Modifier.fillMaxWidth()
//                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid && !busy,
                onClick = { onSave(draft.copy(typeId = typeId, name = name, text = text)) }
            ) { Text(if (busy) "Сохранение…" else "Сохранить") }
        },
        dismissButton = {
            TextButton(enabled = !busy, onClick = onDismiss) { Text("Отмена") }
        }
    )
}

@Composable
fun DeleteDialog(
    recipe: Recipe,
    busy: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        shape = RoundedCornerShape(28.dp),
        title = { Text("Удалить рецепт?") },
        text = { Text("«${recipe.name}» будет удалён без возможности восстановления.") },
        confirmButton = {
            TextButton(enabled = !busy, onClick = onConfirm) {
                Text("Удалить", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(enabled = !busy, onClick = onDismiss) { Text("Отмена") }
        }
    )
}

@Composable
fun AddTypeDialog(
    busy: Boolean,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    title: String = "Новый тип",
    initialName: String = ""
) {
    var name by remember(initialName) { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        shape = RoundedCornerShape(28.dp),
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Название типа") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && !busy,
                onClick = { onSave(name) }
            ) { Text(if (busy) "Сохранение…" else "Сохранить") }
        },
        dismissButton = {
            TextButton(enabled = !busy, onClick = onDismiss) { Text("Отмена") }
        }
    )
}

@Composable
fun DeleteTypeDialog(
    type: Type,
    recipeCount: Int?,
    busy: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val hasRecipes = (recipeCount ?: 0) > 0

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        shape = RoundedCornerShape(28.dp),
        title = { Text(if (hasRecipes) "В типе есть рецепты" else "Удалить тип?") },
        text = {
            Text(
                when {
                    recipeCount == null ->
                        "Проверяем, есть ли в типе «${typeTitle(type.name)}» рецепты…"
                    recipeCount < 0 ->
                        "Не удалось проверить, есть ли в типе «${typeTitle(type.name)}» рецепты. " +
                                "Если они есть, они могут быть удалены вместе с типом."
                    recipeCount > 0 ->
                        "В типе «${typeTitle(type.name)}» рецептов: $recipeCount. " +
                                "Они также будут удалены. " +
                                "Удалить тип?"
                    else ->
                        "Тип «${typeTitle(type.name)}» будет удалён."
                }
            )
        },
        confirmButton = {
            TextButton(enabled = recipeCount != null && !busy, onClick = onConfirm) {
                Text(
                    if (hasRecipes) "Удалить всё равно" else "Удалить",
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(enabled = !busy, onClick = onDismiss) { Text("Отмена") }
        }
    )
}