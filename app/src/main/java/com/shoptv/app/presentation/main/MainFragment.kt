package com.shoptv.app.presentation.main

import android.os.Bundle
import android.view.View
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.shoptv.app.presentation.common.ProductCardPresenter
import com.shoptv.core.model.UnifiedProduct
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainFragment : BrowseSupportFragment() {

    private val viewModel: MainViewModel by viewModel()
    private lateinit var rowsAdapter: ArrayObjectAdapter
    private lateinit var magnitRowAdapter: ArrayObjectAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        title = "ShopTV — Каталог"
        setupAdapter()
        observeViewModel()
        viewModel.loadMagnitProducts()
    }

    private fun setupAdapter() {
        rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
        val presenter = ProductCardPresenter()
        magnitRowAdapter = ArrayObjectAdapter(presenter)
        val header = HeaderItem(0, "Магнит — Популярное")
        rowsAdapter.add(ListRow(header, magnitRowAdapter))
        adapter = rowsAdapter
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.products.collect { products ->
                    magnitRowAdapter.clear()
                    products.forEach { magnitRowAdapter.add(it) }
                }
            }
        }
    }
}
