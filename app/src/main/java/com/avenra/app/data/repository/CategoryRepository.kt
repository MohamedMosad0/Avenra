package com.avenra.app.data.repository

import com.avenra.app.data.remote.api.ApiService
import com.avenra.app.data.remote.api.NetworkModule
import com.avenra.app.domain.model.Category
import com.avenra.app.domain.model.DataError
import com.avenra.app.domain.model.NetworkResult
import retrofit2.HttpException
import java.io.IOException

class CategoryRepository(
    private val apiService: ApiService = NetworkModule.apiService
) {
    suspend fun getCategories(): NetworkResult<List<Category>> {
        return try {
            val responseDto = apiService.getCategories()
            val categories = responseDto.categories.map { it.toDomain() }
            NetworkResult.Success(categories)
        } catch (e: IOException) {
            NetworkResult.Error(DataError.Network, e)
        } catch (e: HttpException) {
            NetworkResult.Error(DataError.Server(statusCode = e.code()), e)
        } catch (e: Exception) {
            NetworkResult.Error(DataError.Unknown(), e)
        }
    }
}

