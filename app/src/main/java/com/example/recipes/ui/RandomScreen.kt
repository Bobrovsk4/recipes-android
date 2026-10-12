package com.example.recipes.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun RandomScreen(
    onRecipeClick: (Int) -> Unit,
    viewModel: RandomViewModel = viewModel()
) {
    val scheme = MaterialTheme.colorScheme
    val recipe = viewModel.recipe
    val hasFilter = viewModel.typeFilter != null || viewModel.daytimeFilter != null

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Случайный рецепт",
                style = MaterialTheme.typography.headlineMedium,
                color = scheme.onBackground
            )
            if (hasFilter) {
                TextButton(onClick = { viewModel.clearFilters() }) { Text("Сбросить") }
            }
        }

        if (viewModel.daytimeTypes.isNotEmpty()) {
            ScrollableChipRow {
                viewModel.daytimeTypes.forEach { d ->
                    AppChip(
                        label = d.name.replaceFirstChar { it.uppercase() },
                        selected = viewModel.daytimeFilter == d.id,
                        onClick = { viewModel.toggleDaytime(d.id) },
                        secondary = true
                    )
                }
            }
        }

        if (viewModel.types.isNotEmpty()) {
            ScrollableChipRow {
                viewModel.types.forEach { t ->
                    AppChip(
                        label = typeTitle(t.name),
                        selected = viewModel.typeFilter == t.id,
                        onClick = { viewModel.toggleType(t.id) }
                    )
                }
            }
        }

        Box(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            if (recipe != null) {
                AnimatedContent(
                    targetState = recipe,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "random"
                ) { r ->
                    RecipeItem(recipe = r, onClick = { onRecipeClick(r.id) })
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Filled.Casino,
                        contentDescription = null,
                        tint = scheme.onSurfaceVariant,
                        modifier = Modifier.size(56.dp)
                    )
                    Text(
                        "Выберите тип или время приёма пищи\n(по желанию) и нажмите RANDOM.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = scheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        viewModel.error?.let {
            Text(
                it,
                color = scheme.error,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Button(
            onClick = { viewModel.roll() },
            enabled = !viewModel.loading,
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth().height(64.dp)
        ) {
            if (viewModel.loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 3.dp,
                    color = scheme.onPrimary
                )
            } else {
                Text(
                    "RANDOM",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp
                )
            }
        }
    }
}