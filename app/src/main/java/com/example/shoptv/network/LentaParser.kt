package com.example.shoptv.network

import com.example.shoptv.model.Product
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.net.URI

object LentaParser {
    // Try several candidate selectors to locate product cards on the page.
    private val cardSelectors = listOf(
        ".product-card",
        ".catalog-item",
        ".product",
        "article",
        ".catalog-card",
        ".tile",
        ".card"
    )

    fun parseProducts(html: String, baseUrl: String): List<Product> {
        val doc: Document = Jsoup.parse(html, baseUrl)

        // choose selector with most matches
        val selector = cardSelectors.maxByOrNull { doc.select(it).size } ?: cardSelectors.first()
        val cards = doc.select(selector)

        val result = mutableListOf<Product>()

        for (card in cards) {
            try {
                val name = extractTitle(card)
                val price = extractPrice(card)
                val detailUrl = extractDetailUrl(card, baseUrl)
                val imageUrl = extractImageUrl(card)
                val id = extractId(card, detailUrl)

                if (!name.isNullOrBlank() && price != null) {
                    result.add(
                        Product(
                            id = id ?: detailUrl ?: "",
                            title = name,
                            price = price,
                            imageUrl = imageUrl,
                            detailUrl = detailUrl
                        )
                    )
                }
            } catch (_: Exception) {
                // ignore parse errors for individual cards
            }
        }

        return result
    }

    private fun extractTitle(card: Element): String? {
        val candidates = listOf(".product-title", ".title", "h3", "h2", ".card__title", ".tile__title")
        for (sel in candidates) {
            val el = card.selectFirst(sel)
            if (el != null) return el.text().trim()
        }
        // fallback: any text-bearing link
        val a = card.selectFirst("a[title], a[href]")
        return a?.attr("title")?.takeIf { it.isNotBlank() } ?: a?.text()?.trim()
    }

    private fun extractPrice(card: Element): Double? {
        val candidates = listOf(".price", ".product-price", ".price__value", ".card__price")
        for (sel in candidates) {
            val el = card.selectFirst(sel) ?: continue
            parsePrice(el.text())?.let { return it }
        }
        return null
    }

    /** "123.45 ₽" / "1 234,50 руб" -> 123.45 / 1234.50 */
    internal fun parsePrice(text: String): Double? {
        val cleaned = text.replace('\u00A0', ' ').replace(" ", "").replace(',', '.')
        val match = Regex("""\d+(\.\d+)?""").find(cleaned) ?: return null
        return match.value.toDoubleOrNull()
    }

    private fun extractDetailUrl(card: Element, baseUrl: String): String? {
        val a = card.selectFirst("a[href]") ?: return null
        val href = a.attr("href")
        return if (href.startsWith("http")) href else URI(baseUrl).resolve(href).toString()
    }

    private fun extractImageUrl(card: Element): String? {
        val img = card.selectFirst("img[src]") ?: card.selectFirst("img[data-src]")
        return img?.attr("src") ?: img?.attr("data-src")
    }

    private fun extractId(card: Element, detailUrl: String?): String? {
        // try data-sku or data-id
        val idAttr = card.attr("data-sku").ifBlank { card.attr("data-id") }
        if (idAttr.isNotBlank()) return idAttr
        // try to extract numeric id from detailUrl
        if (!detailUrl.isNullOrBlank()) {
            val segs = detailUrl.trimEnd('/').split('/')
            return segs.lastOrNull()
        }
        return null
    }
}
