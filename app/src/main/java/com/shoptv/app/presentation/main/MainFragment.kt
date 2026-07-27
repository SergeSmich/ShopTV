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
import com.shoptv.app.presentation.common.ProductCardPresenter
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

        setOnItemViewClickedListener { _, item, _, _ ->
            (item as? UnifiedProduct)?.let { openProduct(it) }
        }

        observeViewModel()
        viewModel.loadMagnitCatalog()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.rows.collect { renderRows(it) } }
                launch { viewModel.loading.collect { onLoadingChanged(it) } }
                launch { viewModel.error.collect { onError(it) } }
            }
        }
    }

    private fun renderRows(rows: List<com.shoptv.feature.magnit.data.local.CatalogRow>) {
        rowsAdapter.clear()
        if (rows.isEmpty()) return

        val cardPresenter = ProductCardPresenter()
        var total = 0

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

    /** Раньше ошибка молча уходила в никуда и экран оставался пустым */
    private fun onError(message: String?) {
        if (message.isNullOrBlank()) return
        title = getString(R.string.browse_title)
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    private fun openProduct(product: UnifiedProduct) {
        val link = product.deepLink
        if (link.isNullOrBlank()) {
            Toast.makeText(requireContext(), product.title, Toast.LENGTH_SHORT).show()
            return
        }
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(requireContext(), product.title, Toast.LENGTH_SHORT).show()
        }
    }
}
