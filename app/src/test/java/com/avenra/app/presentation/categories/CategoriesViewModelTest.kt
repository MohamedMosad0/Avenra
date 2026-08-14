package com.avenra.app.presentation.categories

import com.avenra.app.domain.model.Category
import com.avenra.app.domain.model.Subcategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class CategoriesViewModelTest {

    @Test
    fun category_domainModel_storesSubcategoriesCorrectly() {
        val subcategories = listOf(
            Subcategory("sub1", "Dresses", "cat1"),
            Subcategory("sub2", "Tops", "cat1")
        )
        val category = Category("cat1", "Women's Fashion", "http://cat1.jpg", subcategories)

        assertEquals("cat1", category.id)
        assertEquals("Women's Fashion", category.name)
        assertEquals(2, category.subcategories.size)
        assertEquals("Dresses", category.subcategories[0].name)
    }
}
