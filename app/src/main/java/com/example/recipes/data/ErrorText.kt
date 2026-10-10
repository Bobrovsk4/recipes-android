package com.example.recipes.data

import com.google.gson.JsonParseException
import retrofit2.HttpException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

fun Throwable.toUserMessage(): String = when (this) {
    is SocketTimeoutException -> "Сервер не отвечает.\nПроверь адрес или выключи выпын."
    is ConnectException, is UnknownHostException ->
        "Не удалось подключиться к серверу.\nПроверь адрес или выключи выпын."
    is HttpException -> {
        val serverMsg = try {
            com.google.gson.JsonParser.parseString(response()?.errorBody()?.string())
                .asJsonObject.get("error").asString
        } catch (e: Exception) {
            null
        }
        when (code()) {
            404 -> "Не найдено на сервере (404)."
            409 -> serverMsg ?: "Конфликт: такое название уже есть или тип используется."
            in 500..599 -> "Ошибка на сервере (${code()})."
            else -> serverMsg ?: "Ошибка запроса (${code()})."
        }
    }
    is JsonParseException -> "Сервер вернул данные в неожиданном формате."
    else -> message ?: "Неизвестная ошибка."
}