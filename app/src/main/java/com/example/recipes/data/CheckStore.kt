package com.example.recipes.data

import android.content.Context

object CheckStore {
    private const val PREFS = "ingredient_checks"
    private fun key(recipeId: Int) = "recipe_$recipeId"

    fun load(context: Context, recipeId: Int): Set<String> =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(key(recipeId), emptySet())
            ?.toSet()
            ?: emptySet()

    fun save(context: Context, recipeId: Int, checked: Set<String>) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().apply {
            if (checked.isEmpty()) remove(key(recipeId))
            else putStringSet(key(recipeId), checked)
        }.apply()
    }
}