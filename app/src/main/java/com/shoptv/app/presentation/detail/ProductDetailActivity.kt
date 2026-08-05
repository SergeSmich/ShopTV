package com.shoptv.app.presentation.detail

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.shoptv.app.R

class ProductDetailActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_detail)

        val productId = intent.getStringExtra(EXTRA_PRODUCT_ID) ?: ""
        val productTitle = intent.getStringExtra(EXTRA_PRODUCT_TITLE) ?: ""
        val productPrice = intent.getDoubleExtra(EXTRA_PRODUCT_PRICE, 0.0)
        val productOldPrice = intent.getDoubleExtra(EXTRA_PRODUCT_OLD_PRICE, 0.0)
        val productImage = intent.getStringExtra(EXTRA_PRODUCT_IMAGE) ?: ""
        val productCategory = intent.getStringExtra(EXTRA_PRODUCT_CATEGORY) ?: ""
        val productDeepLink = intent.getStringExtra(EXTRA_PRODUCT_DEEP_LINK) ?: ""

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(
                    R.id.detail_fragment,
                    ProductDetailFragment.newInstance(
                        productId, productTitle, productPrice, productOldPrice,
                        productImage, productCategory, productDeepLink
                    )
                )
                .commit()
        }
    }

    companion object {
        const val EXTRA_PRODUCT_ID = "product_id"
        const val EXTRA_PRODUCT_TITLE = "product_title"
        const val EXTRA_PRODUCT_PRICE = "product_price"
        const val EXTRA_PRODUCT_OLD_PRICE = "product_old_price"
        const val EXTRA_PRODUCT_IMAGE = "product_image"
        const val EXTRA_PRODUCT_CATEGORY = "product_category"
        const val EXTRA_PRODUCT_DEEP_LINK = "product_deep_link"
    }
}
