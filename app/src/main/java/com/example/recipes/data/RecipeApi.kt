package com.example.recipes.data

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface RecipeApi {

    @GET("api/types")
    suspend fun getTypes(): List<Type>

    @GET("api/items")
    suspend fun getRecipes(
        @Query("type") typeId: Int? = null,
        @Query("daytime_type") daytimeType: Int? = null
    ): List<Recipe>

    @GET("api/daytime_types")
    suspend fun getDaytimeTypes(): List<DaytimeType>

    @GET("api/items/random")
    suspend fun getRandomRecipe(
        @Query("type_id") typeId: Int? = null,
        @Query("daytime_type_id") daytimeTypeId: Int? = null
    ): RecipeDetails

    @GET("api/items/{id}")
    suspend fun getRecipe(@Path("id") id: Int): RecipeDetails

    @POST("api/items")
    suspend fun createRecipe(@Body body: ItemRequest): RecipeDetails

    @POST("api/types")
    suspend fun createType(@Body body: TypeRequest): Type

    @PUT("api/items/{id}")
    suspend fun updateRecipe(@Path("id") id: Int, @Body body: ItemRequest): RecipeDetails

    @PUT("api/types/{id}")
    suspend fun updateType(@Path("id") id: Int, @Body body: TypeRequest)

    @DELETE("api/items/{id}")
    suspend fun deleteRecipe(@Path("id") id: Int)

    @DELETE("api/types/{id}")
    suspend fun deleteType(@Path("id") id: Int)
}