package com.example.shoptv.presenter

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.leanback.widget.Presenter
import coil.load
import com.example.shoptv.R
import com.example.shoptv.model.Product

/**
 * Карточка товара для строк каталога.
 * Показывает картинку (если есть), название, цену, старую цену и бейдж скидки.
 */
class ProductCardPresenter : Presenter() {

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.card_product, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        val holder = viewHolder as ProductViewHolder
        val product = item as? Product ?: return

        holder.title.text = product.title
        holder.price.text = product.priceFormatted

        // Старая цена — зачёркнутая
        val oldPrice = product.oldPriceFormatted
        if (product.hasDiscount && oldPrice != null) {
            holder.oldPrice.text = oldPrice
            holder.oldPrice.paintFlags = holder.oldPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            holder.oldPrice.visibility = View.VISIBLE
        } else {
            holder.oldPrice.visibility = View.GONE
        }

        // Бейдж скидки
        val discount = product.discountLabel
        if (discount != null) {
            holder.discount.text = discount
            holder.discount.visibility = View.VISIBLE
        } else {
            holder.discount.visibility = View.GONE
        }

        // Картинка есть не у всех товаров — часть выгрузок сохранила только текст
        val url = product.imageUrl
        if (url.isNullOrBlank()) {
            holder.image.setImageDrawable(null)
            holder.image.setBackgroundResource(R.color.card_bg_focused)
        } else {
            holder.image.setBackgroundResource(R.color.card_image_bg)
            holder.image.load(url) {
                crossfade(true)
            }
        }
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        val holder = viewHolder as ProductViewHolder
        holder.image.setImageDrawable(null)
    }

    private class ProductViewHolder(view: View) : ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.card_image)
        val title: TextView = view.findViewById(R.id.card_title)
        val price: TextView = view.findViewById(R.id.card_price)
        val oldPrice: TextView = view.findViewById(R.id.card_old_price)
        val discount: TextView = view.findViewById(R.id.card_discount)
    }
}
