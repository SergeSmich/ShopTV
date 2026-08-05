package com.shoptv.app.presentation.detail

import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import coil.load
import com.shoptv.app.presentation.cart.CartManager
import com.shoptv.app.presentation.common.ProductCardPresenter
import com.shoptv.core.model.StoreType
import com.shoptv.core.model.UnifiedProduct
import com.shoptv.feature.magnit.data.local.MagnitLocalCatalog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject

class ProductDetailFragment : Fragment() {

    private val localCatalog: MagnitLocalCatalog by inject()

    private var productId = ""
    private var productTitle = ""
    private var productPrice = 0.0
    private var productOldPrice = 0.0
    private var productImage = ""
    private var productCategory = ""
    private var productDeepLink = ""

    private var similarContainer: LinearLayout? = null
    private var recommendedContainer: LinearLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            productId = it.getString(ARG_ID, "")
            productTitle = it.getString(ARG_TITLE, "")
            productPrice = it.getDouble(ARG_PRICE)
            productOldPrice = it.getDouble(ARG_OLD_PRICE)
            productImage = it.getString(ARG_IMAGE, "")
            productCategory = it.getString(ARG_CATEGORY, "")
            productDeepLink = it.getString(ARG_DEEP_LINK, "")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, parent: ViewGroup?, state: Bundle?): View {
        val scrollView = ScrollView(requireContext()).apply {
            setBackgroundColor(Color.parseColor("#121212"))
            isFocusable = false
        }

        val root = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(80, 48, 80, 48)
        }

        // 1. Верхняя часть: фото + цена + кнопки
        root.addView(createTopSection())

        // 2. Описание
        root.addView(createSection("Описание") {
            addView(createText("Товар из каталога Магнит. Для подробного описания откройте карточку в приложении.", Color.parseColor("#AAAAAA")))
        })

        // 3. Характеристики
        root.addView(createSpecsSection())

        // 4. Похожие товары
        similarContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
        }
        root.addView(similarContainer)

        // 5. Рекомендуемые
        recommendedContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
        }
        root.addView(recommendedContainer)

        scrollView.addView(root)

        // Загружаем похожие и рекомендуемые
        loadSimilarAndRecommended()

        return scrollView
    }

    private fun loadSimilarAndRecommended() {
        CoroutineScope(Dispatchers.IO).launch {
            val allProducts = localCatalog.getAllProducts()

            // Похожие — та же категория
            val similar = allProducts
                .filter { it.categoryName == productCategory && it.id != productId }
                .shuffled()
                .take(6)

            // Рекомендуемые — другие категории
            val recommended = allProducts
                .filter { it.categoryName != productCategory }
                .shuffled()
                .take(6)

            withContext(Dispatchers.Main) {
                renderSimilar(similar)
                renderRecommended(recommended)
            }
        }
    }

    private fun renderSimilar(products: List<UnifiedProduct>) {
        similarContainer?.removeAllViews()
        if (products.isEmpty()) return

        val section = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#1E1E1E"))
            setPadding(32, 24, 32, 24)
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.setMargins(0, 0, 0, 16)
            layoutParams = lp
        }

        section.addView(createText("Похожие товары", Color.WHITE, 18f, Typeface.DEFAULT_BOLD))

        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.setMargins(0, 16, 0, 0)
            layoutParams = lp
        }

        products.forEach { product ->
            row.addView(createMiniCard(product))
        }

        section.addView(row)
        similarContainer?.addView(section)
    }

    private fun renderRecommended(products: List<UnifiedProduct>) {
        recommendedContainer?.removeAllViews()
        if (products.isEmpty()) return

        val section = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#1E1E1E"))
            setPadding(32, 24, 32, 24)
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.setMargins(0, 0, 0, 16)
            layoutParams = lp
        }

        section.addView(createText("Рекомендуем", Color.WHITE, 18f, Typeface.DEFAULT_BOLD))

        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.setMargins(0, 16, 0, 0)
            layoutParams = lp
        }

        products.forEach { product ->
            row.addView(createMiniCard(product))
        }

        section.addView(row)
        recommendedContainer?.addView(section)
    }

    private fun createMiniCard(product: UnifiedProduct): View {
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(12, 12, 12, 12)
            setBackgroundColor(Color.parseColor("#2A2A2A"))
            layoutParams = LinearLayout.LayoutParams(dpToPx(160), ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                marginEnd = 12
            }
            isFocusable = true
            isFocusableInTouchMode = true

            // Фото
            addView(ImageView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(dpToPx(136), dpToPx(100))
                scaleType = ImageView.ScaleType.CENTER_CROP
                load(product.imageUrl) {
                    crossfade(true)
                    placeholder(android.R.color.darker_gray)
                }
            })

            // Название
            addView(TextView(requireContext()).apply {
                text = product.title
                textSize = 12f
                setTextColor(Color.WHITE)
                maxLines = 2
                setPadding(0, 8, 0, 0)
            })

            // Цена
            addView(TextView(requireContext()).apply {
                text = product.formattedPrice
                textSize = 14f
                setTextColor(Color.parseColor("#4CAF50"))
                setPadding(0, 4, 0, 0)
            })

            // Скидка
            product.discountPercent?.let { discount ->
                addView(TextView(requireContext()).apply {
                    text = "-$discount%"
                    textSize = 11f
                    setTextColor(Color.parseColor("#FF5722"))
                })
            }

            // Клик — открыть этот товар
            setOnClickListener {
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

            setOnFocusChangeListener { v, hasFocus ->
                v.setBackgroundColor(if (hasFocus) Color.parseColor("#444444") else Color.parseColor("#2A2A2A"))
            }
        }
    }

    private fun createTopSection(): View {
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, 32)

            // Фото
            addView(ImageView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(dpToPx(300), dpToPx(250))
                scaleType = ImageView.ScaleType.CENTER_CROP
                load(productImage) {
                    crossfade(true)
                    placeholder(android.R.color.darker_gray)
                }
            })

            // Информация
            addView(LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = 32
                }

                // Название
                addView(createText(productTitle, Color.WHITE, 22f, Typeface.DEFAULT_BOLD))

                // Категория — КЛИКАБЕЛЬНАЯ
                addView(TextView(requireContext()).apply {
                    text = "📁 $productCategory"
                    textSize = 14f
                    setTextColor(Color.parseColor("#64B5F6"))
                    setPadding(0, 8, 0, 8)
                    isFocusable = true
                    isFocusableInTouchMode = true
                    paintFlags = paintFlags or Paint.UNDERLINE_TEXT_FLAG
                    setOnClickListener {
                        requireActivity().finish()
                    }
                    setOnFocusChangeListener { v, hasFocus ->
                        (v as TextView).setTextColor(if (hasFocus) Color.WHITE else Color.parseColor("#64B5F6"))
                    }
                })

                // Бейдж скидки
                val discount = if (productOldPrice > productPrice && productOldPrice > 0) {
                    ((productOldPrice - productPrice) / productOldPrice * 100).toInt()
                } else 0
                if (discount > 0) {
                    addView(createText("-$discount%", Color.WHITE, 16f, Typeface.DEFAULT_BOLD).apply {
                        setBackgroundColor(Color.parseColor("#FF5722"))
                        setPadding(16, 4, 16, 4)
                        val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                        lp.setMargins(0, 8, 0, 8)
                        layoutParams = lp
                    })
                }

                // Цена
                addView(createText("%.2f ₽".format(productPrice), Color.parseColor("#4CAF50"), 28f, Typeface.DEFAULT_BOLD))

                // Старая цена
                if (productOldPrice > productPrice && productOldPrice > 0) {
                    addView(createText("%.2f ₽".format(productOldPrice), Color.parseColor("#666666"), 16f).apply {
                        paintFlags = paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                    })
                    addView(createText("Выгода %.2f ₽".format(productOldPrice - productPrice), Color.parseColor("#FF9800"), 14f))
                }

                // Кнопки
                addView(createActionButtons())
            })
        }
    }

    private fun createActionButtons(): View {
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.setMargins(0, 24, 0, 0)
            layoutParams = lp

            addView(createButton("🛒 В корзину", Color.parseColor("#4CAF50"), Color.parseColor("#66BB6A")) {
                CartManager.add(createProduct())
                Toast.makeText(requireContext(), "🛒 $productTitle", Toast.LENGTH_SHORT).show()
            })

            addView(createButton("❤️", Color.parseColor("#333333"), Color.parseColor("#555555")) {
                Toast.makeText(requireContext(), "❤️ В избранное", Toast.LENGTH_SHORT).show()
            })

            addView(createButton("🔗 В Магните", Color.parseColor("#333333"), Color.parseColor("#555555")) {
                if (productDeepLink.isNotBlank()) {
                    try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(productDeepLink))) }
                    catch (e: Exception) { Toast.makeText(requireContext(), "Не удалось открыть", Toast.LENGTH_SHORT).show() }
                }
            })
        }
    }

    private fun createSpecsSection(): View {
        return createSection("Характеристики") {
            listOf(
                "Артикул" to productId,
                "Категория" to productCategory,
                "Магазин" to "Магнит",
                "Тип" to "Экспресс-доставка"
            ).forEach { (key, value) ->
                addView(LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(0, 8, 0, 8)
                    addView(createText(key, Color.parseColor("#888888"), 14f).apply {
                        layoutParams = LinearLayout.LayoutParams(dpToPx(150), ViewGroup.LayoutParams.WRAP_CONTENT)
                    })
                    addView(createText(value.ifEmpty { "—" }, Color.WHITE, 14f))
                })
            }
        }
    }

    private fun createSection(title: String, content: LinearLayout.() -> Unit): View {
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#1E1E1E"))
            setPadding(32, 24, 32, 24)
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.setMargins(0, 0, 0, 16)
            layoutParams = lp
            addView(createText(title, Color.WHITE, 18f, Typeface.DEFAULT_BOLD))
            content()
        }
    }

    private fun createText(text: String, color: Int, size: Float = 14f, typeface: Typeface? = null): TextView {
        return TextView(requireContext()).apply {
            this.text = text
            textSize = size
            setTextColor(color)
            typeface?.let { this.typeface = it }
            setPadding(0, 4, 0, 4)
        }
    }

    private fun createButton(text: String, bgColor: Int, focusColor: Int, onClick: () -> Unit): TextView {
        return TextView(requireContext()).apply {
            this.text = text
            textSize = 16f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(24, 16, 24, 16)
            isFocusable = true
            isFocusableInTouchMode = true
            setBackgroundColor(bgColor)
            setOnClickListener { onClick() }
            setOnFocusChangeListener { v, hasFocus -> v.setBackgroundColor(if (hasFocus) focusColor else bgColor) }
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = 8 }
        }
    }

    private fun createProduct() = UnifiedProduct(
        id = productId, title = productTitle, priceCurrent = productPrice,
        priceOld = if (productOldPrice > productPrice) productOldPrice else null,
        imageUrl = productImage, storeType = StoreType.MAGNIT,
        categoryName = productCategory, deepLink = productDeepLink
    )

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    companion object {
        private const val ARG_ID = "id"
        private const val ARG_TITLE = "title"
        private const val ARG_PRICE = "price"
        private const val ARG_OLD_PRICE = "old_price"
        private const val ARG_IMAGE = "image"
        private const val ARG_CATEGORY = "category"
        private const val ARG_DEEP_LINK = "deep_link"

        fun newInstance(id: String, title: String, price: Double, oldPrice: Double,
                        image: String, category: String, deepLink: String
        ) = ProductDetailFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_ID, id); putString(ARG_TITLE, title)
                putDouble(ARG_PRICE, price); putDouble(ARG_OLD_PRICE, oldPrice)
                putString(ARG_IMAGE, image); putString(ARG_CATEGORY, category)
                putString(ARG_DEEP_LINK, deepLink)
            }
        }
    }
}
