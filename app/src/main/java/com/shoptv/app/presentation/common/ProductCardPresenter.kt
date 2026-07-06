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

class ProductCardPresenter : Presenter() {

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
        val product = item as UnifiedProduct
        val refs = viewHolder.view.tag as? ViewRefs ?: return
        refs.title.text = product.title
        refs.price.text = buildString {
            append(product.formattedPrice)
            product.discountPercent?.let { append(" (-$it%)") }
        }
        refs.image.load(product.imageUrl) {
            crossfade(true)
            placeholder(android.R.color.darker_gray)
        }
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        val refs = viewHolder.view.tag as? ViewRefs ?: return
        refs.image.setImageDrawable(null)
    }

    private fun dpToPx(ctx: android.content.Context, dp: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), ctx.resources.displayMetrics).toInt()

    private data class ViewRefs(
        val image: ImageView,
        val title: TextView,
        val price: TextView
    )
}
