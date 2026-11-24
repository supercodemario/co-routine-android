package com.diljith.android_coroutine.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException

private val errorJson = Json { ignoreUnknownKeys = true }

@Serializable
private data class ErrorBody(val message: String? = null)

suspend fun <T> safeApiCall(block: suspend () -> T): ApiResult<T> {
    return try {
        ApiResult.Success(block())
    } catch (exception: HttpException) {
        ApiResult.Error(
            code = exception.code(),
            message = exception.errorMessage()
        )
    } catch (exception: IOException) {
        ApiResult.Error(message = exception.message ?: "Check your internet connection")
    } catch (exception: Exception) {
        ApiResult.Error(message = exception.message ?: "Something went wrong")
    }
}

private fun HttpException.errorMessage(): String {
    val raw = response()?.errorBody()?.string()
    val parsed = raw?.let { body ->
        runCatching { errorJson.decodeFromString<ErrorBody>(body).message }.getOrNull()
    }
    return parsed ?: message()
}
