package com.avenra.app.data.repository

import com.avenra.app.data.remote.api.ApiService
import com.avenra.app.data.remote.api.NetworkModule
import com.avenra.app.domain.model.DataError
import com.avenra.app.domain.model.NetworkResult
import com.avenra.app.domain.model.Product
import retrofit2.HttpException
import java.io.IOException

class ProductRepository(
    private val apiService: ApiService = NetworkModule.apiService
) {
    suspend fun getProducts(
        categoryId: String? = null,
        query: String? = null
    ): NetworkResult<List<Product>> {
        return try {
            val responseDto = apiService.getProducts(categoryId = categoryId, query = query)
            val products = responseDto.products.map { it.toDomain() }
            NetworkResult.Success(products)
        } catch (e: IOException) {
            NetworkResult.Error(DataError.Network, e)
        } catch (e: HttpException) {
            NetworkResult.Error(DataError.Server(statusCode = e.code()), e)
        } catch (e: Exception) {
            NetworkResult.Error(DataError.Unknown(), e)
        }
    }

    suspend fun getProductById(productId: String): NetworkResult<Product> {
        return try {
            val responseDto = apiService.getProductById(productId)
            NetworkResult.Success(responseDto.product.toDomain())
        } catch (e: IOException) {
            NetworkResult.Error(DataError.Network, e)
        } catch (e: HttpException) {
            if (e.code() == 404) {
                NetworkResult.Error(DataError.NotFound, e)
            } else {
                NetworkResult.Error(DataError.Server(statusCode = e.code()), e)
            }
        } catch (e: Exception) {
            NetworkResult.Error(DataError.Unknown(), e)
        }
    }
}

