package com.shoptv.app.presentation.common

import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.view.Gravity
import android.util.TypedValue
import androidx.leanback.widget.Presenter
import coil.load
import com.shoptv.core.model.UnifiedProduct

/**
 * Карточка товара для Leanback TV.
 *
 * Долгое нажатие — добавить в корзину.
 */
class ProductCardPresenter(
    private val onLongClick: ((UnifiedProduct) -> Unit)? = null
) : Presenter() {

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val ctx = parent.context
        val container = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            isFocusable = true
            isFocusableInTouchMode = true
            gravity = Gravity.CENTER
            val pad = dpToPx(ctx, 8)
            setPadding(pad, pad, pad, pad)
        }

        val image = ImageView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(ctx, 313), dpToPx(ctx, 176))
            scaleType = ImageView.ScaleType.CENTER_CROP
        }

        val title = TextView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            maxLines = 2
        }

        val price = TextView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        container.addView(image)
        container.addView(title)
        container.addView(price)

        container.tag = ViewRefs(image, title, price)
        return ViewHolder(container)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        val refs = viewHolder.view.tag as? ViewRefs ?: return
        when (item) {
            is UnifiedProduct -> {
                refs.title.text = item.title
                refs.price.text = buildString {
                    append(item.formattedPrice)
                    item.discountPercent?.let { append(" (-$it%)") }
                }
                refs.image.load(item.imageUrl) {
                    crossfade(true)
                    placeholder(android.R.color.darker_gray)
                }
                viewHolder.view.setOnLongClickListener {
                    onLongClick?.invoke(item)
                    true
                }
            }
            is String -> {
                refs.title.text = item
                refs.price.text = ""
                refs.image.setImageResource(android.R.drawable.ic_menu_search)
                viewHolder.view.setOnLongClickListener(null)
            }
        }
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        val refs = viewHolder.view.tag as? ViewRefs ?: return
        refs.image.setImageDrawable(null)
        viewHolder.view.setOnLongClickListener(null)
    }

    private fun dpToPx(ctx: android.content.Context, dp: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), ctx.resources.displayMetrics).toInt()

    private data class ViewRefs(
        val image: ImageView,
        val title: TextView,
        val price: TextView
    )
}
