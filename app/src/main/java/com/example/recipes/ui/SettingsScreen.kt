package com.example.recipes.ui

import android.widget.Toast
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.recipes.data.ApiClient
import com.example.recipes.data.SettingsStore

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    var url by remember { mutableStateOf(ApiClient.baseUrl) }
    val dark = SettingsStore.darkTheme ?: isSystemInDarkTheme()
    val isValid = url.trim().let { it.startsWith("http://") || it.startsWith("https://") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Настройки",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        // --- Карточка: сервер ---
        SettingsCard(icon = { Icon(Icons.Filled.Dns, null) }, title = "Сервер") {
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("Адрес сервера") },
                singleLine = true,
                isError = url.isNotEmpty() && !isValid,
                supportingText = { Text("Пример: http://192.168.0.18:8080/") },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    if (isValid) {
                        SettingsStore.saveUrl(context, url.trim())
                        Toast.makeText(context, "Принято. Обновляй", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(
                            context,
                            "Херню написал. Стартуй с http:// или https://",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                },
                enabled = url.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Сохранить")
            }
        }

        // --- Карточка: оформление ---
        SettingsCard(icon = { Icon(Icons.Filled.DarkMode, null) }, title = "Оформление") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Тёмная тема", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = dark,
                    onCheckedChange = { SettingsStore.saveDarkTheme(context, it) }
                )
            }
        }
    }
}

// Общая карточка с иконкой и заголовком. Все цвета берутся из темы.
@Composable
private fun SettingsCard(
    icon: @Composable () -> Unit,
    title: String,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.material3.LocalContentColor provides MaterialTheme.colorScheme.primary
                ) { icon() }
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            content()
        }
    }
}