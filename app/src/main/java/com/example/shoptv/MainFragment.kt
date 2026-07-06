package com.example.shoptv

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ImageView
import android.widget.TextView
import android.util.TypedValue
import android.content.Context
import android.view.Gravity
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
// ...existing code...
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.Presenter
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.shoptv.network.MockProductRepository
import com.example.shoptv.model.Product
import coil.load

class MainFragment : BrowseSupportFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // МЕНЯЕМ ТУТ, чтобы проверить, обновилось ли приложение
        title = "ЛЕНТА — КАТАЛОГ"

        setupAdapter()
    }

    private fun setupAdapter() {
        val mRowsAdapter = ArrayObjectAdapter(ListRowPresenter())

        // Создаем категорию
        val header = HeaderItem(0, "Популярное")
        val cardPresenter = ProductCardPresenter()
        val listRowAdapter = ArrayObjectAdapter(cardPresenter)

         // Добавляем строку и адаптер
         mRowsAdapter.add(ListRow(header, listRowAdapter))
         adapter = mRowsAdapter

         // Используем mock-данные (lenta.com блокирует скрейпинг в эмуляторе)
         lifecycleScope.launch {
             val mockRepo = MockProductRepository()
             val products = try {
                 withContext(Dispatchers.IO) { mockRepo.getPopularProducts() }
             } catch (e: Exception) {
                 emptyList<Product>()
             }

             // Всегда есть товары из mock-данных
             for (p in products) listRowAdapter.add(p)
         }
     }

    // Вспомогательный контейнер для представлений карточки
    private data class ViewRefs(val image: ImageView, val title: TextView, val content: TextView)

    // Презентер для отрисовки карточек
    inner class ProductCardPresenter : Presenter() {
        private fun dpToPx(ctx: Context, dp: Int): Int = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), ctx.resources.displayMetrics
        ).toInt()

        override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
            val ctx = parent.context
            val container = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                isFocusable = true
                isFocusableInTouchMode = true
                gravity = Gravity.CENTER
                val padding = dpToPx(ctx, 8)
                setPadding(padding, padding, padding, padding)
            }

            val image = ImageView(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(dpToPx(ctx, 313), dpToPx(ctx, 176))
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
            val titleView = TextView(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            }
            val contentView = TextView(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            }

            container.addView(image)
            container.addView(titleView)
            container.addView(contentView)

            // store references in tag for onBind
            container.tag = ViewRefs(image, titleView, contentView)

            return ViewHolder(container)
        }

        override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
            val product = item as Product
            val container = viewHolder.view as LinearLayout
            val refs = container.tag as? ViewRefs
            if (refs != null) {
                refs.title.text = product.title
                refs.content.text = product.price
                // load image asynchronously with Coil (imageUrl may be null)
                refs.image.load(product.imageUrl) {
                    crossfade(true)
                    placeholder(android.R.color.darker_gray)
                }
            }
        }

        override fun onUnbindViewHolder(viewHolder: ViewHolder) {}
    }
}


