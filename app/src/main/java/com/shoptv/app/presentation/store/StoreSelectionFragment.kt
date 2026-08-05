package com.shoptv.app.presentation.store

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment

/**
 * Экран выбора магазина.
 *
 * Показывает предустановленные магазины.
 * API магнита блокируется антибот-защитой, поэтому используем локальный список.
 */
class StoreSelectionFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, parent: ViewGroup?, state: Bundle?): View {
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(64, 64, 64, 64)
        }

        // Заголовок
        container.addView(TextView(requireContext()).apply {
            text = "🏪 Выберите магазин"
            textSize = 24f
            setPadding(0, 0, 0, 48)
        })

        // Предустановленные магазины
        PREDEFINED_STORES.forEach { (code, name, city) ->
            container.addView(createStoreItem(code, name, city))
        }

        return container
    }

    private fun createStoreItem(code: String, name: String, city: String): View {
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 24, 32, 24)
            isFocusable = true
            isFocusableInTouchMode = true
            setBackgroundColor(0xFF333333.toInt())

            addView(TextView(requireContext()).apply {
                text = name
                textSize = 18f
            })
            addView(TextView(requireContext()).apply {
                text = "$city • $code"
                textSize = 14f
                setPadding(0, 8, 0, 0)
            })

            setOnClickListener {
                saveStore(code, name)
            }

            setOnFocusChangeListener { v, hasFocus ->
                v.setBackgroundColor(if (hasFocus) 0xFF666666.toInt() else 0xFF333333.toInt())
            }
        }
    }

    private fun saveStore(storeCode: String, storeName: String) {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, 0)
        prefs.edit()
            .putString(KEY_STORE_CODE, storeCode)
            .putString(KEY_STORE_NAME, storeName)
            .apply()

        Toast.makeText(requireContext(), "✅ $storeName", Toast.LENGTH_SHORT).show()
        requireActivity().finish()
    }

    companion object {
        const val PREFS_NAME = "shop_prefs"
        const val KEY_STORE_CODE = "store_code"
        const val KEY_STORE_NAME = "store_name"

        // Предустановленные магазины
        // storeCode из offline-каталога: 781225 (Москва, экспресс)
        private val PREDEFINED_STORES = listOf(
            Triple("781225", "Магнит Экспресс", "Москва"),
            Triple("781225", "Магнит Доставка", "Москва"),
            Triple("543358", "Магнит Экспресс", "Санкт-Петербург"),
            Triple("543358", "Магнит Доставка", "Санкт-Петербург"),
            Triple("992301", "Магнит Маркет", "Москва"),
            Triple("780001", "Магнит Экспресс", "Екатеринбург"),
            Triple("640001", "Магнит Экспресс", "Казань"),
            Triple("380001", "Магнит Экспресс", "Краснодар"),
        )
    }
}
