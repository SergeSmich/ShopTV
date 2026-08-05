package com.shoptv.app.presentation.cart

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.shoptv.core.model.UnifiedProduct

class CartFragment : Fragment() {

    private var container: LinearLayout? = null

    override fun onCreateView(inflater: LayoutInflater, parent: ViewGroup?, state: Bundle?): View {
        container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(80, 64, 80, 64)
            setBackgroundColor(Color.parseColor("#1A1A1A"))
        }
        return container!!
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        renderCart()
    }

    override fun onResume() {
        super.onResume()
        renderCart()
    }

    private fun renderCart() {
        container?.removeAllViews()
        val items = CartManager.getItems()

        // Заголовок
        container?.addView(TextView(requireContext()).apply {
            text = "🛒 Корзина"
            textSize = 28f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, 16)
        })

        // Разделитель
        container?.addView(View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 2
            )
            setBackgroundColor(Color.parseColor("#333333"))
        })

        if (items.isEmpty()) {
            container?.addView(TextView(requireContext()).apply {
                text = "\nКорзина пуста\n\nДобавьте товары из каталога\n(долгое нажатие на карточку товара)"
                textSize = 18f
                setTextColor(Color.parseColor("#AAAAAA"))
                setPadding(0, 48, 0, 0)
                gravity = Gravity.CENTER
            })
            return
        }

        // Шапка таблицы
        container?.addView(LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 24, 0, 16)
            addView(TextView(requireContext()).apply {
                text = "Товар"
                textSize = 14f
                setTextColor(Color.parseColor("#888888"))
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(TextView(requireContext()).apply {
                text = "Цена"
                textSize = 14f
                setTextColor(Color.parseColor("#888888"))
                gravity = Gravity.END
                setPadding(0, 0, 60, 0)
            })
        })

        // Список товаров
        items.forEach { product ->
            container?.addView(createCartItem(product))
        }

        // Разделитель
        container?.addView(View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 2
            )
            setBackgroundColor(Color.parseColor("#333333"))
            setPadding(0, 16, 0, 0)
        })

        // Итого
        val total = items.sumOf { it.priceCurrent }
        container?.addView(LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 24, 0, 0)
            addView(TextView(requireContext()).apply {
                text = "Итого:"
                textSize = 22f
                setTextColor(Color.WHITE)
                typeface = Typeface.DEFAULT_BOLD
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(TextView(requireContext()).apply {
                text = "%.2f ₽".format(total)
                textSize = 22f
                setTextColor(Color.parseColor("#4CAF50"))
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.END
            })
        })

        // Количество
        container?.addView(TextView(requireContext()).apply {
            text = "${items.size} ${itemWord(items.size)}"
            textSize = 14f
            setTextColor(Color.parseColor("#888888"))
            gravity = Gravity.END
            setPadding(0, 4, 0, 24)
        })

        // Кнопка очистки
        container?.addView(TextView(requireContext()).apply {
            text = "🗑 Очистить корзину"
            textSize = 16f
            setTextColor(Color.WHITE)
            setPadding(32, 20, 32, 20)
            gravity = Gravity.CENTER
            isFocusable = true
            isFocusableInTouchMode = true
            setBackgroundColor(Color.parseColor("#CC0000"))
            setOnClickListener {
                CartManager.clear()
                renderCart()
                Toast.makeText(requireContext(), "Корзина очищена", Toast.LENGTH_SHORT).show()
            }
            setOnFocusChangeListener { v, hasFocus ->
                v.setBackgroundColor(if (hasFocus) Color.parseColor("#FF0000") else Color.parseColor("#CC0000"))
            }
        })
    }

    private fun createCartItem(product: UnifiedProduct): View {
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 16, 0, 16)
            setBackgroundColor(Color.parseColor("#1A1A1A"))

            // Название + скидка
            addView(LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                addView(TextView(requireContext()).apply {
                    text = product.title
                    textSize = 16f
                    setTextColor(Color.WHITE)
                    maxLines = 2
                })
                product.discountPercent?.let { discount ->
                    addView(TextView(requireContext()).apply {
                        text = "Скидка $discount%"
                        textSize = 12f
                        setTextColor(Color.parseColor("#FF5722"))
                    })
                }
            })

            // Цена
            addView(TextView(requireContext()).apply {
                text = product.formattedPrice
                textSize = 16f
                setTextColor(Color.parseColor("#4CAF50"))
                gravity = Gravity.CENTER_VERTICAL
                setPadding(16, 0, 16, 0)
            })

            // Кнопка удаления
            addView(TextView(requireContext()).apply {
                text = "✕"
                textSize = 20f
                setTextColor(Color.parseColor("#FF5555"))
                setPadding(16, 0, 16, 0)
                gravity = Gravity.CENTER_VERTICAL
                isFocusable = true
                isFocusableInTouchMode = true
                setOnClickListener {
                    CartManager.remove(product)
                    renderCart()
                }
                setOnFocusChangeListener { v, hasFocus ->
                    (v as TextView).setTextColor(
                        if (hasFocus) Color.RED else Color.parseColor("#FF5555")
                    )
                }
            })
        }
    }

    private fun itemWord(count: Int): String {
        val lastTwo = count % 100
        val lastOne = count % 10
        return when {
            lastTwo in 11..19 -> "товаров"
            lastOne == 1 -> "товар"
            lastOne in 2..4 -> "товара"
            else -> "товаров"
        }
    }

    companion object {
        fun newInstance() = CartFragment()
    }
}
