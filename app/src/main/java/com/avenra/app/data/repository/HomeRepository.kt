package com.avenra.app.data.repository

import com.avenra.app.data.remote.api.ApiService
import com.avenra.app.data.remote.api.NetworkModule
import com.avenra.app.data.remote.dto.BannerDto
import com.avenra.app.data.remote.dto.CategoryDto
import com.avenra.app.data.remote.dto.HomeResponseDto
import com.avenra.app.data.remote.dto.ProductDto
import com.avenra.app.domain.model.Banner
import com.avenra.app.domain.model.Category
import com.avenra.app.domain.model.DataError
import com.avenra.app.domain.model.HomeData
import com.avenra.app.domain.model.NetworkResult
import com.avenra.app.domain.model.Product
import com.avenra.app.domain.model.Subcategory
import retrofit2.HttpException
import java.io.IOException

class HomeRepository(
    private val apiService: ApiService = NetworkModule.apiService
) {
    suspend fun getHomeData(): NetworkResult<HomeData> {
        return try {
            val responseDto = apiService.getHome()
            val homeData = responseDto.toDomain()
            NetworkResult.Success(homeData)
        } catch (e: IOException) {
            NetworkResult.Error(DataError.Network, e)
        } catch (e: HttpException) {
            NetworkResult.Error(DataError.Server(statusCode = e.code()), e)
        } catch (e: Exception) {
            NetworkResult.Error(DataError.Unknown(), e)
        }
    }
}


// DTO to Domain Mapper extensions
fun HomeResponseDto.toDomain(): HomeData {
    return HomeData(
        banners = banners.map { it.toDomain() },
        categories = categories.map { it.toDomain() },
        featuredProducts = featuredProducts.map { it.toDomain() }
    )
}

fun BannerDto.toDomain(): Banner {
    return Banner(
        id = id,
        title = title,
        subtitle = subtitle,
        imageUrl = imageUrl,
        targetCategoryId = targetCategoryId
    )
}

fun CategoryDto.toDomain(): Category {
    return Category(
        id = id,
        name = name,
        imageUrl = imageUrl,
        subcategories = subcategories?.map { Subcategory(it.id, it.name, it.categoryId) } ?: emptyList()
    )
}

fun ProductDto.toDomain(): Product {
    return Product(
        id = id,
        title = title,
        description = description,
        price = price,
        discountPrice = discountPrice,
        imageUrl = imageUrl,
        galleryImages = galleryImages ?: emptyList(),
        rating = rating,
        reviewCount = reviewCount,
        categoryId = categoryId,
        isAvailable = isAvailable,
        availableQuantity = availableQuantity,
        sizes = sizes ?: emptyList(),
        colors = colors ?: emptyList()
    )
}
