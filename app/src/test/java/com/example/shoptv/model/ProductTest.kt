package com.example.shoptv.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductTest {

    @Test
    fun formatsPriceWithKopecks() {
        val p = Product(title = "Масло", price = 179.99)
        assertEquals("179,99 ₽", p.priceFormatted)
    }

    @Test
    fun formatsWholePriceWithoutKopecks() {
        val p = Product(title = "Хлеб", price = 45.0)
        assertEquals("45 ₽", p.priceFormatted)
    }

    @Test
    fun exposesDiscountWhenOldPricePresent() {
        val p = Product(
            title = "Масло сливочное",
            price = 179.99,
            oldPrice = 359.99,
            discountPercent = 50
        )
        assertTrue(p.hasDiscount)
        assertEquals("-50%", p.discountLabel)
        assertEquals("359,99 ₽", p.oldPriceFormatted)
    }

    @Test
    fun noDiscountWhenPercentZero() {
        val p = Product(title = "Молоко", price = 89.99)
        assertFalse(p.hasDiscount)
        assertNull(p.discountLabel)
        assertNull(p.oldPriceFormatted)
    }

    @Test
    fun buildsAbsoluteUrlFromRelativeLink() {
        val p = Product(title = "Сыр", price = 1.0, detailUrl = "/product/123-syr")
        assertEquals("https://magnit.ru/product/123-syr", p.fullUrl)
    }

    @Test
    fun keepsAbsoluteUrlAsIs() {
        val p = Product(title = "Сыр", price = 1.0, detailUrl = "https://magnit.ru/product/9")
        assertEquals("https://magnit.ru/product/9", p.fullUrl)
    }
}
