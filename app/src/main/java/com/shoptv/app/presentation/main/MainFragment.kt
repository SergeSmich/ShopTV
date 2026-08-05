package com.shoptv.app.presentation.main

import android.content.ActivityNotFoundException
import android.content.Intent
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.shoptv.app.R
import com.shoptv.app.presentation.cart.CartActivity
import com.shoptv.app.presentation.cart.CartManager
import com.shoptv.app.presentation.common.ProductCardPresenter
import com.shoptv.app.presentation.detail.ProductDetailActivity
import com.shoptv.app.presentation.search.SearchActivity
import com.shoptv.app.presentation.store.StoreSelectionFragment
import com.shoptv.app.presentation.store.StoreSelectionActivity
import com.shoptv.core.model.UnifiedProduct
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainFragment : BrowseSupportFragment() {

    private val viewModel: MainViewModel by viewModel()
    private val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        title = getString(R.string.browse_title)
        headersState = HEADERS_ENABLED
        isHeadersTransitionOnBackEnabled = true
        brandColor = ContextCompat.getColor(requireContext(), R.color.magnit_red)
        adapter = rowsAdapter

        // Клик — открыть товар, поиск, магазин или корзину
        setOnItemViewClickedListener { _, item, _, _ ->
            when (item) {
                is UnifiedProduct -> openProduct(item)
                is String -> {
                    when {
                        item.contains("Поиск") -> startActivity(Intent(requireContext(), SearchActivity::class.java))
                        item.contains("магазин") -> startActivity(Intent(requireContext(), StoreSelectionActivity::class.java))
                        item.contains("Корзина") -> startActivity(Intent(requireContext(), CartActivity::class.java))
                    }
                }
            }
        }

        observeViewModel()
        loadCatalog()
    }

    override fun onResume() {
        super.onResume()
        // Перезагружаем каталог при возврате из выбора магазина
        loadCatalog()
    }

    private fun loadCatalog() {
        val prefs = requireContext().getSharedPreferences(StoreSelectionFragment.PREFS_NAME, 0)
        val storeCode = prefs.getString(StoreSelectionFragment.KEY_STORE_CODE, DEFAULT_STORE) ?: DEFAULT_STORE
        viewModel.loadMagnitCatalog(storeCode = storeCode)
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.rows.collect { renderRows(it) } }
                launch { viewModel.loading.collect { onLoadingChanged(it) } }
                launch { viewModel.error.collect { onError(it) } }
                launch { CartManager.count.collect { updateCartHeader() } }
            }
        }
    }

    private fun updateCartHeader() {
        // Находим заголовок корзины и обновляем
        for (i in 0 until rowsAdapter.size()) {
            val listRow = rowsAdapter.get(i) as? ListRow ?: continue
            if (listRow.id == HEADER_CART) {
                val header = HeaderItem(HEADER_CART, "🛒 Корзина (${CartManager.getItems().size})")
                // Обновляем заголовок через пересоздание строки
                val newAdapter = ArrayObjectAdapter(ProductCardPresenter()).apply {
                    add("🛒 Корзина (${CartManager.getItems().size})")
                }
                rowsAdapter.replace(i, ListRow(header, newAdapter))
                break
            }
        }
    }

    private fun renderRows(rows: List<com.shoptv.feature.magnit.data.local.CatalogRow>) {
        rowsAdapter.clear()
        if (rows.isEmpty()) return

        val cardPresenter = ProductCardPresenter(
            onLongClick = { product ->
                CartManager.add(product)
                Toast.makeText(requireContext(), "🛒 ${product.title}", Toast.LENGTH_SHORT).show()
            }
        )
        var total = 0

        // Строка поиска
        val searchAdapter = ArrayObjectAdapter(cardPresenter).apply {
            add("🔍 Поиск товаров")
        }
        rowsAdapter.add(ListRow(HeaderItem(HEADER_SEARCH, "Поиск"), searchAdapter))

        // Строка магазина
        val storeAdapter = ArrayObjectAdapter(cardPresenter).apply {
            add("🏪 Выбрать магазин")
        }
        rowsAdapter.add(ListRow(HeaderItem(HEADER_STORE, "Магазин"), storeAdapter))

        // Строка корзины
        val cartCount = CartManager.getItems().size
        val cartAdapter = ArrayObjectAdapter(cardPresenter).apply {
            add("🛒 Корзина ($cartCount)")
        }
        rowsAdapter.add(ListRow(HeaderItem(HEADER_CART, "Корзина"), cartAdapter))

        rows.forEachIndexed { index, row ->
            val rowAdapter = ArrayObjectAdapter(cardPresenter).apply {
                addAll(0, row.products)
            }
            val header = HeaderItem(index.toLong(), "${row.title} (${row.products.size})")
            rowsAdapter.add(ListRow(header, rowAdapter))
            total += row.products.size
        }

        title = getString(R.string.browse_title_with_count, total)
    }

    private fun onLoadingChanged(loading: Boolean) {
        if (loading) {
            title = getString(R.string.loading)
        }
    }

    private fun onError(message: String?) {
        if (message.isNullOrBlank()) return
        title = getString(R.string.browse_title)
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    private fun openProduct(product: UnifiedProduct) {
        val intent = Intent(requireContext(), ProductDetailActivity::class.java).apply {
            putExtra(ProductDetailActivity.EXTRA_PRODUCT_ID, product.id)
            putExtra(ProductDetailActivity.EXTRA_PRODUCT_TITLE, product.title)
            putExtra(ProductDetailActivity.EXTRA_PRODUCT_PRICE, product.priceCurrent)
            putExtra(ProductDetailActivity.EXTRA_PRODUCT_OLD_PRICE, product.priceOld ?: 0.0)
            putExtra(ProductDetailActivity.EXTRA_PRODUCT_IMAGE, product.imageUrl ?: "")
            putExtra(ProductDetailActivity.EXTRA_PRODUCT_CATEGORY, product.categoryName ?: "")
            putExtra(ProductDetailActivity.EXTRA_PRODUCT_DEEP_LINK, product.deepLink ?: "")
        }
        startActivity(intent)
    }

    companion object {
        private const val HEADER_SEARCH = -1L
        private const val HEADER_STORE = -2L
        private const val HEADER_CART = -3L
        private const val DEFAULT_STORE = "781225"
    }
}
