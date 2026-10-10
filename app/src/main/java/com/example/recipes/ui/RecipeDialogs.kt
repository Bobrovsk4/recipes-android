package com.example.recipes.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
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
    val blocked = (recipeCount ?: 0) > 0
    val name = typeTitle(type.name)

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        shape = RoundedCornerShape(28.dp),
        title = { Text(if (blocked) "Удалить нельзя" else "Удалить тип?") },
        text = {
            Text(
                when {
                    recipeCount == null -> "Проверяем, есть ли в типе «$name» рецепты…"
                    recipeCount < 0 -> "Не удалось проверить рецепты типа «$name». " +
                            "Если они есть, сервер откажется удалять тип."
                    recipeCount > 0 -> "В типе «$name» рецептов: $recipeCount. " +
                            "Сервер не удаляет тип, пока в нём есть рецепты. " +
                            "Удалите их или перенесите в другой тип (кнопка изменения у рецепта)."
                    else -> "Тип «$name» будет удалён."
                }
            )
        },
        confirmButton = {
            if (blocked) {
                TextButton(onClick = onDismiss) { Text("Понятно") }
            } else {
                TextButton(enabled = recipeCount != null && !busy, onClick = onConfirm) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        dismissButton = if (blocked) null else {
            { TextButton(enabled = !busy, onClick = onDismiss) { Text("Отмена") } }
        }
    )
}