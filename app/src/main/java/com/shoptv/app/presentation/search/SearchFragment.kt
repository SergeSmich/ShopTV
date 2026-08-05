package com.shoptv.app.presentation.search

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.leanback.app.SearchSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.ObjectAdapter
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.shoptv.app.R
import com.shoptv.app.presentation.common.ProductCardPresenter
import com.shoptv.core.model.UnifiedProduct
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Поиск по каталогу. Leanback сам рисует строку ввода и кнопку голосового
 * ввода, от нас нужен адаптер с результатами.
 */
class SearchFragment : SearchSupportFragment(),
    SearchSupportFragment.SearchResultProvider {

    private val viewModel: SearchViewModel by viewModel()
    private val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setSearchResultProvider(this)
        setOnItemViewClickedListener { _, item, _, _ ->
            (item as? UnifiedProduct)?.let { openProduct(it) }
        }

        observeResults()
    }

    override fun getResultsAdapter(): ObjectAdapter = rowsAdapter

    override fun onQueryTextChange(newQuery: String): Boolean {
        viewModel.onQueryChanged(newQuery)
        return true
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        viewModel.onQuerySubmitted(query)
        return true
    }

    private fun observeResults() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.results.collect { products -> render(products) }
            }
        }
    }

    private fun render(products: List<UnifiedProduct>) {
        rowsAdapter.clear()
        if (products.isEmpty()) {
            // при пустом запросе ничего не показываем, при неудачном — сообщаем
            if (viewModel.query.value.isNotBlank()) {
                val header = HeaderItem(0, getString(R.string.search_nothing_found))
                rowsAdapter.add(ListRow(header, ArrayObjectAdapter(ProductCardPresenter())))
            }
            return
        }

        val rowAdapter = ArrayObjectAdapter(ProductCardPresenter()).apply {
            addAll(0, products)
        }
        val header = HeaderItem(0, getString(R.string.search_found, products.size))
        rowsAdapter.add(ListRow(header, rowAdapter))
    }

    private fun openProduct(product: UnifiedProduct) {
        val intent = Intent(requireContext(), com.shoptv.app.presentation.detail.ProductDetailActivity::class.java).apply {
            putExtra(com.shoptv.app.presentation.detail.ProductDetailActivity.EXTRA_PRODUCT_ID, product.id)
            putExtra(com.shoptv.app.presentation.detail.ProductDetailActivity.EXTRA_PRODUCT_TITLE, product.title)
            putExtra(com.shoptv.app.presentation.detail.ProductDetailActivity.EXTRA_PRODUCT_PRICE, product.priceCurrent)
            putExtra(com.shoptv.app.presentation.detail.ProductDetailActivity.EXTRA_PRODUCT_OLD_PRICE, product.priceOld ?: 0.0)
            putExtra(com.shoptv.app.presentation.detail.ProductDetailActivity.EXTRA_PRODUCT_IMAGE, product.imageUrl ?: "")
            putExtra(com.shoptv.app.presentation.detail.ProductDetailActivity.EXTRA_PRODUCT_CATEGORY, product.categoryName ?: "")
            putExtra(com.shoptv.app.presentation.detail.ProductDetailActivity.EXTRA_PRODUCT_DEEP_LINK, product.deepLink ?: "")
        }
        startActivity(intent)
    }
}
