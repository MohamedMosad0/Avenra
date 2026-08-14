package com.avenra.app.data.repository

import com.avenra.app.data.remote.dto.BannerDto
import com.avenra.app.data.remote.dto.CategoryDto
import com.avenra.app.data.remote.dto.HomeResponseDto
import com.avenra.app.data.remote.dto.ProductDto
import com.avenra.app.data.remote.dto.SubcategoryDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeRepositoryMapperTest {

    @Test
    fun homeResponseDto_mapsTo_domainHomeData() {
        val dto = HomeResponseDto(
            banners = listOf(
                BannerDto("b1", "Title 1", "Sub 1", "http://img1.jpg")
            ),
            categories = listOf(
                CategoryDto("c1", "Cat 1", "http://cat1.jpg", listOf(SubcategoryDto("sub1", "SubCat 1", "c1")))
            ),
            featuredProducts = listOf(
                ProductDto("p1", "Prod 1", "Desc 1", 100.0, 80.0, "http://p1.jpg", listOf(), 4.5, 10, "c1", true, 5)
            )
        )

        val domain = dto.toDomain()

        assertEquals(1, domain.banners.size)
        assertEquals("b1", domain.banners[0].id)
        assertEquals("Title 1", domain.banners[0].title)

        assertEquals(1, domain.categories.size)
        assertEquals("c1", domain.categories[0].id)
        assertEquals(1, domain.categories[0].subcategories.size)

        assertEquals(1, domain.featuredProducts.size)
        assertEquals("p1", domain.featuredProducts[0].id)
        assertTrue(domain.featuredProducts[0].hasDiscount)
        assertEquals("EGP 80.00", domain.featuredProducts[0].formattedPrice)
    }
}
