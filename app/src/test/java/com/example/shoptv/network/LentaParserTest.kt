package com.example.shoptv.network

import org.junit.Test
import org.junit.Assert.*

class LentaParserTest {
    @Test
    fun parseSimpleCard() {
        val html = """
            <html><body>
            <div class="product-card" data-id="p123">
              <a href="/product/p123" title="Test Product">Test Product</a>
              <span class="price">123.45 ₽</span>
              <img src="https://example.com/img.jpg" />
            </div>
            </body></html>
        """.trimIndent()

        val list = LentaParser.parseProducts(html, "https://lenta.com")
        assertEquals(1, list.size)
        val p = list[0]
        assertEquals("Test Product", p.title)
        assertEquals(123.45, p.price, 0.001)
        assertEquals("https://example.com/img.jpg", p.imageUrl)
        assertTrue(p.detailUrl!!.contains("/product/p123"))
        assertEquals("p123", p.id)
    }
}

