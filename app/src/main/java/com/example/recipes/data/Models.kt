package com.example.recipes.data

import com.google.gson.annotations.SerializedName

// тип кухни
data class Type(
    val id: Int,
    val name: String
)

data class TypeRequest(
    val name: String
)

// превью рецепта
data class Recipe(
    val id: Int,
    val name: String,
    val type: String, // TODO: убрать отсюда и с сервака
//    val image: String?
)

// полный рецепт
data class RecipeDetails(
    val id: Int,
    val name: String,
    val type: String, // TODO: убрать отсюда и с сервака
    @SerializedName("recipe_text") val recipeText: String,
//    val image: String?
)

data class ItemRequest(
    @SerializedName("type") val typeId: Int,
    val name: String,
    @SerializedName("recipe_text") val recipeText: String
)