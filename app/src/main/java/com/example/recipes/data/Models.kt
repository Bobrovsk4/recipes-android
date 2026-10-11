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

data class DaytimeType(
    val id: Int,
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
    val type: String? = null, // TODO: убрать отсюда и с сервака
    val ingredients: List<String>? = null,
    @SerializedName("recipe_text") val recipeText: String,
    @SerializedName("type_id") val typeId: Int? = null,
    @SerializedName("daytime_type") val daytimeTypeId: Int? = null
)

data class ItemRequest(
    @SerializedName("type") val typeId: Int,
    @SerializedName("daytime_type") val daytimeTypeId: Int,
    val name: String,
    val ingredients: List<String>,
    @SerializedName("recipe_text") val recipeText: String
)
