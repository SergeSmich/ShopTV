package com.example.shoptv

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.lifecycle.lifecycleScope
import com.example.shoptv.model.Product
import com.example.shoptv.presenter.ProductCardPresenter
import com.example.shoptv.repository.MagnitCatalogRepository
import kotlinx.coroutines.launch

/**
 * Главный экран: каталог Магнита строками (Скидки дня, Молочное, Заморозка и т.д.).
 * Данные читаются из assets/magnit_catalog.json.
 */
class MainFragment : BrowseSupportFragment() {

    private lateinit var repository: MagnitCatalogRepository
    private val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repository = MagnitCatalogRepository(requireContext().applicationContext)

        setupUi()
        adapter = rowsAdapter
        loadCatalog()
    }

    private fun setupUi() {
        title = getString(R.string.browse_title)
        headersState = HEADERS_ENABLED
        isHeadersTransitionOnBackEnabled = true
        brandColor = ContextCompat.getColor(requireContext(), R.color.magnit_red)
        searchAffordanceColor = ContextCompat.getColor(requireContext(), R.color.magnit_red)
        view?.setBackgroundColor(Color.parseColor("#141414"))

        setOnItemViewClickedListener { _, item, _, _ ->
            (item as? Product)?.let { openProduct(it) }
        }
    }

    private fun loadCatalog() {
        lifecycleScope.launch {
            val catalog = repository.getCatalog()

            if (catalog.rows.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.catalog_empty),
                    Toast.LENGTH_LONG
                ).show()
                return@launch
            }

            rowsAdapter.clear()
            val cardPresenter = ProductCardPresenter()

            catalog.rows.forEachIndexed { index, row ->
                val listRowAdapter = ArrayObjectAdapter(cardPresenter).apply {
                    addAll(0, row.items)
                }
                val header = HeaderItem(
                    index.toLong(),
                    "${row.title} (${row.items.size})"
                )
                rowsAdapter.add(ListRow(header, listRowAdapter))
            }

            title = "${catalog.shop} — ${catalog.totalItems} товаров"
        }
    }

    /** Открывает страницу товара на magnit.ru во внешнем браузере, если он есть на устройстве */
    private fun openProduct(product: Product) {
        val url = product.fullUrl
        if (url.isNullOrBlank()) {
            Toast.makeText(requireContext(), product.title, Toast.LENGTH_SHORT).show()
            return
        }
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(requireContext(), product.title, Toast.LENGTH_SHORT).show()
        }
    }
}
