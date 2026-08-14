package com.avenra.app.domain.model

sealed interface NetworkResult<out T> {
    data class Success<out T>(val data: T) : NetworkResult<T>
    data class Error(val error: DataError, val throwable: Throwable? = null) : NetworkResult<Nothing>
}

sealed interface DataError {
    data object Network : DataError
    data object NotFound : DataError
    data object Unauthorized : DataError
    data class Server(
        val statusCode: Int? = null,
        val errorCode: String? = null,
        val message: String? = null
    ) : DataError
    data class Unknown(val message: String? = null) : DataError
}
